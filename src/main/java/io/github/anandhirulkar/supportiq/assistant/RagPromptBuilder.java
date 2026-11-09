package io.github.anandhirulkar.supportiq.assistant;

import java.util.List;

import io.github.anandhirulkar.supportiq.knowledge.RetrievedChunk;

/**
 * Builds the augmented user message: context first, numbered sources, then the question.
 * Numbering lets the model cite sources as [1], [2] and lets the API return matching citations.
 */
public final class RagPromptBuilder {

    private RagPromptBuilder() {
    }

    public static String build(String question, List<RetrievedChunk> chunks) {
        if (chunks == null || chunks.isEmpty()) {
            return """
                    No knowledge-base context was found for this question.

                    Customer question:
                    %s
                    """.formatted(question);
        }
        StringBuilder prompt = new StringBuilder("""
                Use the knowledge-base extracts below to answer the customer question.
                Cite sources using the bracketed numbers, for example [1]. If the extracts do not
                contain the answer, say so explicitly instead of inventing details.

                """);
        for (int index = 0; index < chunks.size(); index++) {
            RetrievedChunk chunk = chunks.get(index);
            prompt.append('[').append(index + 1).append("] ").append(chunk.citationLabel()).append('\n');
            prompt.append(chunk.content()).append("\n\n");
        }
        prompt.append("Customer question:\n").append(question);
        return prompt.toString();
    }
}
