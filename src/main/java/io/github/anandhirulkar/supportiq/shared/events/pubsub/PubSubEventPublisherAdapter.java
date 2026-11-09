package io.github.anandhirulkar.supportiq.shared.events.pubsub;

import com.google.cloud.pubsub.v1.Publisher;
import com.google.pubsub.v1.TopicName;
import com.google.protobuf.ByteString;
import com.google.pubsub.v1.PubsubMessage;
import io.github.anandhirulkar.supportiq.shared.events.DomainEvent;
import io.github.anandhirulkar.supportiq.shared.events.EventPublisher;
import jakarta.annotation.PreDestroy;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import tools.jackson.databind.json.JsonMapper;

/**
 * Cloud adapter for the same messaging port. Activated with the {@code gcp} profile.
 *
 * <p>Google Cloud Pub/Sub has a permanent free tier (10 GiB of messages per month), which makes it
 * a realistic broker for a portfolio deployment. Credentials are supplied by the runtime
 * environment (Workload Identity, or GOOGLE_APPLICATION_CREDENTIALS locally) - never in code.
 */
@Component
@Profile("gcp")
public class PubSubEventPublisherAdapter implements EventPublisher {

    private static final Logger log = LoggerFactory.getLogger(PubSubEventPublisherAdapter.class);

    private final Publisher publisher;
    private final JsonMapper jsonMapper;

    public PubSubEventPublisherAdapter(PubSubProperties properties, JsonMapper jsonMapper) throws Exception {
        if (!StringUtils.hasText(properties.projectId())) {
            throw new IllegalStateException("app.messaging.pubsub.project-id is required when the gcp profile is active");
        }
        this.jsonMapper = jsonMapper;
        this.publisher = Publisher.newBuilder(TopicName.of(properties.projectId(), properties.topic())).build();
        log.info("Pub/Sub publisher bound to topic {}", properties.topic());
    }

    @Override
    public void publish(DomainEvent event) {
        PubsubMessage message = PubsubMessage.newBuilder()
                .setData(ByteString.copyFrom(jsonMapper.writeValueAsBytes(event)))
                .putAttributes("type", event.type())
                .putAttributes("eventId", event.eventId().toString())
                .build();
        publisher.publish(message);
        log.debug("Published {} to Pub/Sub", event.type());
    }

    @PreDestroy
    void shutdown() {
        publisher.shutdown();
    }
}
