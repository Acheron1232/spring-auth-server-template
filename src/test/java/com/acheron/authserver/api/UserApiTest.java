package com.acheron.authserver.api;

import com.acheron.authserver.dto.response.UserResponse;
import com.acheron.authserver.entity.Role;
import com.acheron.authserver.entity.User;
import com.acheron.authserver.service.QrCodeService;
import com.acheron.authserver.service.UserService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.http.ResponseEntity;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.BDDMockito.given;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(UserApi.class)
@DisplayName("UserApi unit tests")
public class UserApiTest {

    @MockitoBean private UserService userService;
    @MockitoBean private QrCodeService qrCodeService;
    @Autowired  private MockMvc mockMvc;

    private User mockUser;

    @BeforeEach
    void setUp() {
        mockUser = new User();
        mockUser.setEmail("test@example.com");
        mockUser.setUsername("testuser");
        mockUser.setRole(Role.USER);
        mockUser.setEnabled(true);
        mockUser.setEmailVerified(true);
        mockUser.setMfaEnabled(false);
        mockUser.setTokenVersion(UUID.randomUUID());
    }

    // ── GET /user-info ───────────────────────────────────────────────────────

    @Test
    @DisplayName("GET /user-info returns 200 with user JSON when authenticated")
    void shouldReturnCurrentUser() throws Exception {
        UserResponse response = UserResponse.fromEntity(mockUser);
        given(userService.getUserInfo(any(User.class))).willReturn(ResponseEntity.ok(response));

        mockMvc.perform(get("/user-info").with(user(mockUser)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.email").value("test@example.com"))
                .andExpect(jsonPath("$.username").value("testuser"));
    }

    @Test
    @DisplayName("GET /user-info without auth returns 401")
    void shouldReturn401WhenNotAuthenticated() throws Exception {
        mockMvc.perform(get("/user-info"))
                .andExpect(status().isUnauthorized());
    }

    // ── MFA setup ────────────────────────────────────────────────────────────

    @Test
    @DisplayName("POST /user-info/mfa/setup returns 200 with secret")
    void mfaSetup_returnsSecret() throws Exception {
        mockUser.setMfaSecret(null);
        given(userService.save(any())).willReturn(mockUser);

        mockMvc.perform(post("/user-info/mfa/setup")
                        .with(user(mockUser))
                        .with(csrf()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.secret").exists());
    }

    @Test
    @DisplayName("POST /user-info/mfa/setup without auth returns 401")
    void mfaSetup_withoutAuth_returns401() throws Exception {
        mockMvc.perform(post("/user-info/mfa/setup").with(csrf()))
                .andExpect(status().isUnauthorized());
    }

    // ── MFA verify ───────────────────────────────────────────────────────────

    @Test
    @DisplayName("POST /user-info/mfa/verify with no secret returns 400")
    void mfaVerify_withNoSecret_returns400() throws Exception {
        mockUser.setMfaSecret(null);

        mockMvc.perform(post("/user-info/mfa/verify")
                        .with(user(mockUser))
                        .with(csrf())
                        .param("code", "123456"))
                .andExpect(status().isBadRequest());
    }

    // ── MFA disable ──────────────────────────────────────────────────────────

    @Test
    @DisplayName("POST /user-info/mfa/disable when MFA not enabled returns 400")
    void mfaDisable_whenNotEnabled_returns400() throws Exception {
        mockUser.setMfaEnabled(false);

        mockMvc.perform(post("/user-info/mfa/disable")
                        .with(user(mockUser))
                        .with(csrf())
                        .param("code", "123456"))
                .andExpect(status().isBadRequest());
    }

    // ── Email confirmation ───────────────────────────────────────────────────

    @Test
    @DisplayName("POST /user-info/confirmEmail returns 200")
    void confirmEmail_returns200() throws Exception {
        given(userService.confirmEmail(anyString())).willReturn(ResponseEntity.ok("Confirmation email sent"));

        mockMvc.perform(post("/user-info/confirmEmail")
                        .with(user(mockUser))
                        .with(csrf()))
                .andExpect(status().isOk());
    }

    // ── Password reset ───────────────────────────────────────────────────────

    @ParameterizedTest(name = "POST /user-info/resetPassword returns 200 for email {0}")
    @ValueSource(strings = {"test@example.com", "unknown@example.com"})
    @DisplayName("POST /user-info/resetPassword always returns 200 (prevents enumeration)")
    void resetPassword_alwaysReturns200(String email) throws Exception {
        given(userService.resetPassword(anyString()))
                .willReturn(ResponseEntity.ok("If this email is registered, a reset link has been sent"));

        mockMvc.perform(post("/user-info/resetPassword")
                        .with(user(mockUser))
                        .with(csrf())
                        .contentType("application/json")
                        .content("{\"email\":\"" + email + "\"}"))
                .andExpect(status().isOk());
    }
}