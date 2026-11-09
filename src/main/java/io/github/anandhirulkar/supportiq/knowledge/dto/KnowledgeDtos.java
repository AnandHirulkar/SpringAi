package io.github.anandhirulkar.supportiq.knowledge.dto;

import java.time.Instant;
import java.util.List;
import java.util.Set;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

import io.github.anandhirulkar.supportiq.knowledge.KnowledgeDocument;
import io.github.anandhirulkar.supportiq.knowledge.KnowledgeStatus;

public final class KnowledgeDtos {

    private KnowledgeDtos() {
    }

    public record IngestRequest(
            @NotBlank @Size(max = 200) String title,
            @Size(max = 300) String source,
            @NotBlank @Size(max = 200_000) String content,
            Set<@Size(max = 40) String> tags) {
    }

    public record DocumentResponse(
            Long id,
            String title,
            String source,
            KnowledgeStatus status,
            int chunkCount,
            String failureReason,
            Set<String> tags,
            Instant createdAt,
            Instant updatedAt) {

        public static DocumentResponse from(KnowledgeDocument document) {
            return new DocumentResponse(
                    document.getId(),
                    document.getTitle(),
                    document.getSource(),
                    document.getStatus(),
                    document.getChunkCount(),
                    document.getFailureReason(),
                    document.getTags(),
                    document.getCreatedAt(),
                    document.getUpdatedAt());
        }
    }

    public record IngestResponse(DocumentResponse document, boolean duplicate) {
    }

    public record SearchHit(
            String documentId, String title, String source, String content, double score) {
    }

    public record SearchResponse(
            String query, int topK, double similarityThreshold, List<SearchHit> hits) {
    }
}
