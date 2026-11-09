package io.github.anandhirulkar.supportiq.ticket;

import java.time.Instant;

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
import jakarta.persistence.Table;

/** Part of the Ticket aggregate: only the aggregate root hands out references to messages. */
@Entity
@Table(name = "ticket_messages")
public class TicketMessage {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "ticket_id", nullable = false)
    private Ticket ticket;

    @Enumerated(EnumType.STRING)
    @Column(name = "author_type", nullable = false, length = 20)
    private TicketMessageAuthor authorType;

    @Column(name = "author_name", nullable = false, length = 120)
    private String authorName;

    @Column(nullable = false, length = 8000)
    private String body;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    protected TicketMessage() {
        // for JPA
    }

    TicketMessage(Ticket ticket, TicketMessageAuthor authorType, String authorName, String body) {
        this.ticket = ticket;
        this.authorType = authorType;
        this.authorName = authorName;
        this.body = body;
        this.createdAt = Instant.now();
    }

    public Long getId() {
        return id;
    }

    public TicketMessageAuthor getAuthorType() {
        return authorType;
    }

    public String getAuthorName() {
        return authorName;
    }

    public String getBody() {
        return body;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }
}
