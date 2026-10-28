package com.riwi.helpflow.ticket.infrastructure.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.security.SecurityScheme;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class OpenApiConfig {

    @Bean
    public OpenAPI helpflowOpenAPI() {
        return new OpenAPI()
                .info(new Info()
                        .title("HelpFlow Support Ticket Service API")
                        .description("Main service for IT support tickets: users, technicians, " +
                                "tickets, SLA, assignment, escalation, history.")
                        .version("1.0.0"))
                .components(new io.swagger.v3.oas.models.Components()
                        .addSecuritySchemes("bearer-jwt",
                                new SecurityScheme()
                                        .type(SecurityScheme.Type.HTTP)
                                        .scheme("bearer")
                                        .bearerFormat("JWT")
                                        .description("JWT bearer token")));
    }
}
