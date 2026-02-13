package com.acheron.authserver.config.oauth_provider_handlers;

import com.acheron.authserver.dto.util.UnifiedUserDto;
import org.springframework.security.oauth2.core.user.OAuth2User;

public interface OAuth2UserHandler {
    
    boolean supports(String registrationId);

    UnifiedUserDto extract(String registrationId, OAuth2User user);
}