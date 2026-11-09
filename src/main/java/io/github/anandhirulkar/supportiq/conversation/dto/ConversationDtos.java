package io.github.anandhirulkar.supportiq.conversation.dto;

import java.time.Instant;
import java.util.List;

import io.github.anandhirulkar.supportiq.conversation.AiConversation;
import io.github.anandhirulkar.supportiq.conversation.Citation;
import io.github.anandhirulkar.supportiq.conversation.ConversationTurn;

public final class ConversationDtos {

    private ConversationDtos() {
    }

    public record CitationResponse(String documentId, String title, String source, String snippet, double score) {

        public static CitationResponse from(Citation citation) {
            return new CitationResponse(
                    citation.documentId(), citation.title(), citation.source(), citation.snippet(), citation.score());
        }
    }

    public record TurnResponse(
            String role,
            String content,
            List<CitationResponse> citations,
            int promptTokens,
            int completionTokens,
            String model,
            long latencyMs,
            Instant at) {

        public static TurnResponse from(ConversationTurn turn) {
            return new TurnResponse(
                    turn.role(),
                    turn.content(),
                    turn.citations().stream().map(CitationResponse::from).toList(),
                    turn.promptTokens(),
                    turn.completionTokens(),
                    turn.model(),
                    turn.latencyMs(),
                    turn.at());
        }
    }

    public record ConversationResponse(
            String id,
            Long customerId,
            String title,
            Instant createdAt,
            Instant updatedAt,
            List<TurnResponse> turns) {

        public static ConversationResponse from(AiConversation conversation) {
            return new ConversationResponse(
                    conversation.getId(),
                    conversation.getCustomerId(),
                    conversation.getTitle(),
                    conversation.getCreatedAt(),
                    conversation.getUpdatedAt(),
                    conversation.getTurns().stream().map(TurnResponse::from).toList());
        }
    }

    public record ConversationSummary(
            String id, Long customerId, String title, Instant createdAt, Instant updatedAt, int turns) {

        public static ConversationSummary from(AiConversation conversation) {
            return new ConversationSummary(
                    conversation.getId(),
                    conversation.getCustomerId(),
                    conversation.getTitle(),
                    conversation.getCreatedAt(),
                    conversation.getUpdatedAt(),
                    conversation.getTurns().size());
        }
    }
}
