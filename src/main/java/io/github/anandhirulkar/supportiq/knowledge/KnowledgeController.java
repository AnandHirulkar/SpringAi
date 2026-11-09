package io.github.anandhirulkar.supportiq.knowledge;

import io.github.anandhirulkar.supportiq.knowledge.dto.KnowledgeDtos.DocumentResponse;
import io.github.anandhirulkar.supportiq.knowledge.dto.KnowledgeDtos.IngestRequest;
import io.github.anandhirulkar.supportiq.knowledge.dto.KnowledgeDtos.IngestResponse;
import io.github.anandhirulkar.supportiq.knowledge.dto.KnowledgeDtos.SearchResponse;
import io.github.anandhirulkar.supportiq.shared.api.PageResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import org.springdoc.core.annotations.ParameterObject;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/knowledge")
@Validated
@Tag(name = "Knowledge", description = "RAG knowledge base: ingestion, browsing and semantic search")
public class KnowledgeController {

    private final DocumentIngestionService ingestionService;
    private final KnowledgeQueryService queryService;

    public KnowledgeController(DocumentIngestionService ingestionService, KnowledgeQueryService queryService) {
        this.ingestionService = ingestionService;
        this.queryService = queryService;
    }

    @PostMapping("/documents")
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(summary = "Ingest a document (idempotent by content hash)")
    public IngestResponse ingest(@Valid @RequestBody IngestRequest request) {
        return ingestionService.ingest(request);
    }

    @GetMapping("/documents")
    @Operation(summary = "List knowledge documents")
    public PageResponse<DocumentResponse> list(
            @RequestParam(required = false) KnowledgeStatus status,
            @ParameterObject @PageableDefault(size = 20, sort = "createdAt") Pageable pageable) {
        return queryService.page(status, pageable);
    }

    @GetMapping("/documents/{id}")
    @Operation(summary = "Fetch knowledge document metadata")
    public DocumentResponse get(@PathVariable Long id) {
        return queryService.get(id);
    }

    @GetMapping("/search")
    @Operation(summary = "Semantic search over pgvector embeddings")
    public SearchResponse search(
            @RequestParam @NotBlank String q,
            @RequestParam(required = false) Integer topK,
            @RequestParam(required = false) Double threshold) {
        return queryService.search(q, topK, threshold);
    }
}
