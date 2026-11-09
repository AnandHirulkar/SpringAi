package io.github.anandhirulkar.supportiq.shared.audit;

import java.time.Instant;
import java.util.Map;

import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

/**
 * Append-only audit trail. Written asynchronously from domain events, so the request thread never
 * pays for the extra write and a failure here cannot roll back business work.
 */
@Document(collection = "audit_events")
public class AuditEntry {

    @Id
    private String id;

    private String eventId;

    private String type;

    private Instant occurredAt;

    private Instant receivedAt;

    private Map<String, Object> payload;

    private String source;

    protected AuditEntry() {
        // for Spring Data
    }

    public AuditEntry(
            String id,
            String eventId,
            String type,
            Instant occurredAt,
            Instant receivedAt,
            Map<String, Object> payload,
            String source) {
        this.id = id;
        this.eventId = eventId;
        this.type = type;
        this.occurredAt = occurredAt;
        this.receivedAt = receivedAt;
        this.payload = payload;
        this.source = source;
    }

    public String getId() {
        return id;
    }

    public String getEventId() {
        return eventId;
    }

    public String getType() {
        return type;
    }

    public Instant getOccurredAt() {
        return occurredAt;
    }

    public Instant getReceivedAt() {
        return receivedAt;
    }

    public Map<String, Object> getPayload() {
        return payload;
    }

    public String getSource() {
        return source;
    }
}
