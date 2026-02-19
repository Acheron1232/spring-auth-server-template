package com.acheron.authserver;

import com.acheron.authserver.entity.Role;
import com.acheron.authserver.entity.User;
import com.acheron.authserver.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Import;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.oauth2.core.AuthorizationGrantType;
import org.springframework.security.oauth2.core.ClientAuthenticationMethod;
import org.springframework.security.oauth2.server.authorization.client.RegisteredClient;
import org.springframework.security.oauth2.server.authorization.client.RegisteredClientRepository;
import org.springframework.security.oauth2.server.authorization.settings.ClientSettings;
import org.springframework.security.oauth2.server.authorization.settings.TokenSettings;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import java.time.Duration;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@Import(TestcontainersConfiguration.class)
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@AutoConfigureMockMvc
@ActiveProfiles("test")
@DisplayName("Public Client Refresh Token Tests")
class PublicClientRefreshTokenTest {

    @Autowired private MockMvc mockMvc;
    @Autowired private RegisteredClientRepository registeredClientRepository;
    @Autowired private UserRepository userRepository;
    @Autowired private PasswordEncoder passwordEncoder;

    @BeforeEach
    void setUp() {
        if (userRepository.findUserByUsername("publicclienttest").isEmpty()) {
            userRepository.save(User.builder()
                    .username("publicclienttest")
                    .email("publicclienttest@example.com")
                    .passwordHash(passwordEncoder.encode("password123"))
                    .role(Role.USER)
                    .enabled(true)
                    .emailVerified(true)
                    .mfaEnabled(false)
                    .build());
        }

        // Register a public client with _mobile suffix
        if (registeredClientRepository.findByClientId("test-spa_mobile") == null) {
            registeredClientRepository.save(RegisteredClient.withId(UUID.randomUUID().toString())
                    .clientId("test-spa_mobile")
                    .clientAuthenticationMethod(ClientAuthenticationMethod.NONE)
                    .authorizationGrantType(AuthorizationGrantType.AUTHORIZATION_CODE)
                    .redirectUri("http://localhost:3000/callback")
                    .scope("openid")
                    .tokenSettings(TokenSettings.builder()
                            .accessTokenTimeToLive(Duration.ofMinutes(5))
                            .refreshTokenTimeToLive(Duration.ofDays(20))
                            .reuseRefreshTokens(false)
                            .build())
                    .clientSettings(ClientSettings.builder()
                            .requireProofKey(true)
                            .requireAuthorizationConsent(false)
                            .build())
                    .build());
        }

        // Register a public client WITHOUT allowed suffix
        if (registeredClientRepository.findByClientId("test-spa-no-refresh") == null) {
            registeredClientRepository.save(RegisteredClient.withId(UUID.randomUUID().toString())
                    .clientId("test-spa-no-refresh")
                    .clientAuthenticationMethod(ClientAuthenticationMethod.NONE)
                    .authorizationGrantType(AuthorizationGrantType.AUTHORIZATION_CODE)
                    .redirectUri("http://localhost:3001/callback")
                    .scope("openid")
                    .tokenSettings(TokenSettings.builder()
                            .accessTokenTimeToLive(Duration.ofMinutes(5))
                            .build())
                    .clientSettings(ClientSettings.builder()
                            .requireProofKey(true)
                            .requireAuthorizationConsent(false)
                            .build())
                    .build());
        }
    }

    @ParameterizedTest(name = "Client {0} is registered as public (NONE auth method)")
    @ValueSource(strings = {"test-spa_mobile", "test-spa-no-refresh"})
    @DisplayName("Public clients are registered with NONE auth method")
    void publicClients_areRegisteredWithNoneAuthMethod(String clientId) {
        RegisteredClient client = registeredClientRepository.findByClientId(clientId);
        assertThat(client).isNotNull();
        assertThat(client.getClientAuthenticationMethods())
                .contains(ClientAuthenticationMethod.NONE);
    }

    @Test
    @DisplayName("Public client with _mobile suffix is registered")
    void publicClientWithMobileSuffix_isRegistered() {
        RegisteredClient client = registeredClientRepository.findByClientId("test-spa_mobile");
        assertThat(client).isNotNull();
        assertThat(client.getClientId()).endsWith("_mobile");
    }

    @Test
    @DisplayName("Refresh token request without client secret for non-allowed client returns error")
    void refreshToken_withoutSecret_forNonAllowedClient_returnsError() throws Exception {
        mockMvc.perform(post("/oauth2/token")
                        .param("grant_type", "refresh_token")
                        .param("refresh_token", "invalid-token")
                        .param("client_id", "test-spa-no-refresh"))
                .andExpect(status().isUnauthorized());
    }
}