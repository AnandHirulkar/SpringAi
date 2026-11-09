package io.github.anandhirulkar.supportiq.assistant.tools;

import io.github.anandhirulkar.supportiq.knowledge.KnowledgeRetrievalService;
import io.github.anandhirulkar.supportiq.knowledge.RetrievedChunk;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.ai.tool.annotation.ToolParam;
import org.springframework.stereotype.Component;

@Component
public class KnowledgeTools {

    private final KnowledgeRetrievalService retrieval;

    public KnowledgeTools(KnowledgeRetrievalService retrieval) {
        this.retrieval = retrieval;
    }

    @Tool(
            name = "searchKnowledgeBase",
            description = "Search the internal knowledge base for documentation relevant to a customer question.")
    public String searchKnowledgeBase(@ToolParam(description = "Search query in natural language") String query) {
        var chunks = retrieval.retrieve(query, 4, null);
        if (chunks.isEmpty()) {
            return "No knowledge-base matches for: " + query;
        }
        StringBuilder result = new StringBuilder();
        for (int index = 0; index < chunks.size(); index++) {
            RetrievedChunk chunk = chunks.get(index);
            result.append('[')
                    .append(index + 1)
                    .append("] ")
                    .append(chunk.citationLabel())
                    .append('\n')
                    .append(chunk.snippet(600))
                    .append("\n\n");
        }
        return result.toString().strip();
    }
}
