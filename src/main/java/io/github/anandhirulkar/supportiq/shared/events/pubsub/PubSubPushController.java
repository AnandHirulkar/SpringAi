package io.github.anandhirulkar.supportiq.shared.events.pubsub;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.Base64;
import java.util.Map;

import io.github.anandhirulkar.supportiq.shared.audit.AuditIngestService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Profile;
import org.springframework.http.HttpStatus;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

/**
 * Inbound edge for a Pub/Sub push subscription: the broker posts base64-encoded messages here.
 * Enabled only with the {@code gcp} profile and protected by a shared secret that Pub/Sub adds
 * as a push endpoint header.
 */
@RestController
@RequestMapping("/api/v1/events/pubsub")
@Profile("gcp")
@Tag(name = "Messaging", description = "Inbound Pub/Sub push subscription")
public class PubSubPushController {

    private static final Logger log = LoggerFactory.getLogger(PubSubPushController.class);

    private final PubSubProperties properties;
    private final AuditIngestService auditIngestService;

    public PubSubPushController(PubSubProperties properties, AuditIngestService auditIngestService) {
        this.properties = properties;
        this.auditIngestService = auditIngestService;
    }

    @PostMapping("/push")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @Operation(summary = "Receive a Pub/Sub push message")
    public void receive(
            @RequestHeader(value = "X-PubSub-Token", required = false) String token,
            @RequestBody PushEnvelope envelope) {
        if (!validToken(token)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Invalid push token");
        }
        if (envelope == null || envelope.message() == null || envelope.message().data() == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Malformed Pub/Sub envelope");
        }
        String decoded = new String(
                Base64.getDecoder().decode(envelope.message().data()), StandardCharsets.UTF_8);
        String type = envelope.message().attributes() == null
                ? "unknown"
                : envelope.message().attributes().getOrDefault("type", "unknown");
        auditIngestService.record(
                envelope.message().messageId(), type, Map.of("payload", decoded), "pubsub");
        log.debug("Processed Pub/Sub message {} of type {}", envelope.message().messageId(), type);
    }

    private boolean validToken(String supplied) {
        if (!StringUtils.hasText(properties.pushToken())) {
            // Fail closed in the cloud profile: an unset secret is a deployment error.
            log.error("app.messaging.pubsub.push-token is not configured; rejecting Pub/Sub push");
            return false;
        }
        if (supplied == null) {
            return false;
        }
        return MessageDigest.isEqual(
                properties.pushToken().getBytes(StandardCharsets.UTF_8),
                supplied.getBytes(StandardCharsets.UTF_8));
    }

    public record PushEnvelope(PushMessage message, String subscription) {
    }

    public record PushMessage(
            String data, Map<String, String> attributes, String messageId, String publishTime) {
    }
}
