package com.riwi.helpflow.ticket.integration;

import com.riwi.helpflow.ticket.application.port.out.TechnicianDispatchPort;
import com.riwi.helpflow.ticket.infrastructure.adapter.out.persistence.repository.*;
import org.junit.jupiter.api.BeforeEach;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

/**
 * Base class for integration tests using Testcontainers PostgreSQL.
 * The dispatch port is mocked to avoid external HTTP calls.
 */
@SpringBootTest
@AutoConfigureMockMvc
@Testcontainers
public abstract class IntegrationTestBase {

    @Container
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:16-alpine")
            .withDatabaseName("helpflow_test")
            .withUsername("test")
            .withPassword("test");

    @DynamicPropertySource
    static void configureProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", postgres::getJdbcUrl);
        registry.add("spring.datasource.username", postgres::getUsername);
        registry.add("spring.datasource.password", postgres::getPassword);
    }

    @Autowired protected MockMvc mockMvc;
    @Autowired protected UserJpaRepository       userRepo;
    @Autowired protected TechnicianJpaRepository  techRepo;
    @Autowired protected TicketJpaRepository      ticketRepo;
    @Autowired protected TicketHistoryJpaRepository historyRepo;
    @Autowired protected AssignmentJpaRepository   assignmentRepo;
    @Autowired protected SlaPolicyJpaRepository   slaRepo;

    @MockitoBean protected TechnicianDispatchPort dispatchPort;

    @BeforeEach
    void cleanDatabase() {
        assignmentRepo.deleteAll();
        historyRepo.deleteAll();
        ticketRepo.deleteAll();
        techRepo.deleteAll();
        userRepo.deleteAll();
        // Re-seed SLA policies (Flyway already ran, but clean-up may have removed them)
        if (slaRepo.count() == 0) {
            // Flyway V3 already inserts SLA policies; they survive if not deleted
        }
    }
}
