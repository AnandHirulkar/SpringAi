package io.github.anandhirulkar.supportiq.ticket;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import io.github.anandhirulkar.supportiq.customer.Customer;
import io.github.anandhirulkar.supportiq.shared.exception.BusinessRuleException;
import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import jakarta.persistence.OrderBy;
import jakarta.persistence.Table;
import jakarta.persistence.Version;

/**
 * Aggregate root for support tickets. All state changes go through intention-revealing methods so
 * the invariants (valid transitions, no messages on closed tickets) hold regardless of caller.
 */
@Entity
@Table(name = "tickets")
public class Ticket {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true, length = 20)
    private String reference;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "customer_id", nullable = false)
    private Customer customer;

    @Column(nullable = false, length = 200)
    private String subject;

    @Column(nullable = false, length = 4000)
    private String description;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private TicketStatus status;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private TicketPriority priority;

    @Column(name = "assigned_agent", length = 120)
    private String assignedAgent;

    @Version
    @Column(nullable = false)
    private long version;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    @OneToMany(mappedBy = "ticket", cascade = CascadeType.ALL, orphanRemoval = true)
    @OrderBy("createdAt ASC")
    private List<TicketMessage> messages = new ArrayList<>();

    protected Ticket() {
        // for JPA
    }

    private Ticket(String reference, Customer customer, String subject, String description, TicketPriority priority) {
        this.reference = reference;
        this.customer = customer;
        this.subject = subject;
        this.description = description;
        this.priority = priority == null ? TicketPriority.MEDIUM : priority;
        this.status = TicketStatus.NEW;
        this.createdAt = Instant.now();
        this.updatedAt = this.createdAt;
    }

    public static Ticket open(
            Customer customer, String subject, String description, TicketPriority priority) {
        return new Ticket(newReference(), customer, subject, description, priority);
    }

    private static String newReference() {
        return "TKT-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase();
    }

    public void changeStatus(TicketStatus target) {
        if (target == null) {
            throw new BusinessRuleException("Target status is required");
        }
        if (target == status) {
            return;
        }
        if (!status.canTransitionTo(target)) {
            throw new BusinessRuleException(
                    "Illegal transition from %s to %s".formatted(status, target));
        }
        this.status = target;
        touch();
    }

    public void changePriority(TicketPriority target) {
        if (target == null) {
            throw new BusinessRuleException("Target priority is required");
        }
        this.priority = target;
        touch();
    }

    public void assignTo(String agent) {
        if (agent == null || agent.isBlank()) {
            throw new BusinessRuleException("Agent name is required");
        }
        if (status == TicketStatus.CLOSED) {
            throw new BusinessRuleException("Cannot assign a closed ticket");
        }
        this.assignedAgent = agent.trim();
        touch();
    }

    public TicketMessage addMessage(TicketMessageAuthor authorType, String authorName, String body) {
        if (status == TicketStatus.CLOSED) {
            throw new BusinessRuleException("Cannot add a message to a closed ticket");
        }
        TicketMessage message = new TicketMessage(this, authorType, authorName, body);
        messages.add(message);
        touch();
        return message;
    }

    private void touch() {
        this.updatedAt = Instant.now();
    }

    public Long getId() {
        return id;
    }

    public String getReference() {
        return reference;
    }

    public Customer getCustomer() {
        return customer;
    }

    public String getSubject() {
        return subject;
    }

    public String getDescription() {
        return description;
    }

    public TicketStatus getStatus() {
        return status;
    }

    public TicketPriority getPriority() {
        return priority;
    }

    public String getAssignedAgent() {
        return assignedAgent;
    }

    public long getVersion() {
        return version;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public Instant getUpdatedAt() {
        return updatedAt;
    }

    public List<TicketMessage> getMessages() {
        return List.copyOf(messages);
    }
}
