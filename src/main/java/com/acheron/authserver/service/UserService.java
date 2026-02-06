package com.acheron.authserver.service;

import com.acheron.authserver.dto.UserCreateDto;
import com.acheron.authserver.dto.request.MailDto;
import com.acheron.authserver.entity.Role;
import com.acheron.authserver.entity.Token;
import com.acheron.authserver.entity.User;
import com.acheron.authserver.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.io.ClassPathResource;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
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

    public void saveOauthUser(UserCreateDto user) {
        User save = save(new User(null, user.user().email(), user.user().username(), null, user.user().isEmailVerified(), true, false, false, null, null, Role.USER));

//        save.setFederatedIdentities(Set.of(new FederatedIdentity(null,save, OAuthProvider.GITHUB,)));
        return; //TODO
    }

    public boolean existsByEmail(String email) {
        return userRepository.existsUserByEmail(email);
    }

    public User findByUsername(String username) {
        return userRepository.findUserByUsername(username).get(); //TODO
    }

    public User findByEmail(String email) {
        return userRepository.findUserByEmail(email).get(); //TODO
    }

    public User save(User user) {
        return userRepository.save(user);
    }

    @Override
    @Transactional(readOnly = true)
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
}
