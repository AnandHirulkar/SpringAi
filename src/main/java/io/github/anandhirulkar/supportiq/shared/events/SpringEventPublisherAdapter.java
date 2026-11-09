package io.github.anandhirulkar.supportiq.shared.events;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

/**
 * Default adapter: publishes through Spring's application event bus. This keeps the local demo
 * broker-free while the same port is implemented by a Pub/Sub adapter under the {@code gcp} profile.
 */
@Component
@Profile("!gcp")
public class SpringEventPublisherAdapter implements EventPublisher {

    private static final Logger log = LoggerFactory.getLogger(SpringEventPublisherAdapter.class);

    private final ApplicationEventPublisher publisher;

    public SpringEventPublisherAdapter(ApplicationEventPublisher publisher) {
        this.publisher = publisher;
    }

    @Override
    public void publish(DomainEvent event) {
        log.debug("Publishing {} {}", event.type(), event.eventId());
        publisher.publishEvent(event);
    }
}
