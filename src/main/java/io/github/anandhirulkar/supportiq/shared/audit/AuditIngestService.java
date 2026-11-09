package io.github.anandhirulkar.supportiq.shared.audit;

import java.time.Instant;
import java.util.Map;

import org.springframework.stereotype.Service;

/** Write side of the audit trail, shared by messaging adapters. */
@Service
public class AuditIngestService {

    private final AuditEntryRepository auditEntries;

    public AuditIngestService(AuditEntryRepository auditEntries) {
        this.auditEntries = auditEntries;
    }

    public void record(String eventId, String type, Map<String, Object> payload, String source) {
        Instant now = Instant.now();
        auditEntries.save(new AuditEntry(null, eventId, type, now, now, payload, source));
    }
}
