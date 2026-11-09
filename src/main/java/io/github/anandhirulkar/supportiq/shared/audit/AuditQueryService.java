package io.github.anandhirulkar.supportiq.shared.audit;

import io.github.anandhirulkar.supportiq.shared.api.PageResponse;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

@Service
public class AuditQueryService {

    private final AuditEntryRepository auditEntries;

    public AuditQueryService(AuditEntryRepository auditEntries) {
        this.auditEntries = auditEntries;
    }

    public PageResponse<AuditEntry> list(String type, Pageable pageable) {
        var page = type == null || type.isBlank()
                ? auditEntries.findAllByOrderByOccurredAtDesc(pageable)
                : auditEntries.findByTypeOrderByOccurredAtDesc(type, pageable);
        return PageResponse.from(page);
    }
}
