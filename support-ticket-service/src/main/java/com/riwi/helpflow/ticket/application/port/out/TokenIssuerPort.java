package com.riwi.helpflow.ticket.application.port.out;

import com.riwi.helpflow.ticket.domain.model.User;

public interface TokenIssuerPort {
    /** Issues a signed JWT for the given user. */
    String issue(User user);
}
