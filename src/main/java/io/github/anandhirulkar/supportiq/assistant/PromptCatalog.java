package io.github.anandhirulkar.supportiq.assistant;

import java.io.IOException;
import java.nio.charset.StandardCharsets;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Component;

/** Loads versioned prompt resources once at startup instead of embedding them in Java strings. */
@Component
public class PromptCatalog {

    private final String supportSystem;
    private final String triageSystem;

    public PromptCatalog(
            @Value("classpath:prompts/support-system.st") Resource supportPrompt,
            @Value("classpath:prompts/triage-system.st") Resource triagePrompt) {
        this.supportSystem = read(supportPrompt);
        this.triageSystem = read(triagePrompt);
    }

    public String supportSystemPrompt() {
        return supportSystem;
    }

    public String triageSystemPrompt() {
        return triageSystem;
    }

    public String triageUserPrompt(String reference, String subject, String description, String priority) {
        return """
                Ticket reference: %s
                Current priority: %s
                Subject: %s

                Description:
                %s
                """.formatted(reference, priority, subject, description);
    }

    private static String read(Resource resource) {
        try {
            return resource.getContentAsString(StandardCharsets.UTF_8);
        } catch (IOException e) {
            throw new IllegalStateException("Missing prompt resource: " + resource, e);
        }
    }
}
