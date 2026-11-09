package io.github.anandhirulkar.supportiq.conversation;

import java.time.Instant;
import java.util.List;

/** One message in a conversation. Stored embedded inside the conversation document. */
public record ConversationTurn(
        String role,
        String content,
        List<Citation> citations,
        int promptTokens,
        int completionTokens,
        String model,
        long latencyMs,
        Instant at) {

    public ConversationTurn {
        citations = citations == null ? List.of() : List.copyOf(citations);
    }
}
