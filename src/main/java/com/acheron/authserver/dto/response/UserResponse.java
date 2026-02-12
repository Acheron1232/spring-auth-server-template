package com.acheron.authserver.dto.response;

import com.acheron.authserver.entity.Role;
import com.acheron.authserver.entity.User;
import java.util.UUID;

public record UserResponse(
        UUID id,
        String email,
        String username,
        boolean emailVerified,
        boolean enabled,
        boolean mfaEnabled,
        Role role
) {

    public static UserResponse fromEntity(User user) {
        if (user == null) {
            return null;
        }
        return new UserResponse(
                user.getId(),
                user.getEmail(),
                user.getUsername(),
                user.isEmailVerified(),
                user.isEnabled(),
                user.isMfaEnabled(),
                user.getRole()
        );
    }

    public User toEntity() {
        return User.builder()
                .id(this.id)
                .email(this.email)
                .username(this.username)
                .emailVerified(this.emailVerified)
                .enabled(this.enabled)
                .mfaEnabled(this.mfaEnabled)
                .role(this.role)
                .build();
    }
}