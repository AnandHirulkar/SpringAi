package io.github.anandhirulkar.supportiq.config;

import io.github.anandhirulkar.supportiq.assistant.AssistantProperties;
import io.github.anandhirulkar.supportiq.assistant.chat.ChatPort;
import io.github.anandhirulkar.supportiq.assistant.chat.ResilientChatPort;
import io.github.anandhirulkar.supportiq.assistant.chat.SpringAiChatPort;
import io.github.anandhirulkar.supportiq.assistant.tools.KnowledgeTools;
import io.github.anandhirulkar.supportiq.assistant.tools.TicketTools;
import io.micrometer.core.instrument.MeterRegistry;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration(proxyBeanMethods = false)
public class AiConfig {

    /**
     * Decorator wiring is explicit: the orchestrator sees only {@link ChatPort}; resilience and
     * provider details stay at the infrastructure edge.
     */
    @Bean
    public ResilientChatPort chatPort(
            ChatClient.Builder chatClientBuilder,
            TicketTools ticketTools,
            KnowledgeTools knowledgeTools,
            AssistantProperties assistantProperties,
            MeterRegistry meterRegistry,
            @Value("${spring.ai.openai.chat.options.model:not-configured}") String model) {
        ChatPort adapter = new SpringAiChatPort(chatClientBuilder, ticketTools, knowledgeTools);
        return new ResilientChatPort(adapter, assistantProperties, meterRegistry, model);
    }
}
