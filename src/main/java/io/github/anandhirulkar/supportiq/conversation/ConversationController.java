package io.github.anandhirulkar.supportiq.conversation;

import io.github.anandhirulkar.supportiq.conversation.dto.ConversationDtos.ConversationResponse;
import io.github.anandhirulkar.supportiq.conversation.dto.ConversationDtos.ConversationSummary;
import io.github.anandhirulkar.supportiq.shared.api.PageResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springdoc.core.annotations.ParameterObject;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/conversations")
@Tag(name = "Conversations", description = "AI conversation transcripts stored in MongoDB")
public class ConversationController {

    private final ConversationService conversationService;

    public ConversationController(ConversationService conversationService) {
        this.conversationService = conversationService;
    }

    @GetMapping("/{id}")
    @Operation(summary = "Fetch a full conversation transcript")
    public ConversationResponse get(@PathVariable String id) {
        return ConversationResponse.from(conversationService.require(id));
    }

    @GetMapping
    @Operation(summary = "List a customer's conversations")
    public PageResponse<ConversationSummary> list(
            @RequestParam Long customerId,
            @ParameterObject @PageableDefault(size = 20) Pageable pageable) {
        return conversationService.list(customerId, pageable);
    }
}
