package io.github.anandhirulkar.supportiq.assistant.tools;

import io.github.anandhirulkar.supportiq.shared.api.PageResponse;
import io.github.anandhirulkar.supportiq.ticket.TicketService;
import io.github.anandhirulkar.supportiq.ticket.TicketStatus;
import io.github.anandhirulkar.supportiq.ticket.dto.TicketDtos.TicketFilter;
import io.github.anandhirulkar.supportiq.ticket.dto.TicketDtos.TicketResponse;
import io.github.anandhirulkar.supportiq.ticket.dto.TicketDtos.TicketSummary;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.ai.tool.annotation.ToolParam;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Component;

/**
 * Tool-calling surface for the model. Methods return plain text so the model gets compact,
 * predictable results and the application keeps control over what is exposed.
 */
@Component
public class TicketTools {

    private final TicketService ticketService;

    public TicketTools(TicketService ticketService) {
        this.ticketService = ticketService;
    }

    @Tool(name = "getTicket", description = "Look up a support ticket by its reference, for example TKT-1A2B3C4D.")
    public String getTicket(@ToolParam(description = "Ticket reference") String reference) {
        TicketResponse ticket = ticketService.get(reference);
        return """
                reference: %s
                status: %s
                priority: %s
                assignedAgent: %s
                subject: %s
                description: %s
                """.formatted(
                ticket.reference(),
                ticket.status(),
                ticket.priority(),
                ticket.assignedAgent() == null ? "unassigned" : ticket.assignedAgent(),
                ticket.subject(),
                ticket.description());
    }

    @Tool(name = "listOpenTickets", description = "List a customer's open tickets. Use the numeric customer id.")
    public String listOpenTickets(@ToolParam(description = "Numeric customer id") Long customerId) {
        PageResponse<TicketSummary> page = ticketService.search(
                new TicketFilter(TicketStatus.OPEN, null, customerId, null, null), PageRequest.of(0, 10));
        if (page.content().isEmpty()) {
            return "No open tickets for customer " + customerId;
        }
        return page.content().stream()
                .map(ticket -> "%s | %s | %s".formatted(ticket.reference(), ticket.priority(), ticket.subject()))
                .reduce((left, right) -> left + "\n" + right)
                .orElse("");
    }
}
