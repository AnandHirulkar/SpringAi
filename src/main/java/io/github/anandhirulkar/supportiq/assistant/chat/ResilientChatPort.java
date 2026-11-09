package io.github.anandhirulkar.supportiq.assistant.chat;

import java.io.IOException;
import java.time.Duration;
import java.util.function.Supplier;

import io.github.anandhirulkar.supportiq.assistant.AssistantProperties;
import io.github.anandhirulkar.supportiq.observability.CircuitStateProvider;
import io.github.anandhirulkar.supportiq.shared.exception.AiServiceUnavailableException;
import io.github.resilience4j.circuitbreaker.CallNotPermittedException;
import io.github.resilience4j.circuitbreaker.CircuitBreaker;
import io.github.resilience4j.circuitbreaker.CircuitBreakerConfig;
import io.github.resilience4j.circuitbreaker.CircuitBreakerRegistry;
import io.github.resilience4j.retry.Retry;
import io.github.resilience4j.retry.RetryConfig;
import io.github.resilience4j.retry.RetryRegistry;
import io.micrometer.core.instrument.MeterRegistry;
import org.springframework.web.client.HttpServerErrorException;
import org.springframework.web.client.ResourceAccessException;
import reactor.core.publisher.Flux;

/**
 * Decorator that adds a circuit breaker and bounded retries to every blocking model call.
 * When the provider is failing, callers fail fast with {@link AiServiceUnavailableException}
 * instead of pinning request threads for the length of an HTTP timeout.
 */
public class ResilientChatPort implements ChatPort, CircuitStateProvider {

    private static final String DEPENDENCY = "llm";

    private final ChatPort delegate;
    private final CircuitBreakerRegistry circuitBreakers;
    private final Retry retry;

    public ResilientChatPort(
            ChatPort delegate,
            AssistantProperties properties,
            MeterRegistry meterRegistry,
            String modelName) {
        this.delegate = delegate;
        CircuitBreakerConfig circuitBreakerConfig = CircuitBreakerConfig.custom()
                .failureRateThreshold(50)
                .slidingWindowSize(10)
                .minimumNumberOfCalls(5)
                .waitDurationInOpenState(Duration.ofSeconds(Math.max(5, properties.circuitBreakerWaitSeconds())))
                .permittedNumberOfCallsInHalfOpenState(2)
                .build();
        this.circuitBreakers = CircuitBreakerRegistry.of(circuitBreakerConfig);
        this.retry = RetryRegistry.of(RetryConfig.custom()
                        .maxAttempts(Math.max(1, properties.retryAttempts()))
                        .waitDuration(Duration.ofMillis(Math.max(50, properties.retryBackoffMillis())))
                        .retryExceptions(
                                ResourceAccessException.class,
                                HttpServerErrorException.class,
                                IOException.class)
                        .build())
                .retry("llm-retry");
        meterRegistry.gauge("supportiq.ai.circuit.breaker.open", circuitBreakers.circuitBreaker(DEPENDENCY),
                breaker -> breaker.getState() == CircuitBreaker.State.OPEN ? 1 : 0);
        meterRegistry.counter("supportiq.ai.model.info",
                "model", modelName == null ? "unconfigured" : modelName);
    }

    @Override
    public ChatAnswer complete(ChatCommand command) {
        Supplier<ChatAnswer> decorated = Retry.decorateSupplier(
                retry, CircuitBreaker.decorateSupplier(circuitBreaker(), () -> delegate.complete(command)));
        try {
            return decorated.get();
        } catch (CallNotPermittedException notPermitted) {
            throw new AiServiceUnavailableException(
                    "The AI provider is temporarily unavailable (circuit breaker open). Retry shortly.", notPermitted);
        }
    }

    @Override
    public <T> T completeStructured(ChatCommand command, Class<T> targetType) {
        Supplier<T> decorated = Retry.decorateSupplier(
                retry,
                CircuitBreaker.decorateSupplier(circuitBreaker(), () -> delegate.completeStructured(command, targetType)));
        try {
            return decorated.get();
        } catch (CallNotPermittedException notPermitted) {
            throw new AiServiceUnavailableException(
                    "The AI provider is temporarily unavailable (circuit breaker open). Retry shortly.", notPermitted);
        }
    }

    @Override
    public Flux<String> stream(ChatCommand command) {
        // Streaming is asynchronous: the breaker wraps subscription-time failures only.
        return Flux.defer(() -> delegate.stream(command)).onErrorMap(
                error -> new AiServiceUnavailableException("The AI provider stream failed", error));
    }

    public CircuitBreaker.State circuitBreakerState() {
        return circuitBreaker().getState();
    }

    @Override
    public String circuitState() {
        return circuitBreakerState().name();
    }

    private CircuitBreaker circuitBreaker() {
        return circuitBreakers.circuitBreaker(DEPENDENCY);
    }
}
