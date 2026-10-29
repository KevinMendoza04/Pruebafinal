package com.riwi.helpflow.ticket.infrastructure.config;

import com.riwi.helpflow.ticket.application.port.out.TechnicianRepositoryPort;
import com.riwi.helpflow.ticket.application.port.out.UserRepositoryPort;
import com.riwi.helpflow.ticket.application.port.out.PasswordHasherPort;
import com.riwi.helpflow.ticket.domain.model.*;
import com.riwi.helpflow.ticket.domain.model.TechnicianStatus;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.annotation.Profile;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Set;
import java.util.UUID;

/**
 * Seeds 20 demo technicians when the 'demo' profile is active.
 * Technicians have codes TEC-001 to TEC-020, spread over sites and specialties.
 */
@Component
@Profile("demo")
public class DemoSeeder {

    private static final Logger log = LoggerFactory.getLogger(DemoSeeder.class);
    private static final int TECHNICIAN_COUNT = 20;
    private static final List<String> SITES = List.of("BOGOTA", "MEDELLIN", "CALI", "BARRANQUILLA");
    private static final List<Set<TicketCategory>> SPECIALTY_SETS = List.of(
            Set.of(TicketCategory.HARDWARE),
            Set.of(TicketCategory.SOFTWARE),
            Set.of(TicketCategory.RED),
            Set.of(TicketCategory.ACCESOS),
            Set.of(TicketCategory.HARDWARE, TicketCategory.SOFTWARE),
            Set.of(TicketCategory.RED, TicketCategory.ACCESOS),
            Set.of(TicketCategory.HARDWARE, TicketCategory.RED),
            Set.of(TicketCategory.SOFTWARE, TicketCategory.ACCESOS),
            Set.of(TicketCategory.HARDWARE, TicketCategory.SOFTWARE, TicketCategory.RED),
            Set.of(TicketCategory.HARDWARE, TicketCategory.SOFTWARE, TicketCategory.RED, TicketCategory.ACCESOS)
    );

    private final UserRepositoryPort userRepo;
    private final TechnicianRepositoryPort technicianRepo;
    private final PasswordHasherPort hasher;
    private final String demoPassword;

    public DemoSeeder(UserRepositoryPort userRepo, TechnicianRepositoryPort technicianRepo,
                      PasswordHasherPort hasher,
                      @Value("${helpflow.demo.seed-password:demo-pass}") String demoPassword) {
        this.userRepo = userRepo;
        this.technicianRepo = technicianRepo;
        this.hasher = hasher;
        this.demoPassword = demoPassword;
    }

    @EventListener(ApplicationReadyEvent.class)
    public void seedTechnicians() {
        // Check if technicians already exist
        if (technicianRepo.count() > 0) {
            log.info("Technicians already exist, skipping demo seed");
            return;
        }

        String hashedPassword = hasher.hash(demoPassword);

        for (int i = 1; i <= TECHNICIAN_COUNT; i++) {
            String code = String.format("TEC-%03d", i);
            String email = String.format("tec%03d@helpflow.com", i);
            String document = String.format("DOC-%03d", i);
            String fullName = String.format("Technician %03d", i);
            String site = SITES.get((i - 1) % SITES.size());
            Set<TicketCategory> specialties = SPECIALTY_SETS.get((i - 1) % SPECIALTY_SETS.size());

            // Create user with technician role
            User user = User.create(
                    UUID.randomUUID(),
                    document,
                    fullName,
                    email,
                    site,
                    "IT Support",
                    Role.ROLE_TECNICO,
                    UserStatus.ACTIVO,
                    hashedPassword
            );
            userRepo.save(user);

            // Create technician profile
            Technician technician = Technician.create(
                    UUID.randomUUID(),
                    user.getId(),
                    code,
                    specialties,
                    site,
                    "Support profile for " + fullName,
                    TechnicianStatus.ACTIVO,
                    5
            );
            technicianRepo.save(technician);

            log.debug("Created demo technician {} with email {}", code, email);
        }

        log.info("Seeded {} demo technicians", TECHNICIAN_COUNT);
    }
}
