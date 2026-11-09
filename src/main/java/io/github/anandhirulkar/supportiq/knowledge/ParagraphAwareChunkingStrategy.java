package io.github.anandhirulkar.supportiq.knowledge;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.springframework.stereotype.Component;

/**
 * Paragraph-aware splitting with a character budget and overlap.
 *
 * <p>The algorithm prefers paragraph boundaries, falls back to sentence boundaries for long
 * paragraphs and finally hard-splits single tokens that exceed the budget. Overlap preserves
 * context that would otherwise be cut in half between two chunks.
 */
@Component
public class ParagraphAwareChunkingStrategy implements ChunkingStrategy {

    private final int maxChars;
    private final int overlap;

    public ParagraphAwareChunkingStrategy(KnowledgeProperties properties) {
        this.maxChars = properties.chunkSize();
        this.overlap = properties.chunkOverlap();
    }

    @Override
    public List<String> chunk(String text) {
        if (text == null || text.isBlank()) {
            return List.of();
        }
        String normalized = text.replace("\r\n", "\n").replace('\r', '\n').trim();
        List<String> chunks = new ArrayList<>();
        StringBuilder current = new StringBuilder();

        for (String paragraph : normalized.split("\n\\s*\n")) {
            String clean = paragraph.strip();
            if (clean.isEmpty()) {
                continue;
            }
            for (String piece : splitParagraph(clean)) {
                if (current.isEmpty()) {
                    current.append(piece);
                } else if (current.length() + 2 + piece.length() <= maxChars) {
                    current.append("\n\n").append(piece);
                } else {
                    chunks.add(current.toString().strip());
                    current = new StringBuilder(overlapTail(current.toString()));
                    if (!current.isEmpty()) {
                        if (current.length() + 2 + piece.length() > maxChars) {
                            current.setLength(0);
                        } else {
                            current.append("\n\n");
                        }
                    }
                    current.append(piece);
                }
            }
        }
        if (!current.isEmpty()) {
            chunks.add(current.toString().strip());
        }
        return chunks.stream().filter(chunk -> !chunk.isBlank()).toList();
    }

    private List<String> splitParagraph(String paragraph) {
        if (paragraph.length() <= maxChars) {
            return List.of(paragraph);
        }
        List<String> pieces = new ArrayList<>();
        StringBuilder current = new StringBuilder();
        for (String sentence : paragraph.split("(?<=[.!?])\\s+")) {
            if (sentence.length() > maxChars) {
                if (!current.isEmpty()) {
                    pieces.add(current.toString().strip());
                    current.setLength(0);
                }
                pieces.addAll(hardSplit(sentence));
            } else if (current.isEmpty()) {
                current.append(sentence);
            } else if (current.length() + 1 + sentence.length() <= maxChars) {
                current.append(' ').append(sentence);
            } else {
                pieces.add(current.toString().strip());
                current = new StringBuilder(sentence);
            }
        }
        if (!current.isEmpty()) {
            pieces.add(current.toString().strip());
        }
        return pieces;
    }

    private List<String> hardSplit(String sentence) {
        List<String> pieces = new ArrayList<>();
        for (int start = 0; start < sentence.length(); start += maxChars) {
            int end = Math.min(start + maxChars, sentence.length());
            pieces.add(sentence.substring(start, end).strip());
        }
        return pieces;
    }

    private String overlapTail(String chunk) {
        if (overlap == 0 || chunk.isEmpty()) {
            return "";
        }
        int start = Math.max(0, chunk.length() - overlap);
        int boundary = chunk.indexOf(' ', start);
        String tail = boundary > 0 ? chunk.substring(boundary + 1) : chunk.substring(start);
        return tail.strip();
    }

    /** Visible for tests: the configured budget. */
    int maxChars() {
        return maxChars;
    }

    /** Visible for tests: the configured overlap. */
    int overlap() {
        return overlap;
    }
}
