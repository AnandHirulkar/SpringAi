package io.github.anandhirulkar.supportiq.shared.audit;

import java.time.Instant;
import java.util.Map;

import io.github.anandhirulkar.supportiq.shared.events.AiAnswerGeneratedEvent;
import io.github.anandhirulkar.supportiq.shared.events.DocumentIngestedEvent;
import io.github.anandhirulkar.supportiq.shared.events.DomainEvent;
import io.github.anandhirulkar.supportiq.shared.events.TicketCreatedEvent;
import io.github.anandhirulkar.supportiq.shared.events.TicketStatusChangedEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;

/**
 * Observer: turns every domain event into an audit document in MongoDB.
 * Runs on a virtual thread (see {@code spring.threads.virtual.enabled}) and never blocks the caller.
 */
@Component
public class AuditEventListener {

    private static final Logger log = LoggerFactory.getLogger(AuditEventListener.class);

    private final AuditEntryRepository auditEntries;

    public AuditEventListener(AuditEntryRepository auditEntries) {
        this.auditEntries = auditEntries;
    }

    @Async
    @EventListener
    public void on(DomainEvent event) {
        try {
            auditEntries.save(new AuditEntry(
                    null,
                    event.eventId().toString(),
                    event.type(),
                    event.occurredAt(),
                    Instant.now(),
                    payload(event),
                    "application"));
        } catch (RuntimeException failure) {
            log.warn("Could not persist audit record for {}", event.eventId(), failure);
        }
    }

    private static Map<String, Object> payload(DomainEvent event) {
        return switch (event) {
            case TicketCreatedEvent ticket -> Map.of(
                    "reference", ticket.reference(),
                    "customerId", ticket.customerId(),
                    "subject", ticket.subject(),
                    "priority", ticket.priority());
            case TicketStatusChangedEvent ticket -> Map.of(
                    "reference", ticket.reference(),
                    "previousStatus", ticket.previousStatus(),
                    "newStatus", ticket.newStatus());
            case DocumentIngestedEvent document -> Map.of(
                    "documentId", document.documentId(),
                    "title", document.title(),
                    "chunks", document.chunks());
            case AiAnswerGeneratedEvent answer -> Map.of(
                    "conversationId", String.valueOf(answer.conversationId()),
                    "model", String.valueOf(answer.model()),
                    "promptTokens", answer.promptTokens(),
                    "completionTokens", answer.completionTokens(),
                    "latencyMs", answer.latencyMs());
        };
    }
}
