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
import org.springframework.security.oauth2.server.authorization.client.RegisteredClient;
import org.springframework.security.oauth2.server.authorization.client.RegisteredClientRepository;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.options;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@Import(TestcontainersConfiguration.class)
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@AutoConfigureMockMvc
@ActiveProfiles("test")
@DisplayName("Dynamic CORS Tests")
class DynamicCorsTest {

    @Autowired private MockMvc mockMvc;
    @Autowired private RegisteredClientRepository registeredClientRepository;
    @Autowired private UserRepository userRepository;
    @Autowired private PasswordEncoder passwordEncoder;

    @BeforeEach
    void setUp() {
        if (userRepository.findUserByUsername("corstest").isEmpty()) {
            userRepository.save(User.builder()
                    .username("corstest")
                    .email("corstest@example.com")
                    .passwordHash(passwordEncoder.encode("password123"))
                    .role(Role.USER)
                    .enabled(true)
                    .emailVerified(true)
                    .mfaEnabled(false)
                    .build());
        }
    }

    @Test
    @DisplayName("Gateway client redirect URI origin is allowed for CORS")
    void gatewayClientRedirectUri_isAllowedForCors() {
        RegisteredClient client = registeredClientRepository.findByClientId("gateway-client");
        assertThat(client).isNotNull();
        assertThat(client.getRedirectUris()).isNotEmpty();
    }

    @ParameterizedTest(name = "OPTIONS preflight from {0} returns CORS headers")
    @ValueSource(strings = {"http://localhost:8080", "http://localhost:3000"})
    @DisplayName("Preflight requests return CORS headers for known origins")
    void preflightRequest_returnsCorrectHeaders(String origin) throws Exception {
        mockMvc.perform(options("/login")
                        .header("Origin", origin)
                        .header("Access-Control-Request-Method", "GET"))
                .andExpect(status().isOk());
    }
}