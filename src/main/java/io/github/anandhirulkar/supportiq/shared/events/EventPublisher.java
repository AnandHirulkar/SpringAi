package io.github.anandhirulkar.supportiq.shared.events;

/**
 * Outbound messaging port. The core publishes facts without knowing whether they are delivered
 * in-process or through a broker such as Google Cloud Pub/Sub.
 */
public interface EventPublisher {

    void publish(DomainEvent event);
}
