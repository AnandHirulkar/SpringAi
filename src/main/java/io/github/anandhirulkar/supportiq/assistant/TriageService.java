package io.github.anandhirulkar.supportiq.assistant;

import java.util.List;

import io.github.anandhirulkar.supportiq.assistant.chat.ChatCommand;
import io.github.anandhirulkar.supportiq.assistant.chat.ChatPort;
import io.github.anandhirulkar.supportiq.assistant.chat.ChatRole;
import io.github.anandhirulkar.supportiq.assistant.chat.ChatTurn;
import io.github.anandhirulkar.supportiq.assistant.dto.AssistantDtos.TriageResponse;
import io.github.anandhirulkar.supportiq.assistant.triage.TicketTriage;
import io.github.anandhirulkar.supportiq.ticket.TicketPriority;
import io.github.anandhirulkar.supportiq.ticket.TicketService;
import io.github.anandhirulkar.supportiq.ticket.dto.TicketDtos.TicketResponse;
import org.springframework.stereotype.Service;

@Service
public class TriageService {

    private final TicketService ticketService;
    private final ChatPort chatPort;
    private final PromptCatalog prompts;

    public TriageService(TicketService ticketService, ChatPort chatPort, PromptCatalog prompts) {
        this.ticketService = ticketService;
        this.chatPort = chatPort;
        this.prompts = prompts;
    }

    public TriageResponse triage(String reference, boolean applyPriority) {
        TicketResponse ticket = ticketService.get(reference);
        TicketTriage triage = chatPort.completeStructured(
                new ChatCommand(
                        prompts.triageSystemPrompt(),
                        List.of(new ChatTurn(
                                ChatRole.USER,
                                prompts.triageUserPrompt(
                                        ticket.reference(),
                                        ticket.subject(),
                                        ticket.description(),
                                        ticket.priority().name())))),
                TicketTriage.class);

        TicketPriority previous = ticket.priority();
        boolean applied = false;
        if (applyPriority && triage != null && triage.priority() != null && triage.priority() != previous) {
            ticketService.applyTriage(reference, triage.priority());
            applied = true;
        }
        return new TriageResponse(reference, triage, applied, previous);
    }
}
