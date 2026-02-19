package com.acheron.authserver.service;

import com.acheron.authserver.config.oauth_provider_handlers.OAuth2UserHandler;
import com.acheron.authserver.dto.request.MailDto;
import com.acheron.authserver.dto.request.UserPatchRequest;
import com.acheron.authserver.dto.request.UserPutRequest;
import com.acheron.authserver.dto.response.UserResponse;
import com.acheron.authserver.dto.util.UnifiedUserDto;
import com.acheron.authserver.entity.*;
import com.acheron.authserver.exception.AppException;
import com.acheron.authserver.mapper.UserMapper;
import com.acheron.authserver.repository.FederatedIdentityRepository;
import com.acheron.authserver.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.jspecify.annotations.NullMarked;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.ClassPathResource;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;
import org.springframework.web.server.ResponseStatusException;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class UserService implements UserDetailsService {

    private final UserRepository userRepository;
    private final TokenService tokenService;
    private final MailService mailService;
    private final PasswordEncoder passwordEncoder;
    private final UserMapper userMapper;
    private final FederatedIdentityRepository federatedIdentityRepository;
    private final List<OAuth2UserHandler> strategies;

    @Value("${app.base-url:http://localhost:9000}")
    private String baseUrl;

    // ── profile / self-service ───────────────────────────────────────────────

    public ResponseEntity<UserResponse> getUserInfo(User user) {
        return ResponseEntity.ok(UserResponse.fromEntity(user));
    }

    @Transactional
    public ResponseEntity<UserResponse> updateUser(User currentUser, UserPutRequest request) {
        validateUniqueness(request.email(), request.username(), currentUser);

        if (!currentUser.getEmail().equals(request.email())) {
            currentUser.setEmailVerified(false);
        }

        currentUser.setUsername(request.username());
        currentUser.setEmail(request.email());
        currentUser.setEnabled(request.enabled());
        currentUser.setLocked(request.locked());

        User savedUser = userRepository.save(currentUser);
        log.info("User {} fully updated their profile", savedUser.getId());
        return ResponseEntity.ok(UserResponse.fromEntity(savedUser));
    }

    @Transactional
    public ResponseEntity<UserResponse> patchUser(User currentUser, UserPatchRequest request) {
        String newEmail = request.email() != null ? request.email() : currentUser.getEmail();
        String newUsername = request.username() != null ? request.username() : currentUser.getUsername();

        if (request.email() != null || request.username() != null) {
            validateUniqueness(newEmail, newUsername, currentUser);
        }

        if (StringUtils.hasText(request.username())) currentUser.setUsername(request.username());

        if (StringUtils.hasText(request.email()) && !currentUser.getEmail().equals(request.email())) {
            currentUser.setEmail(request.email());
            currentUser.setEmailVerified(false);
        }

        if (request.enabled() != null) currentUser.setEnabled(request.enabled());
        if (request.locked() != null) currentUser.setLocked(request.locked());
        if (request.mfaEnabled() != null) currentUser.setMfaEnabled(request.mfaEnabled());

        User savedUser = userRepository.save(currentUser);
        log.info("User {} patched their profile", savedUser.getId());
        return ResponseEntity.ok(UserResponse.fromEntity(savedUser));
    }

    @Transactional
    public ResponseEntity<Void> delete(User user) {
        userRepository.delete(user);
        log.info("User account deleted: {}", user.getId());
        return ResponseEntity.noContent().build();
    }

    // ── admin operations ─────────────────────────────────────────────────────

    public Page<UserResponse> findAllPaged(String search, Pageable pageable) {
        return userRepository.findAllActive(search, pageable).map(UserResponse::fromEntity);
    }

    public User findById(UUID id) {
        return userRepository.findById(id)
                .orElseThrow(() -> new AppException("User not found", HttpStatus.NOT_FOUND));
    }

    @Transactional
    public User changeRole(UUID id, Role role) {
        User user = findById(id);
        user.setRole(role);
        User saved = userRepository.save(user);
        log.info("Admin changed role of user {} to {}", id, role);
        return saved;
    }

    @Transactional
    public User setLocked(UUID id, boolean locked) {
        User user = findById(id);
        user.setLocked(locked);
        User saved = userRepository.save(user);
        log.info("Admin {} user {}", locked ? "locked" : "unlocked", id);
        return saved;
    }

    @Transactional
    public User setEnabled(UUID id, boolean enabled) {
        User user = findById(id);
        user.setEnabled(enabled);
        User saved = userRepository.save(user);
        log.info("Admin {} user {}", enabled ? "enabled" : "disabled", id);
        return saved;
    }

    @Transactional
    public void deleteById(UUID id) {
        User user = findById(id);
        userRepository.delete(user);
        log.info("Admin deleted user {}", id);
    }

    // ── oauth / federated ────────────────────────────────────────────────────

    @Transactional
    public User saveOauthUser(String providerId, OAuth2User oauth2User) {
        UnifiedUserDto dto = extractUserDto(providerId, oauth2User);
        Optional<User> existingUser = userRepository.findUserByEmail(dto.getEmail());

        User user;
        if (existingUser.isPresent()) {
            user = existingUser.get();
        } else {
            user = userMapper.toUserEntity(dto);
            user = userRepository.save(user);
        }

        OAuthProvider provider = OAuthProvider.valueOf(providerId.toUpperCase());
        if (!user.hasFederatedIdentity(provider)) {
            FederatedIdentity identity = userMapper.toFederatedIdentity(dto, user);
            federatedIdentityRepository.save(identity);
        }
        return user;
    }

    public UnifiedUserDto extractUserDto(String registrationId, OAuth2User oauth2User) {
        return strategies.stream()
                .filter(s -> s.supports(registrationId))
                .findFirst()
                .map(s -> s.extract(registrationId, oauth2User))
                .orElseThrow(() -> new IllegalArgumentException(
                        "Login with " + registrationId + " is not supported."));
    }

    @Override
    @Transactional(readOnly = true)
    @NullMarked
    public UserDetails loadUserByUsername(String username) {
        return findByUsername(username);
    }

    // ── email / password flows ───────────────────────────────────────────────

    public ResponseEntity<String> confirmEmail(String username) {
        User user = findByUsername(username);
        if (user.isEmailVerified()) {
            return ResponseEntity.ok("Email already verified");
        }
        try {
            Token token = tokenService.generateToken(user, Token.TokenType.CONFIRM);
            String confirmUrl = baseUrl + "/user-info/confirm?token=" + token.getToken();
            String html = new ClassPathResource("static/confirmation.html")
                    .getContentAsString(StandardCharsets.UTF_8)
                    .replaceFirst("urll", confirmUrl);
            mailService.sendMail(new MailDto(user.getEmail(), "Email confirmation", html));
        } catch (IOException e) {
            log.error("Failed to send confirmation email", e);
        }
        return ResponseEntity.ok("Confirmation email sent");
    }

    public ResponseEntity<String> confirm(String token) {
        tokenService.getToken(token).ifPresentOrElse(
                confirmationToken -> {
                    if (!confirmationToken.getTokenType().equals(Token.TokenType.CONFIRM)) {
                        throw new BadCredentialsException("Invalid token type");
                    }
                    if (confirmationToken.getExpiredAt().isAfter(Instant.now())) {
                        User user = confirmationToken.getUser();
                        user.setEmailVerified(true);
                        save(user);
                        tokenService.delete(confirmationToken);
                    } else {
                        throw new BadCredentialsException("Token expired");
                    }
                },
                () -> { throw new BadCredentialsException("Invalid token"); }
        );
        return ResponseEntity.ok("Email confirmed successfully");
    }

    public ResponseEntity<String> resetPassword(String email) {
        if (!existsByEmail(email)) {
            return ResponseEntity.ok("If this email is registered, a reset link has been sent");
        }
        try {
            User user = findByEmail(email);
            Token token = tokenService.generateToken(user, Token.TokenType.RESET);
            String resetUrl = baseUrl + "/reset_password_token?token=" + token.getToken();
            String html = new ClassPathResource("static/reset_password.html")
                    .getContentAsString(StandardCharsets.UTF_8)
                    .replaceFirst("urll", resetUrl);
            mailService.sendMail(new MailDto(user.getEmail(), "Reset password", html));
            return ResponseEntity.ok("If this email is registered, a reset link has been sent");
        } catch (IOException e) {
            log.error("Failed to send password reset email", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    @Transactional
    public ResponseEntity<String> resetPasswordWithToken(String token, String newRawPassword) {
        tokenService.getToken(token).ifPresentOrElse(
                resetToken -> {
                    if (!resetToken.getTokenType().equals(Token.TokenType.RESET)) {
                        throw new BadCredentialsException("Invalid token type");
                    }
                    if (resetToken.getExpiredAt().isAfter(Instant.now())) {
                        User user = resetToken.getUser();
                        user.setPasswordHash(passwordEncoder.encode(newRawPassword));
                        save(user);
                        tokenService.delete(resetToken);
                        log.info("Password reset for user: {}", user.getUsername());
                    } else {
                        throw new BadCredentialsException("Token expired");
                    }
                },
                () -> { throw new BadCredentialsException("Invalid token"); }
        );
        return ResponseEntity.ok("Password reset successfully");
    }

    // ── helpers ──────────────────────────────────────────────────────────────

    private void validateUniqueness(String email, String username, User currentUser) {
        Optional<User> byEmail = userRepository.findUserByEmail(email);
        if (byEmail.isPresent() && !byEmail.get().getId().equals(currentUser.getId())) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Email is already in use");
        }
        Optional<User> byUsername = userRepository.findUserByUsername(username);
        if (byUsername.isPresent() && !byUsername.get().getId().equals(currentUser.getId())) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Username is already in use");
        }
    }

    public boolean existsByEmail(String email) {
        return userRepository.existsUserByEmail(email);
    }

    public boolean existsByUsername(String username) {
        return userRepository.existsUserByUsername(username);
    }

    public User findByUsername(String username) {
        return userRepository.findUserByUsername(username)
                .orElseThrow(() -> new AppException("User not found", HttpStatus.NOT_FOUND));
    }

    public User findByEmail(String email) {
        return userRepository.findUserByEmail(email)
                .orElseThrow(() -> new AppException("User not found", HttpStatus.NOT_FOUND));
    }

    public long countUsers() {
        return userRepository.count();
    }

    public User save(User user) {
        return userRepository.save(user);
    }
}