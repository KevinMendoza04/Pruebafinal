package com.riwi.helpflow.ticket.application.usecase;

import com.riwi.helpflow.ticket.application.port.in.RegisterUserUseCase;
import com.riwi.helpflow.ticket.application.port.out.PasswordHasherPort;
import com.riwi.helpflow.ticket.application.port.out.UserRepositoryPort;
import com.riwi.helpflow.ticket.domain.exception.DuplicateResourceException;
import com.riwi.helpflow.ticket.domain.model.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.*;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.UUID;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class RegisterUserUseCaseTest {

    @Mock UserRepositoryPort  userRepo;
    @Mock PasswordHasherPort  hasher;

    RegisterUserUseCaseImpl useCase;

    @BeforeEach void setUp() {
        useCase = new RegisterUserUseCaseImpl(userRepo, hasher);
    }

    private RegisterUserUseCase.Command cmd() {
        return new RegisterUserUseCase.Command(
                "1234567890", "Ana López", "ana@helpflow.com",
                "BOGOTA", "IT", "password123");
    }

    @Test @DisplayName("Successful registration returns ROLE_USUARIO ACTIVO user")
    void register_success() {
        when(userRepo.existsByEmail(anyString())).thenReturn(false);
        when(userRepo.existsByDocument(anyString())).thenReturn(false);
        when(hasher.hash("password123")).thenReturn("hashed");
        User saved = User.register(UUID.randomUUID(), "1234567890", "Ana López",
                "ana@helpflow.com", "BOGOTA", "IT", "hashed");
        when(userRepo.save(any())).thenReturn(saved);

        User result = useCase.register(cmd());

        assertThat(result.getRole()).isEqualTo(Role.ROLE_USUARIO);
        assertThat(result.getStatus()).isEqualTo(UserStatus.ACTIVO);
        verify(userRepo).save(any());
    }

    @Test @DisplayName("Duplicate email throws DuplicateResourceException")
    void register_duplicateEmail() {
        when(userRepo.existsByEmail("ana@helpflow.com")).thenReturn(true);
        assertThatThrownBy(() -> useCase.register(cmd()))
                .isInstanceOf(DuplicateResourceException.class)
                .hasMessageContaining("correo");
    }

    @Test @DisplayName("Duplicate document throws DuplicateResourceException")
    void register_duplicateDocument() {
        when(userRepo.existsByEmail(anyString())).thenReturn(false);
        when(userRepo.existsByDocument("1234567890")).thenReturn(true);
        assertThatThrownBy(() -> useCase.register(cmd()))
                .isInstanceOf(DuplicateResourceException.class);
    }
}
