package com.acheron.authserver.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;

import java.util.List;

public record ClientRegistrationRequest(
        @NotBlank String clientId,
        String clientSecret,
        @NotEmpty List<String> redirectUris,
        @NotEmpty List<String> scopes,
        boolean requirePkce,
        boolean requireConsent,
        long accessTokenTtlMinutes,
        long refreshTokenTtlDays
) {
}
