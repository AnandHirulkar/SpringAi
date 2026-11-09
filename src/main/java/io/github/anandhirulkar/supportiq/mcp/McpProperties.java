package io.github.anandhirulkar.supportiq.mcp;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties("app.mcp")
public record McpProperties(String apiKey) {
}
