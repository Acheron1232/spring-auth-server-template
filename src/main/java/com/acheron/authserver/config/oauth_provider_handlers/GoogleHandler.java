package com.acheron.authserver.config.oauth_provider_handlers;

import com.acheron.authserver.dto.util.UnifiedUserDto;
import org.springframework.security.oauth2.core.oidc.user.OidcUser;
import org.springframework.stereotype.Component;

@Component
public class GoogleHandler implements OidcUserHandler {

    @Override
    public boolean supports(String registrationId) {
        return "google".equalsIgnoreCase(registrationId);
    }

    @Override
    public UnifiedUserDto extractOidc(String registrationId, OidcUser user) {
        return UnifiedUserDto.builder()
                .providerId(registrationId)
                .providerUserId(user.getSubject())
                .email(user.getEmail())
                .firstName(user.getGivenName())
                .lastName(user.getFamilyName())
                .imageUrl(user.getPicture())
                .build();
    }
}