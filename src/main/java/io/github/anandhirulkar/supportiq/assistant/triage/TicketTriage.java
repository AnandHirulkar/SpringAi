package io.github.anandhirulkar.supportiq.assistant.triage;

import io.github.anandhirulkar.supportiq.ticket.TicketPriority;

/**
 * Structured-output target. Spring AI derives a JSON schema from this record, and the model's
 * answer is parsed back into it, so downstream code gets typed data instead of prose.
 */
public record TicketTriage(
        TicketPriority priority,
        String category,
        String sentiment,
        String summary,
        String suggestedReply,
        int confidence) {
}
