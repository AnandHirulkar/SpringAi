package io.github.anandhirulkar.supportiq.ticket;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import io.github.anandhirulkar.supportiq.customer.Customer;
import io.github.anandhirulkar.supportiq.shared.exception.BusinessRuleException;
import org.junit.jupiter.api.Test;

class TicketTest {

    private final Customer customer =
            Customer.register("Ada Lovelace", "ada@acme.test", "Acme", null);

    @Test
    void newTicketsStartNewWithAGeneratedReference() {
        Ticket ticket = Ticket.open(customer, "Export fails", "Details", TicketPriority.HIGH);

        assertThat(ticket.getStatus()).isEqualTo(TicketStatus.NEW);
        assertThat(ticket.getPriority()).isEqualTo(TicketPriority.HIGH);
        assertThat(ticket.getReference()).startsWith("TKT-").hasSize(12);
    }

    @Test
    void illegalTransitionsAreRejected() {
        Ticket ticket = Ticket.open(customer, "Export fails", "Details", TicketPriority.MEDIUM);

        assertThatThrownBy(() -> ticket.changeStatus(TicketStatus.RESOLVED))
                .isInstanceOf(BusinessRuleException.class)
                .hasMessageContaining("Illegal transition");

        ticket.changeStatus(TicketStatus.OPEN);
        ticket.changeStatus(TicketStatus.RESOLVED);

        assertThat(ticket.getStatus()).isEqualTo(TicketStatus.RESOLVED);
    }

    @Test
    void closedTicketsAreImmutable() {
        Ticket ticket = Ticket.open(customer, "Export fails", "Details", TicketPriority.LOW);
        ticket.changeStatus(TicketStatus.CLOSED);

        assertThat(ticket.getStatus().isTerminal()).isTrue();
        assertThatThrownBy(() -> ticket.addMessage(TicketMessageAuthor.AGENT, "nova", "hello"))
                .isInstanceOf(BusinessRuleException.class)
                .hasMessageContaining("closed");
        assertThatThrownBy(() -> ticket.assignTo("nova"))
                .isInstanceOf(BusinessRuleException.class)
                .hasMessageContaining("closed");
    }

    @Test
    void messagesAreOrderedPartOfTheAggregate() {
        Ticket ticket = Ticket.open(customer, "Export fails", "Details", TicketPriority.MEDIUM);
        ticket.addMessage(TicketMessageAuthor.CUSTOMER, "Ada", "first");
        ticket.addMessage(TicketMessageAuthor.AGENT, "nova", "second");

        assertThat(ticket.getMessages()).hasSize(2);
        assertThat(ticket.getMessages().getFirst().getBody()).isEqualTo("first");
        assertThat(ticket.getMessages().getLast().getAuthorType()).isEqualTo(TicketMessageAuthor.AGENT);
    }
}
