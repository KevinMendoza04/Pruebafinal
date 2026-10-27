package com.riwi.helpflow.ticket.application.port.in;

import com.riwi.helpflow.ticket.application.usecase.AuthenticatedActor;
import com.riwi.helpflow.ticket.domain.model.User;
import com.riwi.helpflow.ticket.domain.model.UserStatus;

import java.util.List;
import java.util.UUID;

public interface ManageUsersUseCase {
    User getMe(AuthenticatedActor actor);
    User updateMe(AuthenticatedActor actor, String fullName, String email,
                  String site, String department);
    List<User> listUsers(AuthenticatedActor actor, int page, int size);
    User getUserById(AuthenticatedActor actor, UUID id);
    User updateUser(AuthenticatedActor actor, UUID id, String fullName, String email,
                    String site, String department);
    User changeUserStatus(AuthenticatedActor actor, UUID id, UserStatus newStatus);
}
