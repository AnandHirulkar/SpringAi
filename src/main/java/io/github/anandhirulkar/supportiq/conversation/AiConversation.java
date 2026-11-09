package io.github.anandhirulkar.supportiq.conversation;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.CompoundIndex;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

/**
 * Conversation transcript stored in MongoDB: an unbounded, semi-structured list of turns with
 * citations and token accounting. MongoDB is a better fit than MySQL here because the shape is
 * append-heavy and read as one aggregate.
 */
@Document(collection = "ai_conversations")
@CompoundIndex(name = "idx_conversation_customer_updated", def = "{'customerId': 1, 'updatedAt': -1}")
public class AiConversation {

    @Id
    private String id;

    @Indexed
    private Long customerId;

    private String title;

    private Instant createdAt;

    private Instant updatedAt;

    private List<ConversationTurn> turns = new ArrayList<>();

    protected AiConversation() {
        // for Spring Data
    }

    private AiConversation(Long customerId, String title) {
        this.customerId = customerId;
        this.title = title;
        this.createdAt = Instant.now();
        this.updatedAt = this.createdAt;
    }

    public static AiConversation start(Long customerId, String title) {
        return new AiConversation(customerId, title);
    }

    public void addTurn(ConversationTurn turn) {
        this.turns.add(turn);
        this.updatedAt = Instant.now();
    }

    public List<ConversationTurn> lastTurns(int maxTurns) {
        int wanted = Math.max(0, maxTurns * 2);
        int from = Math.max(0, turns.size() - wanted);
        return List.copyOf(turns.subList(from, turns.size()));
    }

    public String getId() {
        return id;
    }

    public Long getCustomerId() {
        return customerId;
    }

    public String getTitle() {
        return title;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public Instant getUpdatedAt() {
        return updatedAt;
    }

    public List<ConversationTurn> getTurns() {
        return List.copyOf(turns);
    }
}
