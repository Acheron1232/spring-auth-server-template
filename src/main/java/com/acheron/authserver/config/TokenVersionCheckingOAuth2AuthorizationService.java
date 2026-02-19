package com.acheron.authserver.config;

import com.acheron.authserver.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.lang.Nullable;
import org.springframework.security.oauth2.core.AuthorizationGrantType;
import org.springframework.security.oauth2.core.OAuth2AuthenticationException;
import org.springframework.security.oauth2.core.OAuth2Error;
import org.springframework.security.oauth2.core.OAuth2ErrorCodes;
import org.springframework.security.oauth2.server.authorization.OAuth2Authorization;
import org.springframework.security.oauth2.server.authorization.OAuth2AuthorizationService;
import org.springframework.security.oauth2.server.authorization.OAuth2TokenType;
import java.util.Optional;
import java.util.UUID;

@RequiredArgsConstructor
public class TokenVersionCheckingOAuth2AuthorizationService implements OAuth2AuthorizationService {

    public static final String ATTR_TOKEN_VERSION = "token_version";

    private final OAuth2AuthorizationService delegate;
    private final UserRepository userRepository;

    @Override
    public void save(OAuth2Authorization authorization) {
        delegate.save(stampTokenVersion(authorization));
    }

    @Override
    public void remove(OAuth2Authorization authorization) {
        delegate.remove(authorization);
    }

    @Override
    @Nullable
    public OAuth2Authorization findById(String id) {
        return delegate.findById(id);
    }

    @Override
    @Nullable
    public OAuth2Authorization findByToken(String token, @Nullable OAuth2TokenType tokenType) {
        OAuth2Authorization authorization = delegate.findByToken(token, tokenType);
        if (authorization == null) return null;

        if (OAuth2TokenType.REFRESH_TOKEN.equals(tokenType)) {
            verifyTokenVersion(authorization);
        }
        return authorization;
    }

    private OAuth2Authorization stampTokenVersion(OAuth2Authorization authorization) {
        Object stored = authorization.getAttributes().get(ATTR_TOKEN_VERSION);
        if (stored != null) return authorization;

        if (AuthorizationGrantType.CLIENT_CREDENTIALS.equals(authorization.getAuthorizationGrantType())) {
            return authorization;
        }

        String principalName = authorization.getPrincipalName();
        Optional<com.acheron.authserver.entity.User> userOpt = userRepository.findUserByUsername(principalName);
        if (userOpt.isEmpty()) return authorization;
        UUID version = userOpt.get().getTokenVersion();

        return OAuth2Authorization.from(authorization)
                .attributes(a -> a.put(ATTR_TOKEN_VERSION, version.toString()))
                .build();
    }

    private void verifyTokenVersion(OAuth2Authorization authorization) {
        if (AuthorizationGrantType.CLIENT_CREDENTIALS.equals(authorization.getAuthorizationGrantType())) {
            return;
        }

        String principalName = authorization.getPrincipalName();
        Optional<com.acheron.authserver.entity.User> userOpt = userRepository.findUserByUsername(principalName);
        if (userOpt.isEmpty()) {
            delegate.remove(authorization);
            throw invalidGrant("User not found");
        }

        Object stored = authorization.getAttributes().get(ATTR_TOKEN_VERSION);
        if (stored == null) {
            throw invalidGrant("Missing token_version");
        }

        UUID current = userOpt.get().getTokenVersion();
        if (!current.toString().equals(String.valueOf(stored))) {
            delegate.remove(authorization);
            throw invalidGrant("Token revoked — all sessions invalidated");
        }
    }

    private static OAuth2AuthenticationException invalidGrant(String description) {
        OAuth2Error error = new OAuth2Error(OAuth2ErrorCodes.INVALID_GRANT, description, null);
        return new OAuth2AuthenticationException(error);
    }
}
