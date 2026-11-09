package io.github.anandhirulkar.supportiq.shared.security;

import java.time.Duration;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.bind.DefaultValue;

@ConfigurationProperties("app.security.jwt")
public record JwtProperties(
        String secret,
        @DefaultValue("supportiq") String issuer,
        @DefaultValue("PT8H") Duration ttl,
        @DefaultValue("true") boolean devTokenEnabled) {

    public JwtProperties {
        if (secret == null || secret.getBytes(java.nio.charset.StandardCharsets.UTF_8).length < 32) {
            throw new IllegalArgumentException(
                    "app.security.jwt.secret must be at least 32 bytes; set the JWT_SECRET environment variable");
        }
        if (ttl == null || ttl.isNegative() || ttl.isZero()) {
            throw new IllegalArgumentException("app.security.jwt.ttl must be positive");
        }
    }
}
