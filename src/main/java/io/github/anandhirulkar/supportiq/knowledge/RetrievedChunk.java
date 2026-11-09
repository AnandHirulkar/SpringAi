package io.github.anandhirulkar.supportiq.knowledge;

import java.util.Map;

/** A retrieved knowledge fragment plus the metadata needed to cite it in an answer. */
public record RetrievedChunk(
        String documentId, String title, String source, String content, Double score) {

    public static RetrievedChunk from(String text, Map<String, Object> metadata, Double score) {
        return new RetrievedChunk(
                String.valueOf(metadata.getOrDefault("documentId", "unknown")),
                String.valueOf(metadata.getOrDefault("title", "Untitled")),
                String.valueOf(metadata.getOrDefault("source", "")),
                text,
                score);
    }

    public String citationLabel() {
        return source == null || source.isBlank() ? title : title + " (" + source + ")";
    }

    public String snippet(int maxLength) {
        String normalized = content.replaceAll("\\s+", " ").strip();
        return normalized.length() <= maxLength ? normalized : normalized.substring(0, maxLength) + "...";
    }
}
