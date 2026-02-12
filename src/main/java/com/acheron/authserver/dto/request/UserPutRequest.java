package com.acheron.authserver.dto.request;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record UserPutRequest(
        @NotBlank(message = "Username cannot be empty")
        String username,

        @NotBlank(message = "Email cannot be empty")
        @Email(message = "Invalid email format")
        String email,

        @NotNull(message = "Enabled status must be specified")
        Boolean enabled,

        @NotNull(message = "Locked status must be specified")
        Boolean locked
) {
}