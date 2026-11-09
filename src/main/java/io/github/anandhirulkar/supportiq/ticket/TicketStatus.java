package io.github.anandhirulkar.supportiq.ticket;

import java.util.Collections;
import java.util.EnumSet;
import java.util.Map;
import java.util.Set;

/**
 * Ticket lifecycle as an explicit state machine. Encoding the allowed transitions here keeps
 * workflow rules out of controllers and prevents illegal status jumps at any entry point.
 */
public enum TicketStatus {
    NEW,
    OPEN,
    PENDING,
    RESOLVED,
    CLOSED;

    private static final Map<TicketStatus, Set<TicketStatus>> ALLOWED_TRANSITIONS = Map.of(
            NEW, EnumSet.of(OPEN, CLOSED),
            OPEN, EnumSet.of(PENDING, RESOLVED, CLOSED),
            PENDING, EnumSet.of(OPEN, RESOLVED, CLOSED),
            RESOLVED, EnumSet.of(OPEN, CLOSED),
            CLOSED, Collections.unmodifiableSet(EnumSet.noneOf(TicketStatus.class)));

    public boolean canTransitionTo(TicketStatus target) {
        return ALLOWED_TRANSITIONS.getOrDefault(this, Set.of()).contains(target);
    }

    public boolean isTerminal() {
        return this == CLOSED;
    }
}
