package com.acheron.authserver;

import com.acheron.authserver.entity.Role;
import com.acheron.authserver.entity.User;
import com.acheron.authserver.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
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
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@Import(TestcontainersConfiguration.class)
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@AutoConfigureMockMvc
@ActiveProfiles("test")
class AuthorizationServerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private RegisteredClientRepository registeredClientRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @BeforeEach
    void setUp() {
        if (userRepository.findUserByUsername("authtest").isEmpty()) {
            User user = User.builder()
                    .username("authtest")
                    .email("authtest@example.com")
                    .passwordHash(passwordEncoder.encode("password123"))
                    .role(Role.USER)
                    .enabled(true)
                    .emailVerified(true)
                    .mfaEnabled(false)
                    .build();
            userRepository.save(user);
        }
    }

    @Test
    void gatewayClientIsRegistered() {
        RegisteredClient client = registeredClientRepository.findByClientId("gateway-client");
        assertThat(client).isNotNull();
        assertThat(client.getScopes()).contains("openid", "profile", "email");
    }

    @Test
    void wellKnownOpenIdConfiguration_isAccessible() throws Exception {
        mockMvc.perform(get("/.well-known/openid-configuration"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.issuer").exists())
                .andExpect(jsonPath("$.authorization_endpoint").exists())
                .andExpect(jsonPath("$.token_endpoint").exists())
                .andExpect(jsonPath("$.jwks_uri").exists());
    }

    @Test
    void jwksEndpoint_isAccessible() throws Exception {
        mockMvc.perform(get("/.well-known/jwks.json"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.keys").isArray());
    }

    @Test
    void loginPage_isAccessible() throws Exception {
        mockMvc.perform(get("/login"))
                .andExpect(status().isOk());
    }

    @Test
    void registrationPage_isAccessible() throws Exception {
        mockMvc.perform(get("/registration"))
                .andExpect(status().isOk());
    }

    @Test
    void registrationPost_createsUser() throws Exception {
        mockMvc.perform(post("/registration")
                        .param("username", "newregistered")
                        .param("email", "newregistered@example.com")
                        .param("password", "secure123"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/login"));

        assertThat(userRepository.findUserByUsername("newregistered")).isPresent();
    }

    @Test
    void protectedEndpoint_redirectsToLogin() throws Exception {
        mockMvc.perform(get("/user-info"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void tokenEndpoint_withoutCredentials_returns401() throws Exception {
        mockMvc.perform(post("/oauth2/token")
                        .param("grant_type", "authorization_code")
                        .param("code", "invalid"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void authorizeEndpoint_withoutAuth_redirectsToLogin() throws Exception {
        mockMvc.perform(get("/oauth2/authorize")
                        .param("response_type", "code")
                        .param("client_id", "gateway-client")
                        .param("scope", "openid")
                        .param("redirect_uri", "http://localhost:8080/login/oauth2/code/messaging-client-oidc"))
                .andExpect(status().is3xxRedirection());
    }

    @Test
    void actuatorHealth_isAccessible() throws Exception {
        mockMvc.perform(get("/actuator/health"))
                .andExpect(status().isOk());
    }
}
