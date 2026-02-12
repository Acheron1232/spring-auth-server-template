package com.acheron.authserver.dto.request;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.Pattern;

public record UserPatchRequest(
        @Pattern(regexp = "^[a-zA-Z0-9._-]{3,}$", message = "Username must be at least 3 chars")
        String username,

        @Email(message = "Invalid email format")
        String email,

        Boolean enabled,
        Boolean locked,
        Boolean mfaEnabled
) {
}