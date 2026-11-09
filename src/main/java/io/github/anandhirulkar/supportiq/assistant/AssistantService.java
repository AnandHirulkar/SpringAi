package io.github.anandhirulkar.supportiq.assistant;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicReference;

import io.github.anandhirulkar.supportiq.assistant.chat.ChatAnswer;
import io.github.anandhirulkar.supportiq.assistant.chat.ChatCommand;
import io.github.anandhirulkar.supportiq.assistant.chat.ChatPort;
import io.github.anandhirulkar.supportiq.assistant.chat.ChatRole;
import io.github.anandhirulkar.supportiq.assistant.chat.ChatTurn;
import io.github.anandhirulkar.supportiq.assistant.dto.AssistantDtos.ChatRequest;
import io.github.anandhirulkar.supportiq.assistant.dto.AssistantDtos.ChatResponse;
import io.github.anandhirulkar.supportiq.conversation.AiConversation;
import io.github.anandhirulkar.supportiq.conversation.Citation;
import io.github.anandhirulkar.supportiq.conversation.ConversationService;
import io.github.anandhirulkar.supportiq.conversation.ConversationTurn;
import io.github.anandhirulkar.supportiq.customer.CustomerRepository;
import io.github.anandhirulkar.supportiq.knowledge.KnowledgeRetrievalService;
import io.github.anandhirulkar.supportiq.knowledge.RetrievedChunk;
import io.github.anandhirulkar.supportiq.observability.AiMetrics;
import io.github.anandhirulkar.supportiq.shared.events.AiAnswerGeneratedEvent;
import io.github.anandhirulkar.supportiq.shared.events.EventPublisher;
import io.github.anandhirulkar.supportiq.shared.exception.ResourceNotFoundException;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;

@Service
public class AssistantService {

    private static final Logger log = LoggerFactory.getLogger(AssistantService.class);

    private final CustomerRepository customers;
    private final ConversationService conversations;
    private final KnowledgeRetrievalService retrieval;
    private final ChatPort chatPort;
    private final PromptCatalog prompts;
    private final AssistantProperties properties;
    private final EventPublisher events;
    private final AiMetrics metrics;

    public AssistantService(
            CustomerRepository customers,
            ConversationService conversations,
            KnowledgeRetrievalService retrieval,
            ChatPort chatPort,
            PromptCatalog prompts,
            AssistantProperties properties,
            EventPublisher events,
            AiMetrics metrics) {
        this.customers = customers;
        this.conversations = conversations;
        this.retrieval = retrieval;
        this.chatPort = chatPort;
        this.prompts = prompts;
        this.properties = properties;
        this.events = events;
        this.metrics = metrics;
    }

    /**
     * Retrieve -> augment -> generate. The user turn is stored before the model call, so a failed
     * call still leaves an accurate transcript; the assistant turn stores citations, tokens and
     * latency for later auditing.
     */
    public ChatResponse chat(ChatRequest request) {
        var customer = customers
                .findById(request.customerId())
                .orElseThrow(() -> new ResourceNotFoundException("Customer", request.customerId()));
        AiConversation conversation = conversations.loadOrCreate(customer.getId(), request.conversationId());
        conversations.appendTurn(
                conversation.getId(),
                new ConversationTurn("USER", request.message(), List.of(), 0, 0, null, 0, Instant.now()));

        List<RetrievedChunk> chunks = retrieval.retrieve(request.message(), request.topK(), null);
        List<ChatTurn> turns = new ArrayList<>(history(conversation));
        turns.add(new ChatTurn(ChatRole.USER, RagPromptBuilder.build(request.message(), chunks)));

        long startedAt = System.nanoTime();
        ChatAnswer answer = chatPort.complete(new ChatCommand(prompts.supportSystemPrompt(), turns));
        long latencyMs = (System.nanoTime() - startedAt) / 1_000_000;

        List<Citation> citations = toCitations(chunks);
        conversations.appendTurn(
                conversation.getId(),
                new ConversationTurn(
                        "ASSISTANT",
                        answer.text(),
                        citations,
                        answer.promptTokens(),
                        answer.completionTokens(),
                        answer.model(),
                        latencyMs,
                        Instant.now()));
        events.publish(AiAnswerGeneratedEvent.of(
                conversation.getId(),
                customer.getId(),
                answer.model(),
                answer.promptTokens(),
                answer.completionTokens(),
                latencyMs));
        metrics.recordChat(answer.totalTokens(), latencyMs);
        log.debug("Answered conversation {} in {} ms", conversation.getId(), latencyMs);

        return ChatResponse.of(
                conversation.getId(),
                answer.text(),
                citations,
                answer.model(),
                answer.promptTokens(),
                answer.completionTokens(),
                latencyMs);
    }

    /**
     * Streams the answer as server-sent events and persists the full transcript when the stream
     * completes. The conversation id is exposed in the {@code X-Conversation-Id} response header.
     */
    public Flux<String> stream(ChatRequest request, HttpServletResponse response) {
        var customer = customers
                .findById(request.customerId())
                .orElseThrow(() -> new ResourceNotFoundException("Customer", request.customerId()));
        AiConversation conversation = conversations.loadOrCreate(customer.getId(), request.conversationId());
        response.setHeader("X-Conversation-Id", conversation.getId());
        response.setContentType(MediaType.TEXT_EVENT_STREAM_VALUE);
        conversations.appendTurn(
                conversation.getId(),
                new ConversationTurn("USER", request.message(), List.of(), 0, 0, null, 0, Instant.now()));

        List<RetrievedChunk> chunks = retrieval.retrieve(request.message(), request.topK(), null);
        List<ChatTurn> turns = new ArrayList<>(history(conversation));
        turns.add(new ChatTurn(ChatRole.USER, RagPromptBuilder.build(request.message(), chunks)));
        List<Citation> citations = toCitations(chunks);
        AtomicReference<StringBuilder> buffer = new AtomicReference<>(new StringBuilder());
        long startedAt = System.nanoTime();

        return chatPort
                .stream(new ChatCommand(prompts.supportSystemPrompt(), turns))
                .doOnNext(token -> buffer.get().append(token))
                .doOnComplete(() -> {
                    long latencyMs = (System.nanoTime() - startedAt) / 1_000_000;
                    conversations.appendTurn(
                            conversation.getId(),
                            new ConversationTurn(
                                    "ASSISTANT",
                                    buffer.get().toString(),
                                    citations,
                                    0,
                                    0,
                                    "streamed",
                                    latencyMs,
                                    Instant.now()));
                    metrics.recordStream(latencyMs);
                })
                .doOnError(error -> log.warn("Streaming failed for conversation {}", conversation.getId(), error));
    }

    private List<ChatTurn> history(AiConversation conversation) {
        return conversation.lastTurns(properties.maxHistoryTurns()).stream()
                .map(turn -> new ChatTurn(
                        "USER".equals(turn.role()) ? ChatRole.USER : ChatRole.ASSISTANT, turn.content()))
                .toList();
    }

    private static List<Citation> toCitations(List<RetrievedChunk> chunks) {
        return chunks.stream()
                .map(chunk -> new Citation(
                        chunk.documentId(),
                        chunk.title(),
                        chunk.source(),
                        chunk.snippet(280),
                        chunk.score() == null ? 0d : chunk.score()))
                .toList();
    }
}
