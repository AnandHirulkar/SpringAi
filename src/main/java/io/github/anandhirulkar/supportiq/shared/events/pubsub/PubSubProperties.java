package io.github.anandhirulkar.supportiq.shared.events.pubsub;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties("app.messaging.pubsub")
public record PubSubProperties(String projectId, String topic, String pushToken) {
}
