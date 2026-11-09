package io.github.anandhirulkar.supportiq.knowledge;

import java.util.List;

import io.github.anandhirulkar.supportiq.knowledge.dto.KnowledgeDtos.DocumentResponse;
import io.github.anandhirulkar.supportiq.knowledge.dto.KnowledgeDtos.SearchHit;
import io.github.anandhirulkar.supportiq.knowledge.dto.KnowledgeDtos.SearchResponse;
import io.github.anandhirulkar.supportiq.shared.api.PageResponse;
import io.github.anandhirulkar.supportiq.shared.exception.ResourceNotFoundException;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class KnowledgeQueryService {

    private final KnowledgeDocumentRepository documents;
    private final KnowledgeRetrievalService retrieval;
    private final KnowledgeProperties properties;

    public KnowledgeQueryService(
            KnowledgeDocumentRepository documents,
            KnowledgeRetrievalService retrieval,
            KnowledgeProperties properties) {
        this.documents = documents;
        this.retrieval = retrieval;
        this.properties = properties;
    }

    /**
     * Document text and tags never change after ingestion, so this read is safe to cache.
     * The cache is Caffeine with a bounded size and TTL (see {@code spring.cache}).
     */
    @Cacheable(cacheNames = "knowledgeDocuments", key = "#id")
    @Transactional(readOnly = true)
    public DocumentResponse get(Long id) {
        return documents
                .findById(id)
                .map(DocumentResponse::from)
                .orElseThrow(() -> new ResourceNotFoundException("Knowledge document", id));
    }

    @Transactional(readOnly = true)
    public PageResponse<DocumentResponse> page(KnowledgeStatus status, Pageable pageable) {
        var page = status == null ? documents.findAll(pageable) : documents.findByStatus(status, pageable);
        return PageResponse.from(page, DocumentResponse::from);
    }

    public SearchResponse search(String query, Integer topK, Double threshold) {
        int resolvedTopK = topK == null ? properties.searchTopK() : topK;
        double resolvedThreshold = threshold == null ? properties.similarityThreshold() : threshold;
        List<SearchHit> hits = retrieval.retrieve(query, resolvedTopK, resolvedThreshold).stream()
                .map(chunk -> new SearchHit(
                        chunk.documentId(),
                        chunk.title(),
                        chunk.source(),
                        chunk.content(),
                        chunk.score() == null ? 0d : chunk.score()))
                .toList();
        return new SearchResponse(query, resolvedTopK, resolvedThreshold, hits);
    }
}
