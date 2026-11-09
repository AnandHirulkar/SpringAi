package io.github.anandhirulkar.supportiq.ticket;

import org.springframework.data.jpa.domain.Specification;

/**
 * Composable query fragments. Keeping them in one place avoids the "findByXxxAndYyyAndZzz"
 * method explosion and lets the service assemble only the filters the caller supplied.
 */
public final class TicketSpecifications {

    private TicketSpecifications() {
    }

    public static Specification<Ticket> hasStatus(TicketStatus status) {
        return (root, query, cb) -> cb.equal(root.get("status"), status);
    }

    public static Specification<Ticket> hasPriority(TicketPriority priority) {
        return (root, query, cb) -> cb.equal(root.get("priority"), priority);
    }

    public static Specification<Ticket> belongsToCustomer(Long customerId) {
        return (root, query, cb) -> cb.equal(root.get("customer").get("id"), customerId);
    }

    public static Specification<Ticket> assignedTo(String agent) {
        return (root, query, cb) -> cb.equal(cb.lower(root.get("assignedAgent")), agent.toLowerCase());
    }

    public static Specification<Ticket> matchesText(String term) {
        String pattern = "%" + term.toLowerCase() + "%";
        return (root, query, cb) -> cb.or(
                cb.like(cb.lower(root.get("subject")), pattern),
                cb.like(cb.lower(root.get("description")), pattern),
                cb.like(cb.lower(root.get("reference")), pattern));
    }
}
