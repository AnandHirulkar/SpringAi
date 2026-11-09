package io.github.anandhirulkar.supportiq.analytics;

import java.time.Instant;
import java.util.List;

import io.github.anandhirulkar.supportiq.analytics.dto.AnalyticsDtos.DailyUsage;
import io.github.anandhirulkar.supportiq.analytics.dto.AnalyticsDtos.UsageReport;
import org.bson.Document;
import org.springframework.data.domain.Sort;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.aggregation.AccumulatorOperators;
import org.springframework.data.mongodb.core.aggregation.Aggregation;
import org.springframework.data.mongodb.core.aggregation.ArrayOperators;
import org.springframework.data.mongodb.core.aggregation.DateOperators;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.stereotype.Service;

/**
 * Read model for AI usage. The aggregation runs inside MongoDB: grouping by day and summing the
 * embedded turn arrays avoids loading thousands of conversation documents into the JVM.
 */
@Service
public class UsageAnalyticsService {

    private final MongoTemplate mongoTemplate;

    public UsageAnalyticsService(MongoTemplate mongoTemplate) {
        this.mongoTemplate = mongoTemplate;
    }

    public UsageReport usage(Instant from, Instant to) {
        Aggregation aggregation = Aggregation.newAggregation(
                Aggregation.match(Criteria.where("updatedAt").gte(from).lte(to)),
                Aggregation.project()
                        .and(DateOperators.dateOf("updatedAt").toString("%Y-%m-%d"))
                        .as("day")
                        .and(ArrayOperators.Size.lengthOfArray("turns"))
                        .as("turns")
                        .and(AccumulatorOperators.Sum.sumOf("turns.promptTokens"))
                        .as("promptTokens")
                        .and(AccumulatorOperators.Sum.sumOf("turns.completionTokens"))
                        .as("completionTokens"),
                Aggregation.group("day")
                        .count()
                        .as("conversations")
                        .sum("turns")
                        .as("turns")
                        .sum("promptTokens")
                        .as("promptTokens")
                        .sum("completionTokens")
                        .as("completionTokens"),
                Aggregation.sort(Sort.Direction.ASC, "_id"));

        List<DailyUsage> daily = mongoTemplate
                .aggregate(aggregation, "ai_conversations", Document.class)
                .getMappedResults()
                .stream()
                .map(row -> new DailyUsage(
                        String.valueOf(row.get("_id")),
                        number(row.get("conversations")),
                        number(row.get("turns")),
                        number(row.get("promptTokens")),
                        number(row.get("completionTokens"))))
                .toList();

        long conversations = daily.stream().mapToLong(DailyUsage::conversations).sum();
        long turns = daily.stream().mapToLong(DailyUsage::turns).sum();
        long tokens = daily.stream().mapToLong(DailyUsage::totalTokens).sum();
        return new UsageReport(from, to, conversations, turns, tokens, daily);
    }

    private static long number(Object value) {
        return value instanceof Number number ? number.longValue() : 0L;
    }
}
