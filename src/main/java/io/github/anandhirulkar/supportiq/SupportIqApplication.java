package io.github.anandhirulkar.supportiq;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.scheduling.annotation.EnableAsync;

/**
 * SupportIQ - an AI customer-support copilot.
 *
 * <p>The application is organised as a modular monolith: each top-level package is a bounded
 * context (customer, ticket, knowledge, conversation, assistant, analytics) and talks to the
 * others through narrow ports. Infrastructure detail lives in {@code config} and feature
 * {@code adapter} packages, so the core business rules stay free of framework and vendor types.
 */
@SpringBootApplication
@ConfigurationPropertiesScan
@EnableCaching
@EnableAsync
public class SupportIqApplication {

    public static void main(String[] args) {
        SpringApplication.run(SupportIqApplication.class, args);
    }
}
