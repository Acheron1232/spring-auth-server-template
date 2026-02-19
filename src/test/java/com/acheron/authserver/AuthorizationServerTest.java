package com.acheron.authserver;

import com.acheron.authserver.entity.Role;
import com.acheron.authserver.entity.User;
import com.acheron.authserver.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.oauth2.server.authorization.client.RegisteredClient;
import org.springframework.security.oauth2.server.authorization.client.RegisteredClientRepository;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@Import(TestcontainersConfiguration.class)
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@AutoConfigureMockMvc
@ActiveProfiles("test")
class AuthorizationServerTest {

    @Autowired private MockMvc mockMvc;
    @Autowired private RegisteredClientRepository registeredClientRepository;
    @Autowired private UserRepository userRepository;
    @Autowired private PasswordEncoder passwordEncoder;

    @BeforeEach
    void setUp() {
        if (userRepository.findUserByUsername("authtest").isEmpty()) {
            userRepository.save(User.builder()
                    .username("authtest")
                    .email("authtest@example.com")
                    .passwordHash(passwordEncoder.encode("password123"))
                    .role(Role.USER)
                    .enabled(true)
                    .emailVerified(true)
                    .mfaEnabled(false)
                    .build());
        }
    }

    // ── Client registration ──────────────────────────────────────────────────

    @Test
    @DisplayName("Gateway client is registered in the repository")
    void gatewayClientIsRegistered() {
        RegisteredClient client = registeredClientRepository.findByClientId("gateway-client");
        assertThat(client).isNotNull();
        assertThat(client.getScopes()).contains("openid", "profile", "email");
    }

    // ── Well-known endpoints ─────────────────────────────────────────────────

    @Test
    @DisplayName("/.well-known/openid-configuration returns required fields")
    void wellKnownOpenIdConfiguration_isAccessible() throws Exception {
        mockMvc.perform(get("/.well-known/openid-configuration"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.issuer").exists())
                .andExpect(jsonPath("$.authorization_endpoint").exists())
                .andExpect(jsonPath("$.token_endpoint").exists())
                .andExpect(jsonPath("$.jwks_uri").exists());
    }

    @Test
    @DisplayName("JWKS endpoint returns key array")
    void jwksEndpoint_isAccessible() throws Exception {
        mockMvc.perform(get("/.well-known/jwks.json"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.keys").isArray());
    }

    // ── Public pages ─────────────────────────────────────────────────────────

    @ParameterizedTest(name = "GET {0} is publicly accessible")
    @ValueSource(strings = {"/login", "/registration"})
    @DisplayName("Public pages return 200")
    void publicPages_areAccessible(String path) throws Exception {
        mockMvc.perform(get(path))
                .andExpect(status().isOk());
    }

    // ── Registration ─────────────────────────────────────────────────────────

    @Test
    @DisplayName("POST /registration creates user and redirects to login")
    void registrationPost_createsUser() throws Exception {
        mockMvc.perform(post("/registration")
                        .with(csrf())
                        .param("username", "newreguser")
                        .param("email", "newreguser@example.com")
                        .param("password", "secure1234"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrlPattern("/login*"));

        assertThat(userRepository.findUserByUsername("newreguser")).isPresent();
    }

    @ParameterizedTest(name = "Registration with invalid data: username={0}, email={1}, password={2}")
    @CsvSource({
        "'',test@example.com,password123",
        "ab,test@example.com,password123",
        "validuser,not-an-email,password123",
        "validuser,test@example.com,short"
    })
    @DisplayName("Registration with invalid input stays on registration page")
    void registrationPost_withInvalidData_staysOnPage(String username, String email, String password) throws Exception {
        mockMvc.perform(post("/registration")
                        .with(csrf())
                        .param("username", username)
                        .param("email", email)
                        .param("password", password))
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("Registration with duplicate email returns error")
    void registrationPost_withDuplicateEmail_returnsError() throws Exception {
        mockMvc.perform(post("/registration")
                        .with(csrf())
                        .param("username", "dupemailuser")
                        .param("email", "authtest@example.com")
                        .param("password", "password123"))
                .andExpect(status().isOk());
    }

    // ── Protected endpoints ──────────────────────────────────────────────────

    @Test
    @DisplayName("GET /user-info without auth returns 401")
    void protectedEndpoint_withoutAuth_returns401() throws Exception {
        mockMvc.perform(get("/user-info"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("GET /admin without ADMIN role returns 403")
    void adminEndpoint_withoutAdminRole_returns403() throws Exception {
        mockMvc.perform(get("/admin"))
                .andExpect(status().is3xxRedirection());
    }

    // ── Token endpoint ───────────────────────────────────────────────────────

    @Test
    @DisplayName("POST /oauth2/token without credentials returns 401")
    void tokenEndpoint_withoutCredentials_returns401() throws Exception {
        mockMvc.perform(post("/oauth2/token")
                        .param("grant_type", "authorization_code")
                        .param("code", "invalid"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("GET /oauth2/authorize without auth redirects to login")
    void authorizeEndpoint_withoutAuth_redirectsToLogin() throws Exception {
        mockMvc.perform(get("/oauth2/authorize")
                        .param("response_type", "code")
                        .param("client_id", "gateway-client")
                        .param("scope", "openid")
                        .param("redirect_uri", "http://localhost:8080/login/oauth2/code/messaging-client-oidc"))
                .andExpect(status().is3xxRedirection());
    }

    // ── Actuator ─────────────────────────────────────────────────────────────

    @Test
    @DisplayName("GET /actuator/health returns 200")
    void actuatorHealth_isAccessible() throws Exception {
        mockMvc.perform(get("/actuator/health"))
                .andExpect(status().isOk());
    }

    @ParameterizedTest(name = "Sensitive actuator endpoint {0} is not exposed")
    @ValueSource(strings = {"/actuator/env", "/actuator/beans", "/actuator/heapdump"})
    @DisplayName("Sensitive actuator endpoints are not exposed")
    void sensitiveActuatorEndpoints_areNotExposed(String path) throws Exception {
        mockMvc.perform(get(path))
                .andExpect(status().isNotFound());
    }
}