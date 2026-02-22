package com.acheron.authserver.config.oauth_provider_handlers;

import com.acheron.authserver.dto.util.UnifiedUserDto;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.stereotype.Component;

import java.util.Map;

@Component
public class DiscordHandler implements OAuth2UserHandler {

    @Override
    public boolean supports(String registrationId) {
        return "discord".equalsIgnoreCase(registrationId);
    }

    @Override
    public UnifiedUserDto extract(String registrationId, OAuth2User user) {
        Map<String, Object> attributes = user.getAttributes();
        
        String id = (String) attributes.get("id");
        String username = (String) attributes.get("username");
        String discriminator = (String) attributes.get("discriminator");
        String email = (String) attributes.get("email");
        String avatar = (String) attributes.get("avatar");
        
        String avatarUrl = null;
        if (avatar != null) {
            avatarUrl = String.format("https://cdn.discordapp.com/avatars/%s/%s.png", id, avatar);
        }
        
        return UnifiedUserDto.builder()
                .providerId(registrationId)
                .providerUserId(id)
                .email(email)
                .firstName(username)
                .lastName(null)
                .imageUrl(avatarUrl)
                .build();
    }
}
