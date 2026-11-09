package io.github.anandhirulkar.supportiq.conversation;

import java.time.Instant;
import java.util.Objects;

import io.github.anandhirulkar.supportiq.conversation.dto.ConversationDtos.ConversationSummary;
import io.github.anandhirulkar.supportiq.shared.api.PageResponse;
import io.github.anandhirulkar.supportiq.shared.exception.ResourceNotFoundException;
import org.springframework.data.domain.Pageable;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.data.mongodb.core.query.Update;
import org.springframework.stereotype.Service;

@Service
public class ConversationService {

    private final AiConversationRepository conversations;
    private final MongoTemplate mongoTemplate;

    public ConversationService(AiConversationRepository conversations, MongoTemplate mongoTemplate) {
        this.conversations = conversations;
        this.mongoTemplate = mongoTemplate;
    }

    public AiConversation loadOrCreate(Long customerId, String conversationId) {
        if (conversationId == null || conversationId.isBlank()) {
            return conversations.save(AiConversation.start(customerId, "Support conversation"));
        }
        AiConversation conversation = require(conversationId);
        if (!Objects.equals(conversation.getCustomerId(), customerId)) {
            // Never leak another customer's transcript: treat it as not found.
            throw new ResourceNotFoundException("Conversation", conversationId);
        }
        return conversation;
    }

    /**
     * Atomic append. Read-modify-write on the whole document would lose turns when two requests
     * share a conversation, so the turn is pushed in a single MongoDB update instead.
     */
    public void appendTurn(String conversationId, ConversationTurn turn) {
        var result = mongoTemplate.updateFirst(
                Query.query(Criteria.where("_id").is(conversationId)),
                new Update().push("turns", turn).set("updatedAt", Instant.now()),
                AiConversation.class);
        if (result.getMatchedCount() == 0) {
            throw new ResourceNotFoundException("Conversation", conversationId);
        }
    }

    public AiConversation require(String conversationId) {
        return conversations
                .findById(conversationId)
                .orElseThrow(() -> new ResourceNotFoundException("Conversation", conversationId));
    }

    public PageResponse<ConversationSummary> list(Long customerId, Pageable pageable) {
        return PageResponse.from(
                conversations.findByCustomerIdOrderByUpdatedAtDesc(customerId, pageable),
                ConversationSummary::from);
    }
}
