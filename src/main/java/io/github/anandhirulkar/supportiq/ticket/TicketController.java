package io.github.anandhirulkar.supportiq.ticket;

import io.github.anandhirulkar.supportiq.shared.api.PageResponse;
import io.github.anandhirulkar.supportiq.ticket.dto.TicketDtos.AddMessageRequest;
import io.github.anandhirulkar.supportiq.ticket.dto.TicketDtos.AssignRequest;
import io.github.anandhirulkar.supportiq.ticket.dto.TicketDtos.ChangeStatusRequest;
import io.github.anandhirulkar.supportiq.ticket.dto.TicketDtos.CreateTicketRequest;
import io.github.anandhirulkar.supportiq.ticket.dto.TicketDtos.MessageResponse;
import io.github.anandhirulkar.supportiq.ticket.dto.TicketDtos.TicketFilter;
import io.github.anandhirulkar.supportiq.ticket.dto.TicketDtos.TicketResponse;
import io.github.anandhirulkar.supportiq.ticket.dto.TicketDtos.TicketSummary;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springdoc.core.annotations.ParameterObject;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/tickets")
@Tag(name = "Tickets", description = "Support tickets and their lifecycle")
public class TicketController {

    private final TicketService ticketService;

    public TicketController(TicketService ticketService) {
        this.ticketService = ticketService;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(summary = "Open a ticket")
    public TicketResponse create(@Valid @RequestBody CreateTicketRequest request) {
        return ticketService.create(request);
    }

    @GetMapping("/{reference}")
    @Operation(summary = "Fetch a ticket with its messages")
    public TicketResponse get(@PathVariable String reference) {
        return ticketService.get(reference);
    }

    @GetMapping
    @Operation(summary = "Filter and page tickets")
    public PageResponse<TicketSummary> search(
            @RequestParam(required = false) TicketStatus status,
            @RequestParam(required = false) TicketPriority priority,
            @RequestParam(required = false) Long customerId,
            @RequestParam(required = false) String agent,
            @RequestParam(required = false) String q,
            @ParameterObject @PageableDefault(size = 20, sort = "createdAt", direction = Sort.Direction.DESC)
                    Pageable pageable) {
        return ticketService.search(new TicketFilter(status, priority, customerId, agent, q), pageable);
    }

    @PatchMapping("/{reference}/status")
    @Operation(summary = "Move a ticket through its lifecycle")
    public TicketResponse changeStatus(
            @PathVariable String reference, @Valid @RequestBody ChangeStatusRequest request) {
        return ticketService.changeStatus(reference, request);
    }

    @PatchMapping("/{reference}/assignee")
    @Operation(summary = "Assign a ticket to an agent")
    public TicketResponse assign(@PathVariable String reference, @Valid @RequestBody AssignRequest request) {
        return ticketService.assign(reference, request.agent());
    }

    @PostMapping("/{reference}/messages")
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(summary = "Append a message to a ticket")
    public MessageResponse addMessage(
            @PathVariable String reference, @Valid @RequestBody AddMessageRequest request) {
        return ticketService.addMessage(reference, request);
    }
}
