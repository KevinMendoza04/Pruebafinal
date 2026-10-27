package com.riwi.dispatch.web.config;

import com.riwi.dispatch.domain.TechnicianDispatcher;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Infrastructure configuration: registers domain objects as Spring beans.
 * TechnicianDispatcher is a pure domain class, so it is wired here in the
 * infrastructure (config) layer rather than annotating the domain class directly.
 */
@Configuration
public class AppConfig {

    @Bean
    public TechnicianDispatcher technicianDispatcher() {
        return new TechnicianDispatcher();
    }
}
