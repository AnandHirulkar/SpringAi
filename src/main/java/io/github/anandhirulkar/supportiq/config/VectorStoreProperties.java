package io.github.anandhirulkar.supportiq.config;

import org.springframework.ai.vectorstore.pgvector.PgVectorStore;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.bind.DefaultValue;

/**
 * The vector store uses a dedicated PostgreSQL connection rather than the application's MySQL
 * datasource, so the AI store can be scaled, backed up and secured independently.
 */
@ConfigurationProperties("app.vector-store")
public record VectorStoreProperties(
        String url,
        String username,
        String password,
        @DefaultValue("1536") int dimensions,
        @DefaultValue("HNSW") PgVectorStore.PgIndexType indexType,
        @DefaultValue("COSINE_DISTANCE") PgVectorStore.PgDistanceType distanceType,
        @DefaultValue("true") boolean initializeSchema,
        @DefaultValue("public") String schemaName,
        @DefaultValue("vector_store") String tableName) {
}
