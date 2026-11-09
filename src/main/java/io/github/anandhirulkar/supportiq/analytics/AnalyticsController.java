package io.github.anandhirulkar.supportiq.analytics;

import java.time.Duration;
import java.time.Instant;

import io.github.anandhirulkar.supportiq.analytics.dto.AnalyticsDtos.UsageReport;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/analytics")
@Validated
@Tag(name = "Analytics", description = "AI usage analytics computed in MongoDB")
public class AnalyticsController {

    private final UsageAnalyticsService analytics;

    public AnalyticsController(UsageAnalyticsService analytics) {
        this.analytics = analytics;
    }

    @GetMapping("/usage")
    @Operation(summary = "Token and conversation usage per day")
    public UsageReport usage(
            @RequestParam(defaultValue = "7") @Min(1) @Max(90) int days,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Instant from) {
        Instant to = Instant.now();
        Instant start = from == null ? to.minus(Duration.ofDays(days)) : from;
        return analytics.usage(start, to);
    }
}
