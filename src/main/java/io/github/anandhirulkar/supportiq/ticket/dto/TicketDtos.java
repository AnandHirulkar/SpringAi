package io.github.anandhirulkar.supportiq.ticket.dto;

import java.time.Instant;
import java.util.List;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import io.github.anandhirulkar.supportiq.ticket.Ticket;
import io.github.anandhirulkar.supportiq.ticket.TicketMessage;
import io.github.anandhirulkar.supportiq.ticket.TicketMessageAuthor;
import io.github.anandhirulkar.supportiq.ticket.TicketPriority;
import io.github.anandhirulkar.supportiq.ticket.TicketStatus;

public final class TicketDtos {

    private TicketDtos() {
    }

    public record CreateTicketRequest(
            @NotNull Long customerId,
            @NotBlank @Size(max = 200) String subject,
            @NotBlank @Size(max = 4000) String description,
            TicketPriority priority) {
    }

    public record AddMessageRequest(
            @NotNull TicketMessageAuthor authorType,
            @NotBlank @Size(max = 120) String authorName,
            @NotBlank @Size(max = 8000) String body) {
    }

    public record ChangeStatusRequest(@NotNull TicketStatus status) {
    }

    public record AssignRequest(@NotBlank @Size(max = 120) String agent) {
    }

    public record MessageResponse(
            Long id,
            TicketMessageAuthor authorType,
            String authorName,
            String body,
            Instant createdAt) {

        public static MessageResponse from(TicketMessage message) {
            return new MessageResponse(
                    message.getId(),
                    message.getAuthorType(),
                    message.getAuthorName(),
                    message.getBody(),
                    message.getCreatedAt());
        }
    }

    public record TicketResponse(
            Long id,
            String reference,
            Long customerId,
            String customerName,
            String subject,
            String description,
            TicketStatus status,
            TicketPriority priority,
            String assignedAgent,
            long version,
            Instant createdAt,
            Instant updatedAt,
            List<MessageResponse> messages) {

        public static TicketResponse from(Ticket ticket) {
            return new TicketResponse(
                    ticket.getId(),
                    ticket.getReference(),
                    ticket.getCustomer().getId(),
                    ticket.getCustomer().getName(),
                    ticket.getSubject(),
                    ticket.getDescription(),
                    ticket.getStatus(),
                    ticket.getPriority(),
                    ticket.getAssignedAgent(),
                    ticket.getVersion(),
                    ticket.getCreatedAt(),
                    ticket.getUpdatedAt(),
                    ticket.getMessages().stream().map(MessageResponse::from).toList());
        }
    }

    public record TicketSummary(
            Long id,
            String reference,
            Long customerId,
            String customerName,
            String subject,
            TicketStatus status,
            TicketPriority priority,
            String assignedAgent,
            Instant createdAt,
            Instant updatedAt) {

        public static TicketSummary from(Ticket ticket) {
            return new TicketSummary(
                    ticket.getId(),
                    ticket.getReference(),
                    ticket.getCustomer().getId(),
                    ticket.getCustomer().getName(),
                    ticket.getSubject(),
                    ticket.getStatus(),
                    ticket.getPriority(),
                    ticket.getAssignedAgent(),
                    ticket.getCreatedAt(),
                    ticket.getUpdatedAt());
        }
    }

    public record TicketFilter(
            TicketStatus status, TicketPriority priority, Long customerId, String agent, String q) {
    }
}
