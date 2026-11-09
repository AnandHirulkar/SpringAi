package io.github.anandhirulkar.supportiq.config;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;
import io.swagger.v3.oas.models.servers.Server;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration(proxyBeanMethods = false)
public class OpenApiConfig {

    private static final String SECURITY_SCHEME = "bearerAuth";

    @Bean
    public OpenAPI supportIqOpenApi() {
        return new OpenAPI()
                .info(new Info()
                        .title("SupportIQ API")
                        .version("v1")
                        .description("""
                                AI customer-support copilot: RAG over pgvector, MCP tools, MySQL system of record
                                and MongoDB transcripts.

                                Get a development JWT from `POST /api/v1/auth/token`, then click **Authorize**
                                and paste the token.
                                """)
                        .contact(new Contact().name("SupportIQ")))
                .addServersItem(new Server().url("/").description("Current host"))
                .components(new Components()
                        .addSecuritySchemes(
                                SECURITY_SCHEME,
                                new SecurityScheme()
                                        .type(SecurityScheme.Type.HTTP)
                                        .scheme("bearer")
                                        .bearerFormat("JWT")
                                        .description("HS256 JWT issued by /api/v1/auth/token")))
                .addSecurityItem(new SecurityRequirement().addList(SECURITY_SCHEME));
    }
}
