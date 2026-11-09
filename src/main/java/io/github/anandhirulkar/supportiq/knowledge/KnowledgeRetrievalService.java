package io.github.anandhirulkar.supportiq.knowledge;

import java.util.List;

import org.springframework.ai.vectorstore.SearchRequest;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.stereotype.Service;

/**
 * Semantic retrieval is isolated behind this service so the assistant never depends on pgvector
 * specifics and the retrieval policy (top-k, similarity floor) can evolve independently.
 */
@Service
public class KnowledgeRetrievalService {

    private final VectorStore vectorStore;
    private final KnowledgeProperties properties;

    public KnowledgeRetrievalService(VectorStore vectorStore, KnowledgeProperties properties) {
        this.vectorStore = vectorStore;
        this.properties = properties;
    }

    public List<RetrievedChunk> retrieve(String query, Integer topK, Double similarityThreshold) {
        if (query == null || query.isBlank()) {
            return List.of();
        }
        SearchRequest request = SearchRequest.builder()
                .query(query)
                .topK(topK == null || topK < 1 ? properties.searchTopK() : Math.min(topK, 20))
                .similarityThreshold(
                        similarityThreshold == null ? properties.similarityThreshold() : similarityThreshold)
                .build();
        return vectorStore.similaritySearch(request).stream()
                .map(document -> RetrievedChunk.from(document.getText(), document.getMetadata(), document.getScore()))
                .toList();
    }
}
