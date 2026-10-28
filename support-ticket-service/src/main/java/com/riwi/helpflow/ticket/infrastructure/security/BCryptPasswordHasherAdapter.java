package com.riwi.helpflow.ticket.infrastructure.security;

import com.riwi.helpflow.ticket.application.port.out.PasswordHasherPort;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

@Component
public class BCryptPasswordHasherAdapter implements PasswordHasherPort {
    private final PasswordEncoder encoder = new BCryptPasswordEncoder();

    @Override public String hash(String rawPassword) { return encoder.encode(rawPassword); }
    @Override public boolean matches(String rawPassword, String hashedPassword) {
        return encoder.matches(rawPassword, hashedPassword);
    }
}
