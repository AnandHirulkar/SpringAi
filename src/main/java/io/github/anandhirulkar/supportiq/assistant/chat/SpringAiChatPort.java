package io.github.anandhirulkar.supportiq.assistant.chat;

import java.util.List;

import io.github.anandhirulkar.supportiq.assistant.tools.KnowledgeTools;
import io.github.anandhirulkar.supportiq.assistant.tools.TicketTools;
import io.github.anandhirulkar.supportiq.shared.exception.AiServiceUnavailableException;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.messages.AssistantMessage;
import org.springframework.ai.chat.messages.Message;
import org.springframework.ai.chat.messages.UserMessage;
import org.springframework.ai.chat.metadata.Usage;
import org.springframework.ai.chat.model.ChatResponse;
import reactor.core.publisher.Flux;

/**
 * Adapter: translates the provider-agnostic command into Spring AI's {@link ChatClient} and back.
 * Tools are registered once so the model can look up tickets and search the knowledge base.
 */
public class SpringAiChatPort implements ChatPort {

    private final ChatClient chatClient;

    public SpringAiChatPort(ChatClient.Builder builder, TicketTools ticketTools, KnowledgeTools knowledgeTools) {
        this.chatClient = builder.defaultTools(ticketTools, knowledgeTools).build();
    }

    @Override
    public ChatAnswer complete(ChatCommand command) {
        ChatResponse response = chatClient
                .prompt()
                .system(command.systemPrompt())
                .messages(toMessages(command))
                .call()
                .chatResponse();
        if (response == null || response.getResult() == null) {
            throw new AiServiceUnavailableException("The model returned an empty response");
        }
        Usage usage = response.getMetadata() == null ? null : response.getMetadata().getUsage();
        return new ChatAnswer(
                response.getResult().getOutput().getText(),
                response.getMetadata() == null ? "unknown" : response.getMetadata().getModel(),
                usage == null || usage.getPromptTokens() == null ? 0 : usage.getPromptTokens(),
                usage == null || usage.getCompletionTokens() == null ? 0 : usage.getCompletionTokens());
    }

    @Override
    public <T> T completeStructured(ChatCommand command, Class<T> targetType) {
        return chatClient
                .prompt()
                .system(command.systemPrompt())
                .messages(toMessages(command))
                .call()
                .entity(targetType);
    }

    @Override
    public Flux<String> stream(ChatCommand command) {
        return chatClient
                .prompt()
                .system(command.systemPrompt())
                .messages(toMessages(command))
                .stream()
                .content();
    }

    private static List<Message> toMessages(ChatCommand command) {
        return command.turns().stream()
                .map(turn -> switch (turn.role()) {
                    case USER -> (Message) new UserMessage(turn.content());
                    case ASSISTANT -> (Message) new AssistantMessage(turn.content());
                })
                .toList();
    }
}
