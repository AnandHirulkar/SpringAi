package io.github.anandhirulkar.supportiq.observability;

import java.time.Duration;

import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import org.springframework.stereotype.Component;

/** Business metrics that complement the framework's HTTP and JVM instrumentation. */
@Component
public class AiMetrics {

    private final Counter chatRequests;
    private final Counter streamRequests;
    private final Counter tokens;
    private final Timer chatLatency;
    private final Timer streamLatency;

    public AiMetrics(MeterRegistry registry) {
        this.chatRequests = Counter.builder("supportiq.ai.chat.requests")
                .description("Completed blocking chat requests")
                .register(registry);
        this.streamRequests = Counter.builder("supportiq.ai.chat.streams")
                .description("Completed streaming chat requests")
                .register(registry);
        this.tokens = Counter.builder("supportiq.ai.tokens")
                .description("Total model tokens consumed (prompt + completion)")
                .register(registry);
        this.chatLatency = Timer.builder("supportiq.ai.chat.latency")
                .description("End-to-end model latency")
                .publishPercentileHistogram()
                .register(registry);
        this.streamLatency = Timer.builder("supportiq.ai.chat.stream.latency")
                .description("Time to finish a streamed answer")
                .publishPercentileHistogram()
                .register(registry);
    }

    public void recordChat(int totalTokens, long latencyMs) {
        chatRequests.increment();
        tokens.increment(Math.max(0, totalTokens));
        chatLatency.record(Duration.ofMillis(Math.max(0, latencyMs)));
    }

    public void recordStream(long latencyMs) {
        streamRequests.increment();
        streamLatency.record(Duration.ofMillis(Math.max(0, latencyMs)));
    }
}
