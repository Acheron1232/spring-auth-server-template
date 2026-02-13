package com.acheron.authserver.service;

import com.acheron.authserver.config.oauth_provider_handlers.OAuth2UserHandler;
import com.acheron.authserver.dto.UserCreateDto;
import com.acheron.authserver.dto.request.MailDto;
import com.acheron.authserver.dto.request.UserPatchRequest;
import com.acheron.authserver.dto.request.UserPutRequest;
import com.acheron.authserver.dto.response.UserResponse;
import com.acheron.authserver.dto.util.UnifiedUserDto;
import com.acheron.authserver.entity.*;
import com.acheron.authserver.mapper.UserMapper;
import com.acheron.authserver.repository.FederatedIdentityRepository;
import com.acheron.authserver.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.jspecify.annotations.NullMarked;
import org.springframework.core.io.ClassPathResource;
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
import java.util.concurrent.atomic.AtomicReference;

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

    // api methods
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

        if (StringUtils.hasText(request.username())) {
            currentUser.setUsername(request.username());
        }

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

    private void validateUniqueness(String email, String username, User currentUser) {
        Optional<User> userByEmail = userRepository.findUserByEmail(email);
        if (userByEmail.isPresent() && !userByEmail.get().getId().equals(currentUser.getId())) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Email is already in use");
        }

        Optional<User> userByUsername = userRepository.findUserByUsername(username);
        if (userByUsername.isPresent() && !userByUsername.get().getId().equals(currentUser.getId())) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Username is already in use");
        }
    }

    // oauth methods
    @Transactional
    public void saveOauthUser(String providerId, OAuth2User oauth2User) {
        // 1. Екстракт даних через стратегію (як ми робили раніше)
        UnifiedUserDto dto = extractUserDto(providerId, oauth2User);

        // 2. Шукаємо юзера в БД
        Optional<User> existingUser = userRepository.findUserByEmail(dto.getEmail());

        User user;
        if (existingUser.isPresent()) {
            user = existingUser.get();
            // Тут можна оновити дані, якщо треба
        } else {
            // 3. Створення нового юзера через Mapper
            user = userMapper.toUserEntity(dto);
            user = userRepository.save(user);
        }

        // 4. Перевірка/Створення FederatedIdentity
        OAuthProvider provider = OAuthProvider.valueOf(providerId.toUpperCase());

        if (!user.hasFederatedIdentity(provider)) {
            FederatedIdentity identity = userMapper.toFederatedIdentity(dto, user);
            federatedIdentityRepository.save(identity);
        }
    }

    public UnifiedUserDto extractUserDto(String registrationId, OAuth2User oauth2User) {
        return strategies.stream()
                .filter(strategy -> strategy.supports(registrationId))
                .findFirst()
                .map(strategy -> strategy.extract(registrationId, oauth2User))
                .orElseThrow(() -> new IllegalArgumentException(
                        "Sorry, login with " + registrationId + " is not supported yet."));
    }

    @Override
    @Transactional(readOnly = true)
    @NullMarked
    public UserDetails loadUserByUsername(String username) {
        return findByUsername(username);
    }


    public ResponseEntity<String> confirmEmail(String username) {
        User user = findByUsername(username);
        if (user.isEmailVerified()) {
            return ResponseEntity.ok("Email verified");
        }
        try {

            Token token = tokenService.generateToken(user, Token.TokenType.RESET);
            String html = new ClassPathResource("static/confirmation.html").getContentAsString(StandardCharsets.UTF_8).replaceFirst("urll", "http://localhost:8080/user/confirm?token=" + token.getToken());
            mailService.sendMail(new MailDto(user.getEmail(), "Email confirmation", html));
        } catch (IOException e) {
            log.error(e.getMessage(), e);
        }
        return ResponseEntity.ok("Email sent successfully");
    }

    public ResponseEntity<String> confirm(String token) {
        tokenService.getToken(token).ifPresentOrElse(
                (confirmationToken) -> {
                    if (!confirmationToken.getTokenType().equals(Token.TokenType.CONFIRM)) {
                        throw new BadCredentialsException("Token is not confirm");
                    }
                    if (confirmationToken.getExpiredAt().isAfter(Instant.now())) {
                        User user = confirmationToken.getUser();
                        user.setEmailVerified(true);
                        save(user);
                        tokenService.delete(confirmationToken);
                    } else {
                        throw new BadCredentialsException("Token expired");
                    }
                }, () -> {
                    throw new BadCredentialsException("Invalid token");
                }
        );
        return ResponseEntity.ok("Email confirmed");
    }

    public ResponseEntity<String> resetPassword(String email) {
        try {

            boolean b = existsByEmail(email);
            if (!b) {
                return ResponseEntity.status(HttpStatus.BAD_REQUEST).body("Email does not register");
            }
            User user = findByEmail(email);
            Token token = tokenService.generateToken(user, Token.TokenType.RESET);
            String html = new ClassPathResource("static/reset_password.html").getContentAsString(StandardCharsets.UTF_8).replaceFirst("urll", "http://127.0.0.1:" + "9000" + "/reset_password_token?token=" + token.getToken());
            mailService.sendMail(new MailDto(user.getEmail(), "Reset password", html));
            return ResponseEntity.ok("Reset password sent successfully");
        } catch (IOException e) {
            log.error(e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    public String reset(String token) {
        AtomicReference<String> newPassword = new AtomicReference<>();
        tokenService.getToken(token).ifPresentOrElse(
                (confirmationToken) -> {
                    if (!confirmationToken.getTokenType().equals(Token.TokenType.RESET)) {
                        throw new BadCredentialsException("Token is not reset");
                    }
                    if (confirmationToken.getExpiredAt().isAfter(Instant.now())) {
                        String substring = UUID.randomUUID().toString().substring(0, 10);
                        String password = passwordEncoder.encode(substring);
                        User user = confirmationToken.getUser();
                        user.setPasswordHash(password);
                        save(user);
                        tokenService.delete(confirmationToken);
                        newPassword.set(substring);

                    } else {
                        throw new BadCredentialsException("Token expired");
                    }
                }, () -> {
                    throw new BadCredentialsException("Invalid token");
                }
        );
        return newPassword.get();
    }

    //base methods
    public boolean existsByEmail(String email) {
        return userRepository.existsUserByEmail(email);
    }

    public User findByUsername(String username) {
        return userRepository.findUserByUsername(username).orElseThrow(); //TODO
    }

    public User findByEmail(String email) {
        return userRepository.findUserByEmail(email).orElseThrow(); //TODO
    }

    public User save(User user) {
        return userRepository.save(user);
    }
}
