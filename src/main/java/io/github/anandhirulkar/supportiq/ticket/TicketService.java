package io.github.anandhirulkar.supportiq.ticket;

import java.util.ArrayList;
import java.util.List;

import io.github.anandhirulkar.supportiq.customer.Customer;
import io.github.anandhirulkar.supportiq.customer.CustomerRepository;
import io.github.anandhirulkar.supportiq.shared.api.PageResponse;
import io.github.anandhirulkar.supportiq.shared.events.EventPublisher;
import io.github.anandhirulkar.supportiq.shared.events.TicketCreatedEvent;
import io.github.anandhirulkar.supportiq.shared.events.TicketStatusChangedEvent;
import io.github.anandhirulkar.supportiq.shared.exception.ResourceNotFoundException;
import io.github.anandhirulkar.supportiq.ticket.dto.TicketDtos.AddMessageRequest;
import io.github.anandhirulkar.supportiq.ticket.dto.TicketDtos.ChangeStatusRequest;
import io.github.anandhirulkar.supportiq.ticket.dto.TicketDtos.CreateTicketRequest;
import io.github.anandhirulkar.supportiq.ticket.dto.TicketDtos.MessageResponse;
import io.github.anandhirulkar.supportiq.ticket.dto.TicketDtos.TicketFilter;
import io.github.anandhirulkar.supportiq.ticket.dto.TicketDtos.TicketResponse;
import io.github.anandhirulkar.supportiq.ticket.dto.TicketDtos.TicketSummary;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

@Service
public class TicketService {

    private final TicketRepository tickets;
    private final CustomerRepository customers;
    private final EventPublisher events;

    public TicketService(TicketRepository tickets, CustomerRepository customers, EventPublisher events) {
        this.tickets = tickets;
        this.customers = customers;
        this.events = events;
    }

    @Transactional
    public TicketResponse create(CreateTicketRequest request) {
        Customer customer = customers
                .findById(request.customerId())
                .orElseThrow(() -> new ResourceNotFoundException("Customer", request.customerId()));
        Ticket ticket = Ticket.open(customer, request.subject().trim(), request.description().trim(), request.priority());
        tickets.save(ticket);
        events.publish(TicketCreatedEvent.of(
                ticket.getReference(), customer.getId(), ticket.getSubject(), ticket.getPriority().name()));
        return TicketResponse.from(ticket);
    }

    @Transactional(readOnly = true)
    public TicketResponse get(String reference) {
        return TicketResponse.from(require(reference));
    }

    @Transactional(readOnly = true)
    public PageResponse<TicketSummary> search(TicketFilter filter, Pageable pageable) {
        List<Specification<Ticket>> specifications = new ArrayList<>();
        if (filter.status() != null) {
            specifications.add(TicketSpecifications.hasStatus(filter.status()));
        }
        if (filter.priority() != null) {
            specifications.add(TicketSpecifications.hasPriority(filter.priority()));
        }
        if (filter.customerId() != null) {
            specifications.add(TicketSpecifications.belongsToCustomer(filter.customerId()));
        }
        if (StringUtils.hasText(filter.agent())) {
            specifications.add(TicketSpecifications.assignedTo(filter.agent()));
        }
        if (StringUtils.hasText(filter.q())) {
            specifications.add(TicketSpecifications.matchesText(filter.q().trim()));
        }
        Specification<Ticket> specification = specifications.stream()
                .reduce(Specification::and)
                .orElseGet(() -> (root, query, cb) -> cb.conjunction());
        Page<Ticket> page = tickets.findAll(specification, pageable);
        return PageResponse.from(page, TicketSummary::from);
    }

    @Transactional
    public TicketResponse changeStatus(String reference, ChangeStatusRequest request) {
        Ticket ticket = require(reference);
        TicketStatus previous = ticket.getStatus();
        ticket.changeStatus(request.status());
        events.publish(TicketStatusChangedEvent.of(reference, previous.name(), ticket.getStatus().name()));
        return TicketResponse.from(ticket);
    }

    @Transactional
    public TicketResponse assign(String reference, String agent) {
        Ticket ticket = require(reference);
        ticket.assignTo(agent);
        return TicketResponse.from(ticket);
    }

    @Transactional
    public MessageResponse addMessage(String reference, AddMessageRequest request) {
        Ticket ticket = require(reference);
        TicketMessage message = ticket.addMessage(request.authorType(), request.authorName().trim(), request.body().trim());
        tickets.flush();
        return MessageResponse.from(message);
    }

    @Transactional
    public void applyTriage(String reference, TicketPriority priority) {
        require(reference).changePriority(priority);
    }

    @Transactional(readOnly = true)
    public Ticket require(String reference) {
        return tickets.findByReference(reference)
                .orElseThrow(() -> new ResourceNotFoundException("Ticket", reference));
    }
}
