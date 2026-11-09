package io.github.anandhirulkar.supportiq.assistant;

import io.github.anandhirulkar.supportiq.assistant.dto.AssistantDtos.ChatRequest;
import io.github.anandhirulkar.supportiq.assistant.dto.AssistantDtos.ChatResponse;
import io.github.anandhirulkar.supportiq.assistant.dto.AssistantDtos.TriageResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Flux;

@RestController
@RequestMapping("/api/v1/assistant")
@Tag(name = "Assistant", description = "RAG chat, streaming answers and AI ticket triage")
public class AssistantController {

    private final AssistantService assistantService;
    private final TriageService triageService;

    public AssistantController(AssistantService assistantService, TriageService triageService) {
        this.assistantService = assistantService;
        this.triageService = triageService;
    }

    @PostMapping("/chat")
    @Operation(summary = "Ask the support copilot (RAG + tool calling)")
    public ChatResponse chat(@Valid @RequestBody ChatRequest request) {
        return assistantService.chat(request);
    }

    @PostMapping(value = "/chat/stream", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    @Operation(summary = "Stream an answer token by token (SSE)")
    public Flux<String> stream(@Valid @RequestBody ChatRequest request, HttpServletResponse response) {
        return assistantService.stream(request, response);
    }

    @PostMapping("/triage/{reference}")
    @Operation(summary = "Classify a ticket with structured output; optionally apply the suggested priority")
    public TriageResponse triage(
            @PathVariable String reference,
            @RequestParam(defaultValue = "false") boolean applyPriority) {
        return triageService.triage(reference, applyPriority);
    }
}
