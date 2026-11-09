package io.github.anandhirulkar.supportiq.ticket;

import java.util.Optional;

import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

public interface TicketRepository extends JpaRepository<Ticket, Long>, JpaSpecificationExecutor<Ticket> {

    @EntityGraph(attributePaths = {"customer", "messages"})
    Optional<Ticket> findByReference(String reference);

    boolean existsByReference(String reference);

    long countByStatus(TicketStatus status);
}
