package io.github.anandhirulkar.supportiq.analytics.dto;

import java.time.Instant;
import java.util.List;

public final class AnalyticsDtos {

    private AnalyticsDtos() {
    }

    public record DailyUsage(
            String day, long conversations, long turns, long promptTokens, long completionTokens) {

        public long totalTokens() {
            return promptTokens + completionTokens;
        }
    }

    public record UsageReport(
            Instant from,
            Instant to,
            long totalConversations,
            long totalTurns,
            long totalTokens,
            List<DailyUsage> daily) {
    }
}
