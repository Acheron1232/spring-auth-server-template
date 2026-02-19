package com.acheron.authserver;

import com.acheron.authserver.config.TokenVersionCheckingOAuth2AuthorizationService;
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
import org.springframework.security.oauth2.server.authorization.OAuth2Authorization;
import org.springframework.security.oauth2.server.authorization.OAuth2AuthorizationService;
import org.springframework.security.oauth2.server.authorization.OAuth2TokenType;
import org.springframework.security.oauth2.server.authorization.client.RegisteredClientRepository;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@Import(TestcontainersConfiguration.class)
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@AutoConfigureMockMvc
@ActiveProfiles("test")
@DisplayName("Token Version Revocation Integration Tests")
class TokenVersionRevocationTest {

    @Autowired private MockMvc mockMvc;
    @Autowired private UserRepository userRepository;
    @Autowired private PasswordEncoder passwordEncoder;
    @Autowired private OAuth2AuthorizationService authorizationService;
    @Autowired private RegisteredClientRepository registeredClientRepository;

    private User testUser;

    @BeforeEach
    void setUp() {
        testUser = userRepository.findUserByUsername("revocationtest").orElseGet(() ->
                userRepository.save(User.builder()
                        .username("revocationtest")
                        .email("revocationtest@example.com")
                        .passwordHash(passwordEncoder.encode("password123"))
                        .role(Role.USER)
                        .enabled(true)
                        .emailVerified(true)
                        .mfaEnabled(false)
                        .tokenVersion(UUID.randomUUID())
                        .build()));
    }

    @Test
    @DisplayName("TokenVersionCheckingOAuth2AuthorizationService is the active bean")
    void authorizationService_isTokenVersionChecking() {
        assertThat(authorizationService)
                .isInstanceOf(TokenVersionCheckingOAuth2AuthorizationService.class);
    }

    @Test
    @DisplayName("Client credentials grant bypasses token version check")
    void clientCredentials_bypassesTokenVersionCheck() {
        var client = registeredClientRepository.findByClientId("gateway-client");
        assertThat(client).isNotNull();

        OAuth2Authorization authorization = OAuth2Authorization.withRegisteredClient(client)
                .id(UUID.randomUUID().toString())
                .principalName("gateway-client")
                .authorizationGrantType(AuthorizationGrantType.CLIENT_CREDENTIALS)
                .build();

        authorizationService.save(authorization);

        OAuth2Authorization found = authorizationService.findById(authorization.getId());
        assertThat(found).isNotNull();

        authorizationService.remove(authorization);
    }

    @Test
    @DisplayName("Token endpoint with invalid refresh token returns 400")
    void tokenEndpoint_withInvalidRefreshToken_returns400() throws Exception {
        mockMvc.perform(post("/oauth2/token")
                        .header("Authorization", "Basic Z2F0ZXdheS1jbGllbnQ6Z2F0ZXdheS1jbGllbnQtc2VjcmV0")
                        .param("grant_type", "refresh_token")
                        .param("refresh_token", "invalid-token-value"))
                .andExpect(status().isBadRequest());
    }
}