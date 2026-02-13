package com.acheron.authserver.mapper;

import com.acheron.authserver.dto.util.UnifiedUserDto;
import com.acheron.authserver.entity.FederatedIdentity;
import com.acheron.authserver.entity.OAuthProvider;
import com.acheron.authserver.entity.Role;
import com.acheron.authserver.entity.User;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.Map;

@Component
public class UserMapper {

    public User toUserEntity(UnifiedUserDto dto) {
        return User.builder()
                .email(dto.getEmail())
                // В якості username використовуємо email, або частину до @, якщо треба
                .username(dto.getEmail())
                .emailVerified(true) // OAuth провайдери зазвичай гарантують валідність пошти
                .enabled(true)
                .locked(false)
                .mfaEnabled(false)
                .role(Role.USER) // Роль за замовчуванням
                .passwordHash(null) // Пароль відсутній для OAuth користувачів
                .build();
    }

    public FederatedIdentity toFederatedIdentity(UnifiedUserDto dto, User user) {
        OAuthProvider providerEnum = resolveProvider(dto.getProviderId());

        Map<String, Object> metadata = new HashMap<>();
        metadata.put("firstName", dto.getFirstName());
        metadata.put("lastName", dto.getLastName());
        metadata.put("imageUrl", dto.getImageUrl());
        metadata.put("originalEmail", dto.getEmail());

        return FederatedIdentity.builder()
                .user(user)
                .provider(providerEnum)
                .providerUserId(dto.getProviderUserId())
                // Використовуємо email або ім'я як логін на стороні провайдера
                .providerUsername(dto.getEmail() != null ? dto.getEmail() : dto.getFirstName())
                .providerMetadata(metadata) // Зберігаємо профільні дані тут
                .build();
    }

    private OAuthProvider resolveProvider(String providerId) {
        if (providerId == null) {
            throw new IllegalArgumentException("Provider ID cannot be null");
        }
        try {
            return OAuthProvider.valueOf(providerId.toUpperCase());
        } catch (IllegalArgumentException e) {
            throw new IllegalArgumentException("Unsupported OAuth provider: " + providerId);
        }
    }
}