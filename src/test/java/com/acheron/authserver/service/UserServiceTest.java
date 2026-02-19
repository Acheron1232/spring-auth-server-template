package com.acheron.authserver.service;

import com.acheron.authserver.entity.Role;
import com.acheron.authserver.entity.Token;
import com.acheron.authserver.entity.User;
import com.acheron.authserver.exception.AppException;
import com.acheron.authserver.repository.FederatedIdentityRepository;
import com.acheron.authserver.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
@DisplayName("UserService unit tests")
class UserServiceTest {

    @Mock private UserRepository userRepository;
    @Mock private TokenService tokenService;
    @Mock private MailService mailService;
    @Mock private PasswordEncoder passwordEncoder;
    @Mock private com.acheron.authserver.mapper.UserMapper userMapper;
    @Mock private FederatedIdentityRepository federatedIdentityRepository;

    @InjectMocks
    private UserService userService;

    private User testUser;

    @BeforeEach
    void setUp() {
        testUser = User.builder()
                .username("testuser")
                .email("test@example.com")
                .passwordHash("$2a$10$hash")
                .role(Role.USER)
                .enabled(true)
                .emailVerified(false)
                .mfaEnabled(false)
                .tokenVersion(UUID.randomUUID())
                .build();
    }

    // ── findByEmail ──────────────────────────────────────────────────────────

    @Test
    @DisplayName("findByEmail returns user when found")
    void findByEmail_returnsUser_whenFound() {
        given(userRepository.findUserByEmail("test@example.com")).willReturn(Optional.of(testUser));
        User result = userService.findByEmail("test@example.com");
        assertThat(result).isNotNull().isEqualTo(testUser);
    }

    @Test
    @DisplayName("findByEmail throws AppException when not found")
    void findByEmail_throwsAppException_whenNotFound() {
        given(userRepository.findUserByEmail(anyString())).willReturn(Optional.empty());
        AppException ex = assertThrows(AppException.class, () -> userService.findByEmail("missing@example.com"));
        assertThat(ex.getStatus()).isEqualTo(HttpStatus.NOT_FOUND);
    }

    // ── findByUsername ───────────────────────────────────────────────────────

    @Test
    @DisplayName("findByUsername returns user when found")
    void findByUsername_returnsUser_whenFound() {
        given(userRepository.findUserByUsername("testuser")).willReturn(Optional.of(testUser));
        User result = userService.findByUsername("testuser");
        assertThat(result).isEqualTo(testUser);
    }

    @Test
    @DisplayName("findByUsername throws AppException when not found")
    void findByUsername_throwsAppException_whenNotFound() {
        given(userRepository.findUserByUsername(anyString())).willReturn(Optional.empty());
        assertThrows(AppException.class, () -> userService.findByUsername("ghost"));
    }

    // ── existsByEmail / existsByUsername ─────────────────────────────────────

    @ParameterizedTest(name = "existsByEmail returns {1} for email {0}")
    @CsvSource({"test@example.com,true", "unknown@example.com,false"})
    @DisplayName("existsByEmail returns correct boolean")
    void existsByEmail_returnsCorrectBoolean(String email, boolean expected) {
        given(userRepository.existsUserByEmail(email)).willReturn(expected);
        assertThat(userService.existsByEmail(email)).isEqualTo(expected);
    }

    @ParameterizedTest(name = "existsByUsername returns {1} for username {0}")
    @CsvSource({"testuser,true", "ghost,false"})
    @DisplayName("existsByUsername returns correct boolean")
    void existsByUsername_returnsCorrectBoolean(String username, boolean expected) {
        given(userRepository.existsUserByUsername(username)).willReturn(expected);
        assertThat(userService.existsByUsername(username)).isEqualTo(expected);
    }

    // ── resetPassword ────────────────────────────────────────────────────────

    @Test
    @DisplayName("resetPassword returns generic message when email not found (prevents enumeration)")
    void resetPassword_returnsGenericMessage_whenEmailNotFound() {
        given(userRepository.existsUserByEmail(anyString())).willReturn(false);
        var response = userService.resetPassword("nobody@example.com");
        assertThat(response.getStatusCode().value()).isEqualTo(200);
        assertThat(response.getBody()).contains("If this email");
    }

    @Test
    @DisplayName("resetPassword sends email when user exists")
    void resetPassword_sendsEmail_whenUserExists() throws Exception {
        given(userRepository.existsUserByEmail("test@example.com")).willReturn(true);
        given(userRepository.findUserByEmail("test@example.com")).willReturn(Optional.of(testUser));
        Token token = new Token();
        token.setToken(UUID.randomUUID().toString());
        token.setExpiredAt(Instant.now().plusSeconds(3600));
        given(tokenService.generateToken(any(), any())).willReturn(token);

        var response = userService.resetPassword("test@example.com");
        assertThat(response.getStatusCode().value()).isEqualTo(200);
        verify(mailService).sendMail(any());
    }

    // ── confirmEmail ─────────────────────────────────────────────────────────

    @Test
    @DisplayName("confirmEmail returns early when email already verified")
    void confirmEmail_returnsEarly_whenAlreadyVerified() {
        testUser.setEmailVerified(true);
        given(userRepository.findUserByUsername("testuser")).willReturn(Optional.of(testUser));
        var response = userService.confirmEmail("testuser");
        assertThat(response.getBody()).contains("already verified");
    }

    // ── changeRole ───────────────────────────────────────────────────────────

    @ParameterizedTest(name = "changeRole to {0} saves correctly")
    @ValueSource(strings = {"USER", "ADMIN"})
    @DisplayName("changeRole saves new role")
    void changeRole_savesNewRole(String roleName) {
        Role role = Role.valueOf(roleName);
        UUID id = UUID.randomUUID();
        given(userRepository.findById(id)).willReturn(Optional.of(testUser));
        given(userRepository.save(any())).willReturn(testUser);
        User result = userService.changeRole(id, role);
        assertThat(result.getRole()).isEqualTo(role);
    }

    // ── setLocked / setEnabled ───────────────────────────────────────────────

    @ParameterizedTest(name = "setLocked({0}) sets locked flag")
    @ValueSource(booleans = {true, false})
    @DisplayName("setLocked updates locked flag")
    void setLocked_updatesFlag(boolean locked) {
        UUID id = UUID.randomUUID();
        given(userRepository.findById(id)).willReturn(Optional.of(testUser));
        given(userRepository.save(any())).willAnswer(inv -> inv.getArgument(0));
        User result = userService.setLocked(id, locked);
        assertThat(result.isLocked()).isEqualTo(locked);
    }

    @ParameterizedTest(name = "setEnabled({0}) sets enabled flag")
    @ValueSource(booleans = {true, false})
    @DisplayName("setEnabled updates enabled flag")
    void setEnabled_updatesFlag(boolean enabled) {
        UUID id = UUID.randomUUID();
        given(userRepository.findById(id)).willReturn(Optional.of(testUser));
        given(userRepository.save(any())).willAnswer(inv -> inv.getArgument(0));
        User result = userService.setEnabled(id, enabled);
        assertThat(result.isEnabled()).isEqualTo(enabled);
    }
}