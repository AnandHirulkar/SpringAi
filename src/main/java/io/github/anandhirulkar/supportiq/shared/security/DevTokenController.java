package io.github.anandhirulkar.supportiq.shared.security;

import java.time.Instant;
import java.util.List;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.http.HttpStatus;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.JwsHeader;
import org.springframework.security.oauth2.jwt.JwtClaimsSet;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.JwtEncoderParameters;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

/**
 * Convenience token issuer so a reviewer can exercise the API from Swagger in seconds.
 *
 * <p>This is intentionally disabled outside local/demo environments via
 * {@code app.security.jwt.dev-token-enabled=false}. In production the API is a pure resource
 * server and tokens come from an identity provider.
 */
@RestController
@RequestMapping("/api/v1/auth")
@Tag(name = "Authentication", description = "Development token issuer")
@ConditionalOnProperty(name = "app.security.jwt.dev-token-enabled", havingValue = "true", matchIfMissing = true)
public class DevTokenController {

    private final JwtEncoder encoder;
    private final JwtProperties properties;

    public DevTokenController(JwtEncoder encoder, JwtProperties properties) {
        this.encoder = encoder;
        this.properties = properties;
    }

    @PostMapping("/token")
    @ResponseStatus(HttpStatus.OK)
    @Operation(summary = "Issue a short-lived development JWT")
    public TokenResponse token(@Valid @RequestBody TokenRequest request) {
        Instant now = Instant.now();
        List<String> roles = request.roles() == null || request.roles().isEmpty()
                ? List.of("AGENT", "ADMIN")
                : request.roles();
        JwtClaimsSet claims = JwtClaimsSet.builder()
                .issuer(properties.issuer())
                .subject(request.username())
                .issuedAt(now)
                .expiresAt(now.plus(properties.ttl()))
                .claim("roles", roles)
                .build();
        String token = encoder
                .encode(JwtEncoderParameters.from(JwsHeader.with(MacAlgorithm.HS256).build(), claims))
                .getTokenValue();
        return new TokenResponse(token, "Bearer", properties.ttl().toSeconds(), properties.issuer());
    }

    public record TokenRequest(@NotBlank String username, List<String> roles) {
    }

    public record TokenResponse(String accessToken, String tokenType, long expiresIn, String issuer) {
    }
}
