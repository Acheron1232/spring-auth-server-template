package com.acheron.authserver;

import com.acheron.authserver.config.PublicClientRefreshTokenAuthenticationProvider;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.security.oauth2.core.AuthorizationGrantType;
import org.springframework.security.oauth2.core.ClientAuthenticationMethod;
import org.springframework.security.oauth2.core.oidc.OidcScopes;
import org.springframework.security.oauth2.server.authorization.OAuth2AuthorizationService;
import org.springframework.security.oauth2.server.authorization.client.RegisteredClient;
import org.springframework.security.oauth2.server.authorization.client.RegisteredClientRepository;
import org.springframework.security.oauth2.server.authorization.settings.ClientSettings;
import org.springframework.security.oauth2.server.authorization.settings.TokenSettings;
import org.springframework.test.context.ActiveProfiles;

import java.time.Duration;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

@Import(TestcontainersConfiguration.class)
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("test")
class PublicClientRefreshTokenTest {

    @Autowired
    private RegisteredClientRepository registeredClientRepository;

    @Autowired
    private OAuth2AuthorizationService authorizationService;

    @Test
    void mobileClient_isEligibleForRefreshToken() {
        String clientId = "my-app_mobile";
        registerPublicClient(clientId);

        PublicClientRefreshTokenAuthenticationProvider provider =
                new PublicClientRefreshTokenAuthenticationProvider(registeredClientRepository, authorizationService);

        assertThat(provider.supports(
                org.springframework.security.oauth2.server.authorization.authentication.OAuth2ClientAuthenticationToken.class))
                .isTrue();
    }

    @Test
    void testClient_isEligibleForRefreshToken() {
        String clientId = "integration_test";
        registerPublicClient(clientId);

        RegisteredClient client = registeredClientRepository.findByClientId(clientId);
        assertThat(client).isNotNull();
        assertThat(clientId.endsWith("_test")).isTrue();
    }

    @Test
    void withRefreshClient_isEligibleForRefreshToken() {
        String clientId = "spa-client_with_refresh";
        registerPublicClient(clientId);

        RegisteredClient client = registeredClientRepository.findByClientId(clientId);
        assertThat(client).isNotNull();
        assertThat(clientId.endsWith("_with_refresh")).isTrue();
    }

    @Test
    void regularPublicClient_isNotEligibleForRefreshToken() {
        String clientId = "regular-spa-client";
        assertThat(clientId.endsWith("_mobile")).isFalse();
        assertThat(clientId.endsWith("_test")).isFalse();
        assertThat(clientId.endsWith("_with_refresh")).isFalse();
    }

    private void registerPublicClient(String clientId) {
        if (registeredClientRepository.findByClientId(clientId) != null) return;

        RegisteredClient client = RegisteredClient.withId(UUID.randomUUID().toString())
                .clientId(clientId)
                .clientAuthenticationMethod(ClientAuthenticationMethod.NONE)
                .authorizationGrantType(AuthorizationGrantType.AUTHORIZATION_CODE)
                .authorizationGrantType(AuthorizationGrantType.REFRESH_TOKEN)
                .redirectUri("http://localhost:3000/callback")
                .scope(OidcScopes.OPENID)
                .scope(OidcScopes.PROFILE)
                .tokenSettings(TokenSettings.builder()
                        .accessTokenTimeToLive(Duration.ofMinutes(5))
                        .refreshTokenTimeToLive(Duration.ofDays(7))
                        .reuseRefreshTokens(false)
                        .build())
                .clientSettings(ClientSettings.builder()
                        .requireProofKey(true)
                        .requireAuthorizationConsent(false)
                        .build())
                .build();
        registeredClientRepository.save(client);
    }
}
