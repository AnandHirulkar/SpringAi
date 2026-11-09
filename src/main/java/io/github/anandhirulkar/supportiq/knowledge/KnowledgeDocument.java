package io.github.anandhirulkar.supportiq.knowledge;

import java.time.Instant;
import java.util.LinkedHashSet;
import java.util.Set;

import jakarta.persistence.CollectionTable;
import jakarta.persistence.Column;
import jakarta.persistence.ElementCollection;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;

/**
 * Metadata and original text for an ingested knowledge article. Embeddings live in PostgreSQL
 * (pgvector); this table in MySQL remains the system of record for titles, sources and status.
 */
@Entity
@Table(
        name = "knowledge_documents",
        uniqueConstraints = @UniqueConstraint(name = "uk_knowledge_hash", columnNames = "content_hash"))
public class KnowledgeDocument {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 200)
    private String title;

    @Column(length = 300)
    private String source;

    @Column(name = "content_hash", nullable = false, length = 64)
    private String contentHash;

    @Column(nullable = false, columnDefinition = "LONGTEXT")
    private String content;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private KnowledgeStatus status;

    @Column(name = "chunk_count", nullable = false)
    private int chunkCount;

    @Column(name = "failure_reason", length = 500)
    private String failureReason;

    @ElementCollection
    @CollectionTable(
            name = "knowledge_document_tags",
            joinColumns = @JoinColumn(name = "document_id", foreignKey = @jakarta.persistence.ForeignKey(name = "fk_tags_document")))
    @Column(name = "tag", length = 40)
    private Set<String> tags = new LinkedHashSet<>();

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    protected KnowledgeDocument() {
        // for JPA
    }

    private KnowledgeDocument(String title, String source, String contentHash, String content, Set<String> tags) {
        this.title = title;
        this.source = source;
        this.contentHash = contentHash;
        this.content = content;
        this.status = KnowledgeStatus.PROCESSING;
        this.tags = tags == null ? new LinkedHashSet<>() : new LinkedHashSet<>(tags);
        this.createdAt = Instant.now();
        this.updatedAt = this.createdAt;
    }

    public static KnowledgeDocument processing(
            String title, String source, String contentHash, String content, Set<String> tags) {
        return new KnowledgeDocument(title, source, contentHash, content, tags);
    }

    /** Re-arms a previously failed document so ingestion can be retried with the same content. */
    public void markProcessing() {
        this.status = KnowledgeStatus.PROCESSING;
        this.chunkCount = 0;
        this.failureReason = null;
        this.updatedAt = Instant.now();
    }

    public void markReady(int chunks) {
        this.status = KnowledgeStatus.READY;
        this.chunkCount = chunks;
        this.failureReason = null;
        this.updatedAt = Instant.now();
    }

    public void markFailed(String reason) {
        this.status = KnowledgeStatus.FAILED;
        this.failureReason = reason == null ? "unknown error" : reason.substring(0, Math.min(reason.length(), 480));
        this.updatedAt = Instant.now();
    }

    public Long getId() {
        return id;
    }

    public String getTitle() {
        return title;
    }

    public String getSource() {
        return source;
    }

    public String getContentHash() {
        return contentHash;
    }

    public String getContent() {
        return content;
    }

    public KnowledgeStatus getStatus() {
        return status;
    }

    public int getChunkCount() {
        return chunkCount;
    }

    public String getFailureReason() {
        return failureReason;
    }

    public Set<String> getTags() {
        return Set.copyOf(tags);
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public Instant getUpdatedAt() {
        return updatedAt;
    }
}
