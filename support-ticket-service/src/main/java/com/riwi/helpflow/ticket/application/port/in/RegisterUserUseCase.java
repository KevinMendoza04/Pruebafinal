package com.riwi.helpflow.ticket.application.port.in;

import com.riwi.helpflow.ticket.domain.model.User;

public interface RegisterUserUseCase {
    record Command(String document, String fullName, String email,
                   String site, String department, String rawPassword) {}
    User register(Command command);
}
