package io.github.anandhirulkar.supportiq.knowledge;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;
import java.util.List;
import java.util.Map;
import java.util.Set;

import io.github.anandhirulkar.supportiq.knowledge.dto.KnowledgeDtos.DocumentResponse;
import io.github.anandhirulkar.supportiq.knowledge.dto.KnowledgeDtos.IngestRequest;
import io.github.anandhirulkar.supportiq.knowledge.dto.KnowledgeDtos.IngestResponse;
import io.github.anandhirulkar.supportiq.shared.events.DocumentIngestedEvent;
import io.github.anandhirulkar.supportiq.shared.events.EventPublisher;
import io.github.anandhirulkar.supportiq.shared.exception.BusinessRuleException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.document.Document;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.support.TransactionTemplate;

/**
 * Ingestion pipeline: persist metadata (MySQL) -> chunk -> embed and store vectors (pgvector).
 *
 * <p>Two systems cannot share one ACID transaction, so the work is split into explicit steps: the
 * document is first committed as {@code PROCESSING}, then vectors are written, then the document
 * is marked {@code READY} or {@code FAILED}. Re-running ingestion of identical content is a no-op
 * thanks to the content hash, which makes the endpoint safe to retry.
 */
@Service
public class DocumentIngestionService {

    private static final Logger log = LoggerFactory.getLogger(DocumentIngestionService.class);

    private final KnowledgeDocumentRepository documents;
    private final ChunkingStrategy chunkingStrategy;
    private final VectorStore vectorStore;
    private final EventPublisher events;
    private final TransactionTemplate transactions;

    public DocumentIngestionService(
            KnowledgeDocumentRepository documents,
            ChunkingStrategy chunkingStrategy,
            VectorStore vectorStore,
            EventPublisher events,
            TransactionTemplate transactions) {
        this.documents = documents;
        this.chunkingStrategy = chunkingStrategy;
        this.vectorStore = vectorStore;
        this.events = events;
        this.transactions = transactions;
    }

    public IngestResponse ingest(IngestRequest request) {
        String content = request.content().strip();
        String hash = sha256(content);

        var existing = documents.findByContentHash(hash);
        KnowledgeDocument document;
        if (existing.isPresent()) {
            KnowledgeDocument found = existing.get();
            if (found.getStatus() == KnowledgeStatus.READY
                    || found.getStatus() == KnowledgeStatus.PROCESSING) {
                return new IngestResponse(DocumentResponse.from(found), true);
            }
            // A previous attempt failed: re-arm the same document instead of returning stale failure
            // forever. Vector ids are deterministic, so embeddings overwrite rather than duplicate.
            document = transactions.execute(status -> documents
                    .findById(found.getId())
                    .map(managed -> {
                        managed.markProcessing();
                        return managed;
                    })
                    .orElseThrow(() -> new IllegalStateException("Document disappeared before retry")));
        } else {
            try {
                document = transactions.execute(status -> documents.save(KnowledgeDocument.processing(
                        request.title().trim(),
                        request.source(),
                        hash,
                        content,
                        request.tags() == null ? Set.of() : request.tags())));
            } catch (DataIntegrityViolationException race) {
                // A concurrent request won the unique-hash race: return its result instead of failing.
                return documents
                        .findByContentHash(hash)
                        .map(found -> new IngestResponse(DocumentResponse.from(found), true))
                        .orElseThrow(() -> race);
            }
        }
        if (document == null) {
            throw new IllegalStateException("Document could not be persisted");
        }
        Long documentId = document.getId();

        try {
            List<String> chunks = chunkingStrategy.chunk(content);
            if (chunks.isEmpty()) {
                throw new BusinessRuleException("The document does not contain any usable text");
            }
            vectorStore.add(toVectorDocuments(documentId, chunks, request));
            int chunkCount = chunks.size();

            KnowledgeDocument ready = transactions.execute(status -> {
                KnowledgeDocument managed = documents
                        .findById(documentId)
                        .orElseThrow(() -> new IllegalStateException("Document disappeared during ingestion"));
                managed.markReady(chunkCount);
                return managed;
            });
            if (ready == null) {
                throw new IllegalStateException("Document could not be updated after ingestion");
            }
            events.publish(DocumentIngestedEvent.of(ready.getId(), ready.getTitle(), chunkCount));
            log.info("Ingested knowledge document {} ({} chunks)", ready.getId(), chunkCount);
            return new IngestResponse(DocumentResponse.from(ready), false);
        } catch (RuntimeException failure) {
            markFailed(documentId, failure);
            throw failure;
        }
    }

    private List<Document> toVectorDocuments(Long documentId, List<String> chunks, IngestRequest request) {
        String tags = request.tags() == null ? "" : String.join(",", request.tags());
        return java.util.stream.IntStream.range(0, chunks.size())
                .mapToObj(index -> new Document(
                        documentId + ":" + index,
                        chunks.get(index),
                        Map.of(
                                "documentId", String.valueOf(documentId),
                                "title", request.title(),
                                "source", request.source() == null ? "" : request.source(),
                                "chunkIndex", index,
                                "tags", tags)))
                .toList();
    }

    private void markFailed(Long documentId, RuntimeException failure) {
        try {
            transactions.executeWithoutResult(status -> documents.findById(documentId).ifPresent(document ->
                    document.markFailed(failure.getMessage())));
        } catch (RuntimeException secondary) {
            log.warn("Could not mark document {} as failed", documentId, secondary);
        }
        log.error("Knowledge ingestion failed for document {}", documentId, failure);
    }

    private static String sha256(String value) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            return HexFormat.of().formatHex(digest.digest(value.getBytes(StandardCharsets.UTF_8)));
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-256 is required by the JDK", e);
        }
    }
}
