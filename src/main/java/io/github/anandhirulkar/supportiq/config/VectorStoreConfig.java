package io.github.anandhirulkar.supportiq.config;

import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import org.springframework.ai.embedding.EmbeddingModel;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.ai.vectorstore.pgvector.PgVectorStore;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.jdbc.core.JdbcTemplate;

/**
 * The application has three persistence technologies with different connectivity needs:
 * MySQL (JPA, primary datasource), MongoDB (documents) and PostgreSQL/pgvector (embeddings).
 *
 * <p>Spring Boot auto-configures {@code spring.datasource} for MySQL. The pgvector store therefore
 * gets its own small Hikari pool and JdbcTemplate, and the pgvector auto-configuration is excluded
 * in {@code application.yml} so it cannot bind the vector table to the MySQL database.
 */
@Configuration(proxyBeanMethods = false)
public class VectorStoreConfig {

    @Bean(name = "vectorDataSource", destroyMethod = "close")
    public HikariDataSource vectorDataSource(VectorStoreProperties properties) {
        HikariConfig config = new HikariConfig();
        config.setPoolName("pgvector-pool");
        config.setJdbcUrl(properties.url());
        config.setUsername(properties.username());
        config.setPassword(properties.password());
        config.setMaximumPoolSize(4);
        config.setMinimumIdle(1);
        config.setConnectionTimeout(10_000);
        return new HikariDataSource(config);
    }

    @Bean(name = "vectorJdbcTemplate")
    public JdbcTemplate vectorJdbcTemplate(@Qualifier("vectorDataSource") HikariDataSource dataSource) {
        return new JdbcTemplate(dataSource);
    }

    @Bean
    @Primary
    public VectorStore vectorStore(
            @Qualifier("vectorJdbcTemplate") JdbcTemplate jdbcTemplate,
            EmbeddingModel embeddingModel,
            VectorStoreProperties properties) {
        return PgVectorStore.builder(jdbcTemplate, embeddingModel)
                .dimensions(properties.dimensions())
                .indexType(properties.indexType())
                .distanceType(properties.distanceType())
                .initializeSchema(properties.initializeSchema())
                .schemaName(properties.schemaName())
                .vectorTableName(properties.tableName())
                .maxDocumentBatchSize(1000)
                .build();
    }
}
