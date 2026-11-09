package io.github.anandhirulkar.supportiq.shared.events;

import java.time.Instant;
import java.util.UUID;

public record DocumentIngestedEvent(
        UUID eventId, Instant occurredAt, Long documentId, String title, int chunks)
        implements DomainEvent {

    public static DocumentIngestedEvent of(Long documentId, String title, int chunks) {
        return new DocumentIngestedEvent(UUID.randomUUID(), Instant.now(), documentId, title, chunks);
    }

    @Override
    public String type() {
        return "knowledge.document-ingested";
    }
}
