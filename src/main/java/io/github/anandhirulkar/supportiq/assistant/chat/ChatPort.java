package io.github.anandhirulkar.supportiq.assistant.chat;

import reactor.core.publisher.Flux;

/**
 * Port for model access. Keeping provider types (Spring AI, OpenAI, HTTP client) out of the
 * application service makes the orchestrator testable and lets resilience be added as a decorator.
 */
public interface ChatPort {

    ChatAnswer complete(ChatCommand command);

    <T> T completeStructured(ChatCommand command, Class<T> targetType);

    Flux<String> stream(ChatCommand command);
}
