package com.acheron.authserver.service;

import com.acheron.authserver.entity.User;
import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
@RequiredArgsConstructor
public class SessionManagementService {
    private final JdbcTemplate jdbcTemplate;

    @Transactional
    public void revokeAllSessions(User user) {
        rotateTokenVersion(user);
        revokeAllAuthorizations(user);
    }

    @Transactional
    public void rotateTokenVersion(User user) {
        UUID newVersion = UUID.randomUUID();
        jdbcTemplate.update("UPDATE users SET token_version = ? WHERE id = ?", newVersion, user.getId());
        user.setTokenVersion(newVersion);
    }

    @Transactional
    public void revokeAllAuthorizations(User user) {
        jdbcTemplate.update("DELETE FROM oauth2_authorization WHERE principal_name = ?", user.getUsername());
        jdbcTemplate.update("DELETE FROM oauth2_authorization_consent WHERE principal_name = ?", user.getUsername());
    }
}
