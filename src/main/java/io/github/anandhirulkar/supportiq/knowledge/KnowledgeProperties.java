package io.github.anandhirulkar.supportiq.knowledge;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.bind.DefaultValue;

/**
 * Knowledge-ingestion tuning. Chunk size is expressed in characters to keep the splitter
 * deterministic and embeddable without a tokenizer dependency.
 */
@ConfigurationProperties("app.knowledge")
public record KnowledgeProperties(
        @DefaultValue("1200") int chunkSize,
        @DefaultValue("200") int chunkOverlap,
        @DefaultValue("5") int searchTopK,
        @DefaultValue("0.60") double similarityThreshold) {

    public KnowledgeProperties {
        if (chunkSize < 200) {
            throw new IllegalArgumentException("app.knowledge.chunk-size must be >= 200");
        }
        if (chunkOverlap < 0 || chunkOverlap >= chunkSize) {
            throw new IllegalArgumentException("app.knowledge.chunk-overlap must be >= 0 and < chunk-size");
        }
    }
}
