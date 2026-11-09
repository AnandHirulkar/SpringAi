package io.github.anandhirulkar.supportiq.shared.events;

import java.time.Instant;
import java.util.UUID;

public record AiAnswerGeneratedEvent(
        UUID eventId,
        Instant occurredAt,
        String conversationId,
        Long customerId,
        String model,
        int promptTokens,
        int completionTokens,
        long latencyMs)
        implements DomainEvent {

    public static AiAnswerGeneratedEvent of(
            String conversationId,
            Long customerId,
            String model,
            int promptTokens,
            int completionTokens,
            long latencyMs) {
        return new AiAnswerGeneratedEvent(
                UUID.randomUUID(),
                Instant.now(),
                conversationId,
                customerId,
                model,
                promptTokens,
                completionTokens,
                latencyMs);
    }

    @Override
    public String type() {
        return "assistant.answer-generated";
    }
}
