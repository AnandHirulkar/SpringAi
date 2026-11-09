package io.github.anandhirulkar.supportiq.shared.events;

import java.time.Instant;
import java.util.UUID;

public record TicketStatusChangedEvent(
        UUID eventId, Instant occurredAt, String reference, String previousStatus, String newStatus)
        implements DomainEvent {

    public static TicketStatusChangedEvent of(String reference, String previousStatus, String newStatus) {
        return new TicketStatusChangedEvent(
                UUID.randomUUID(), Instant.now(), reference, previousStatus, newStatus);
    }

    @Override
    public String type() {
        return "ticket.status-changed";
    }
}
