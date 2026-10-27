package com.riwi.helpflow.ticket.application.port.in;

public interface AuthenticateUseCase {
    record Command(String email, String rawPassword) {}
    record Result(String accessToken, String tokenType, long expiresIn) {}
    Result authenticate(Command command);
}
