package com.riwi.dispatch.web.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class OpenApiConfig {

    @Bean
    public OpenAPI dispatchOpenAPI() {
        return new OpenAPI()
                .info(new Info()
                        .title("Technician Dispatch Service API")
                        .description("Simulated external dispatch service for HelpFlow. " +
                                "Returns a deterministic technician and estimated attention time.")
                        .version("1.0.0"));
    }
}
