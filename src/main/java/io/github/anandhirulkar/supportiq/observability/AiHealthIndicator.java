package io.github.anandhirulkar.supportiq.observability;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.health.contributor.Health;
import org.springframework.boot.health.contributor.HealthIndicator;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

/**
 * Exposes AI-provider readiness and the circuit breaker state on {@code /actuator/health}.
 * The application is still healthy when the key is absent - only AI endpoints degrade - so the
 * detail is reported instead of failing the whole health check.
 */
@Component
public class AiHealthIndicator implements HealthIndicator {

    private final CircuitStateProvider chatPort;
    private final String apiKey;
    private final String model;

    public AiHealthIndicator(
            CircuitStateProvider chatPort,
            @Value("${spring.ai.openai.api-key:}") String apiKey,
            @Value("${spring.ai.openai.chat.options.model:not-configured}") String model) {
        this.chatPort = chatPort;
        this.apiKey = apiKey;
        this.model = model;
    }

    @Override
    public Health health() {
        boolean configured = StringUtils.hasText(apiKey) && !apiKey.startsWith("demo-key");
        return Health.up()
                .withDetail("provider", "openai-compatible")
                .withDetail("model", model)
                .withDetail("apiKeyConfigured", configured)
                .withDetail("circuitBreaker", chatPort.circuitState())
                .build();
    }
}
