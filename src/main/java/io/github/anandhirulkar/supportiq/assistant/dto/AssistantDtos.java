package io.github.anandhirulkar.supportiq.assistant.dto;

import java.util.List;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import io.github.anandhirulkar.supportiq.assistant.triage.TicketTriage;
import io.github.anandhirulkar.supportiq.conversation.Citation;
import io.github.anandhirulkar.supportiq.conversation.dto.ConversationDtos.CitationResponse;
import io.github.anandhirulkar.supportiq.ticket.TicketPriority;

public final class AssistantDtos {

    private AssistantDtos() {
    }

    public record ChatRequest(
            @NotNull Long customerId,
            String conversationId,
            @NotBlank @Size(max = 4000) String message,
            Integer topK) {
    }

    public record ChatResponse(
            String conversationId,
            String answer,
            List<CitationResponse> citations,
            String model,
            int promptTokens,
            int completionTokens,
            long latencyMs) {

        public static ChatResponse of(
                String conversationId,
                String answer,
                List<Citation> citations,
                String model,
                int promptTokens,
                int completionTokens,
                long latencyMs) {
            return new ChatResponse(
                    conversationId,
                    answer,
                    citations.stream().map(CitationResponse::from).toList(),
                    model,
                    promptTokens,
                    completionTokens,
                    latencyMs);
        }
    }

    public record TriageResponse(
            String ticketReference,
            TicketTriage triage,
            boolean priorityApplied,
            TicketPriority previousPriority) {
    }
}
