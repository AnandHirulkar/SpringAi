package io.github.anandhirulkar.supportiq.assistant;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.bind.DefaultValue;

@ConfigurationProperties("app.assistant")
public record AssistantProperties(
        @DefaultValue("6") int maxHistoryTurns,
        @DefaultValue("3") int retryAttempts,
        @DefaultValue("500") long retryBackoffMillis,
        @DefaultValue("30") int circuitBreakerWaitSeconds) {

    public AssistantProperties {
        if (maxHistoryTurns < 0) {
            throw new IllegalArgumentException("app.assistant.max-history-turns must be >= 0");
        }
        if (retryAttempts < 1) {
            throw new IllegalArgumentException("app.assistant.retry-attempts must be >= 1");
        }
    }
}
