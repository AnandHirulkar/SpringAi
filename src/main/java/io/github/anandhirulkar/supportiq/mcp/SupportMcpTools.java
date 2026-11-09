package io.github.anandhirulkar.supportiq.mcp;

import io.github.anandhirulkar.supportiq.knowledge.KnowledgeQueryService;
import io.github.anandhirulkar.supportiq.shared.api.PageResponse;
import io.github.anandhirulkar.supportiq.shared.exception.ResourceNotFoundException;
import io.github.anandhirulkar.supportiq.ticket.TicketPriority;
import io.github.anandhirulkar.supportiq.ticket.TicketService;
import io.github.anandhirulkar.supportiq.ticket.TicketStatus;
import io.github.anandhirulkar.supportiq.ticket.dto.TicketDtos.CreateTicketRequest;
import io.github.anandhirulkar.supportiq.ticket.dto.TicketDtos.TicketFilter;
import io.github.anandhirulkar.supportiq.ticket.dto.TicketDtos.TicketSummary;
import org.springframework.ai.mcp.annotation.McpTool;
import org.springframework.ai.mcp.annotation.McpToolParam;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Component;

/**
 * Model Context Protocol server surface (Streamable HTTP at {@code /mcp}).
 *
 * <p>These tools let external agents such as Claude Desktop, Cursor or an IDE assistant query the
 * same support data the REST API exposes. Read-only tools advertise protocol hints so clients can
 * reason about safety; the one write tool is explicitly marked destructive-capable.
 */
@Component
public class SupportMcpTools {

    private final KnowledgeQueryService knowledge;
    private final TicketService tickets;

    public SupportMcpTools(KnowledgeQueryService knowledge, TicketService tickets) {
        this.knowledge = knowledge;
        this.tickets = tickets;
    }

    @McpTool(
            name = "search_knowledge_base",
            description = "Semantic search over SupportIQ's knowledge base. Returns the best matching passages with scores.",
            annotations = @McpTool.McpAnnotations(
                    readOnlyHint = true,
                    destructiveHint = false,
                    idempotentHint = true,
                    openWorldHint = false))
    public String searchKnowledgeBase(
            @McpToolParam(description = "Natural-language search query", required = true) String query,
            @McpToolParam(description = "Maximum number of passages (1-20)", required = false) Integer topK) {
        var response = knowledge.search(query, topK == null ? 5 : topK, null);
        if (response.hits().isEmpty()) {
            return "No knowledge-base matches for: " + query;
        }
        StringBuilder builder = new StringBuilder("Matches for \"").append(query).append("\":\n");
        for (int index = 0; index < response.hits().size(); index++) {
            var hit = response.hits().get(index);
            builder.append('[')
                    .append(index + 1)
                    .append("] ")
                    .append(hit.title())
                    .append(" (document ")
                    .append(hit.documentId())
                    .append(", score ")
                    .append("%.3f".formatted(hit.score()))
                    .append(")\n")
                    .append(truncate(hit.content(), 700))
                    .append("\n\n");
        }
        return builder.toString().strip();
    }

    @McpTool(
            name = "get_ticket",
            description = "Look up a support ticket by reference.",
            annotations = @McpTool.McpAnnotations(
                    readOnlyHint = true, destructiveHint = false, idempotentHint = true, openWorldHint = false))
    public String getTicket(@McpToolParam(description = "Ticket reference such as TKT-1A2B3C4D", required = true)
            String reference) {
        try {
            var ticket = tickets.get(reference);
            return """
                    reference: %s
                    subject: %s
                    status: %s
                    priority: %s
                    customerId: %s
                    assignedAgent: %s
                    description: %s
                    """.formatted(
                    ticket.reference(),
                    ticket.subject(),
                    ticket.status(),
                    ticket.priority(),
                    ticket.customerId(),
                    ticket.assignedAgent() == null ? "unassigned" : ticket.assignedAgent(),
                    ticket.description());
        } catch (ResourceNotFoundException notFound) {
            return "Ticket not found: " + reference;
        }
    }

    @McpTool(
            name = "list_open_tickets",
            description = "List up to ten open tickets for a customer id.",
            annotations = @McpTool.McpAnnotations(
                    readOnlyHint = true, destructiveHint = false, idempotentHint = true, openWorldHint = false))
    public String listOpenTickets(@McpToolParam(description = "Numeric customer id", required = true) Long customerId) {
        PageResponse<TicketSummary> page = tickets.search(
                new TicketFilter(TicketStatus.OPEN, null, customerId, null, null), PageRequest.of(0, 10));
        if (page.content().isEmpty()) {
            return "No open tickets for customer " + customerId;
        }
        return page.content().stream()
                .map(ticket -> "%s | %s | %s | %s"
                        .formatted(ticket.reference(), ticket.priority(), ticket.status(), ticket.subject()))
                .reduce((left, right) -> left + "\n" + right)
                .orElse("");
    }

    @McpTool(
            name = "create_ticket",
            description = "Create a support ticket for an existing customer. Side-effecting; only call when the user asks.",
            annotations = @McpTool.McpAnnotations(
                    readOnlyHint = false, destructiveHint = false, idempotentHint = false, openWorldHint = false))
    public String createTicket(
            @McpToolParam(description = "Numeric customer id", required = true) Long customerId,
            @McpToolParam(description = "Ticket subject", required = true) String subject,
            @McpToolParam(description = "Ticket description", required = true) String description,
            @McpToolParam(description = "LOW, MEDIUM, HIGH or URGENT", required = false) String priority) {
        TicketPriority parsed = parsePriority(priority);
        var created = tickets.create(new CreateTicketRequest(customerId, subject, description, parsed));
        return "Created %s with status %s and priority %s"
                .formatted(created.reference(), created.status(), created.priority());
    }

    private static TicketPriority parsePriority(String priority) {
        if (priority == null || priority.isBlank()) {
            return TicketPriority.MEDIUM;
        }
        try {
            return TicketPriority.valueOf(priority.trim().toUpperCase());
        } catch (IllegalArgumentException ignored) {
            return TicketPriority.MEDIUM;
        }
    }

    private static String truncate(String value, int max) {
        String normalized = value == null ? "" : value.strip();
        return normalized.length() <= max ? normalized : normalized.substring(0, max) + "...";
    }
}
