package io.github.anandhirulkar.supportiq.shared.events;

import java.time.Instant;
import java.util.UUID;

public record TicketCreatedEvent(
        UUID eventId, Instant occurredAt, String reference, Long customerId, String subject, String priority)
        implements DomainEvent {

    public static TicketCreatedEvent of(String reference, Long customerId, String subject, String priority) {
        return new TicketCreatedEvent(
                UUID.randomUUID(), Instant.now(), reference, customerId, subject, priority);
    }

    @Override
    public String type() {
        return "ticket.created";
    }
}
