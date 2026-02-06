package com.acheron.authserver.entity;

import lombok.Getter;

@Getter
public enum OAuthProvider {
    GITHUB("github"),
    GOOGLE("google"),
    MICROSOFT("microsoft"),
    APPLE("apple"),
    FACEBOOK("facebook");

    private final String registrationId;

    OAuthProvider(String registrationId) {
        this.registrationId = registrationId;
    }

    public static OAuthProvider fromRegistrationId(String registrationId) {
        for (OAuthProvider provider : values()) {
            if (provider.registrationId.equals(registrationId)) {
                return provider;
            }
        }
        throw new IllegalArgumentException("Unknown provider: " + registrationId);
    }
}
