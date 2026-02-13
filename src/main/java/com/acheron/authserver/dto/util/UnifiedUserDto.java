package com.acheron.authserver.dto.util;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class UnifiedUserDto {
    private String email;
    private String firstName;
    private String lastName;
    private String imageUrl;
    private String providerId;
    private String providerUserId;
}