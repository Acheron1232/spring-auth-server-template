package com.acheron.authserver.config.oauth_provider_handlers;

import com.acheron.authserver.dto.util.UnifiedUserDto;
import org.springframework.security.oauth2.core.oidc.user.OidcUser;
import org.springframework.security.oauth2.core.user.OAuth2User;

public interface OidcUserHandler extends OAuth2UserHandler {

    @Override
    default UnifiedUserDto extract(String registrationId, OAuth2User user) {
        if (user instanceof OidcUser oidcUser) {
            return extractOidc(registrationId, oidcUser);
        }
        throw new IllegalArgumentException("User is not an OIDC user");
    }

    UnifiedUserDto extractOidc(String registrationId, OidcUser user);
}