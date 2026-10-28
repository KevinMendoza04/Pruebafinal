package com.riwi.helpflow.ticket.infrastructure.config;

import com.riwi.helpflow.ticket.application.port.out.*;
import com.riwi.helpflow.ticket.application.usecase.*;
import com.riwi.helpflow.ticket.domain.service.*;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.time.Clock;
import java.time.ZoneOffset;

/**
 * Wires all use-case beans and domain services.
 * Constructor injection only; no field @Autowired.
 */
@Configuration
public class BeanConfig {

    // ── Clock (injectable for deterministic time in tests) ──────────────────
    @Bean
    public Clock clock() {
        return Clock.system(ZoneOffset.UTC);
    }

    // ── Domain services (no Spring deps) ─────────────────────────────────────
    @Bean public TicketTransitionPolicy  ticketTransitionPolicy()  { return new TicketTransitionPolicy(); }
    @Bean public SlaCalculator           slaCalculator()           { return new SlaCalculator(); }
    @Bean public TechnicianSelectionPolicy technicianSelectionPolicy() { return new TechnicianSelectionPolicy(); }
    @Bean public EscalationPolicy         escalationPolicy()         { return new EscalationPolicy(); }

    // ── Use cases ────────────────────────────────────────────────────────────
    @Bean
    public RegisterUserUseCaseImpl registerUserUseCase(UserRepositoryPort userRepo,
                                                        PasswordHasherPort hasher,
                                                        @Value("${helpflow.corporate-email-domain:helpflow.com}") String emailDomain) {
        return new RegisterUserUseCaseImpl(userRepo, hasher, emailDomain);
    }

    @Bean
    public AuthenticateUseCaseImpl authenticateUseCase(UserRepositoryPort userRepo,
                                                        PasswordHasherPort hasher,
                                                        TokenIssuerPort tokenIssuer,
                                                        TicketMetricsPort metrics,
                                                        @Value("${helpflow.jwt.expiration-minutes:60}") long expMinutes) {
        return new AuthenticateUseCaseImpl(userRepo, hasher, tokenIssuer, metrics, expMinutes * 60);
    }

    @Bean
    public CreateTicketUseCaseImpl createTicketUseCase(CreateTicketTxPort createTx,
                                                        AssignTicketTxPort assignTx,
                                                        TechnicianDispatchPort dispatch,
                                                        TicketMetricsPort metrics) {
        return new CreateTicketUseCaseImpl(createTx, assignTx, dispatch, metrics);
    }

    @Bean
    public GetTicketsUseCaseImpl getTicketsUseCase(TicketRepositoryPort ticketRepo,
                                                    TechnicianRepositoryPort technicianRepo,
                                                    TicketHistoryRepositoryPort historyRepo) {
        return new GetTicketsUseCaseImpl(ticketRepo, technicianRepo, historyRepo);
    }

    @Bean
    public UpdateTicketUseCaseImpl updateTicketUseCase(TicketRepositoryPort ticketRepo, Clock clock) {
        return new UpdateTicketUseCaseImpl(ticketRepo, clock);
    }

    @Bean
    public TransitionTicketUseCaseImpl transitionTicketUseCase(TicketRepositoryPort ticketRepo,
                                                               TechnicianRepositoryPort technicianRepo,
                                                               TicketHistoryRepositoryPort historyRepo,
                                                               TicketTransitionPolicy policy,
                                                               TicketMetricsPort metrics,
                                                               Clock clock) {
        return new TransitionTicketUseCaseImpl(ticketRepo, technicianRepo, historyRepo,
                policy, metrics, clock);
    }

    @Bean
    public CancelTicketUseCaseImpl cancelTicketUseCase(TicketRepositoryPort ticketRepo,
                                                       TicketHistoryRepositoryPort historyRepo,
                                                       TicketTransitionPolicy policy,
                                                       Clock clock) {
        return new CancelTicketUseCaseImpl(ticketRepo, historyRepo, policy, clock);
    }

    @Bean
    public ReassignTicketUseCaseImpl reassignTicketUseCase(TicketRepositoryPort ticketRepo,
                                                           TechnicianRepositoryPort technicianRepo,
                                                           AssignmentRepositoryPort assignmentRepo,
                                                           TicketHistoryRepositoryPort historyRepo,
                                                           TechnicianSelectionPolicy selectionPolicy,
                                                           Clock clock) {
        return new ReassignTicketUseCaseImpl(ticketRepo, technicianRepo, assignmentRepo,
                historyRepo, selectionPolicy, clock);
    }

    @Bean
    public EscalateTicketsUseCaseImpl escalateTicketsUseCase(TicketRepositoryPort ticketRepo,
                                                             TicketHistoryRepositoryPort historyRepo,
                                                             EscalationPolicy policy,
                                                             AssignTicketTxPort assignTx,
                                                             TicketMetricsPort metrics,
                                                             Clock clock,
                                                             @Value("${helpflow.escalation.threshold-percent:80}") int threshold) {
        return new EscalateTicketsUseCaseImpl(ticketRepo, historyRepo, policy,
                assignTx, metrics, clock, threshold);
    }

    @Bean
    public ManageSlaPoliciesUseCaseImpl manageSlaPoliciesUseCase(SlaPolicyRepositoryPort policyRepo,
                                                                 SlaCalculator slaCalculator) {
        return new ManageSlaPoliciesUseCaseImpl(policyRepo, slaCalculator);
    }

    @Bean
    public ManageUsersUseCaseImpl manageUsersUseCase(UserRepositoryPort userRepo) {
        return new ManageUsersUseCaseImpl(userRepo);
    }

    @Bean
    public ManageTechniciansUseCaseImpl manageTechniciansUseCase(TechnicianRepositoryPort technicianRepo,
                                                                  UserRepositoryPort userRepo,
                                                                  PasswordHasherPort hasher) {
        return new ManageTechniciansUseCaseImpl(technicianRepo, userRepo, hasher);
    }
}
