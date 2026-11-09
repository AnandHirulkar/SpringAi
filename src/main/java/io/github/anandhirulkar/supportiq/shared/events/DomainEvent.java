package io.github.anandhirulkar.supportiq.shared.events;

import java.time.Instant;
import java.util.UUID;

/**
 * Marker for facts that already happened. Sealed so the compiler flags any event that is added
 * without an explicit serialisation type.
 */
public sealed interface DomainEvent
        permits TicketCreatedEvent, TicketStatusChangedEvent, DocumentIngestedEvent, AiAnswerGeneratedEvent {

    UUID eventId();

    Instant occurredAt();

    /** Stable, dot-separated type used for routing in brokers and audit records. */
    String type();
}
