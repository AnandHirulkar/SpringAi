package io.github.anandhirulkar.supportiq.shared.audit;

import io.github.anandhirulkar.supportiq.shared.api.PageResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springdoc.core.annotations.ParameterObject;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/audit-events")
@Tag(name = "Audit", description = "Append-only event trail stored in MongoDB")
public class AuditController {

    private final AuditQueryService auditQueryService;

    public AuditController(AuditQueryService auditQueryService) {
        this.auditQueryService = auditQueryService;
    }

    @GetMapping
    @Operation(summary = "List recent audit events, optionally filtered by type")
    public PageResponse<AuditEntry> list(
            @RequestParam(required = false) String type,
            @ParameterObject @PageableDefault(size = 20, sort = "occurredAt") Pageable pageable) {
        return auditQueryService.list(type, pageable);
    }
}
