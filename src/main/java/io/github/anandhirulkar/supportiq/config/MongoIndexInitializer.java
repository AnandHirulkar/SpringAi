package io.github.anandhirulkar.supportiq.config;

import io.github.anandhirulkar.supportiq.conversation.AiConversation;
import io.github.anandhirulkar.supportiq.shared.audit.AuditEntry;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.data.domain.Sort;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.index.Index;
import org.springframework.stereotype.Component;

/**
 * Explicit index creation beats implicit auto-creation: production deployments can run with
 * {@code auto-index-creation=false} and still get the indexes this application depends on.
 */
@Component
public class MongoIndexInitializer implements ApplicationRunner {

    private static final Logger log = LoggerFactory.getLogger(MongoIndexInitializer.class);

    private final MongoTemplate mongoTemplate;

    public MongoIndexInitializer(MongoTemplate mongoTemplate) {
        this.mongoTemplate = mongoTemplate;
    }

    @Override
    public void run(ApplicationArguments args) {
        mongoTemplate
                .indexOps(AiConversation.class)
                .ensureIndex(new Index()
                        .on("customerId", Sort.Direction.ASC)
                        .on("updatedAt", Sort.Direction.DESC)
                        .named("idx_conversation_customer_updated"));
        mongoTemplate
                .indexOps(AuditEntry.class)
                .ensureIndex(new Index()
                        .on("occurredAt", Sort.Direction.DESC)
                        .named("idx_audit_occurred"));
        log.info("MongoDB indexes ensured");
    }
}
