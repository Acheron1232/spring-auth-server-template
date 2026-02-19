package com.acheron.authserver.api;

import com.acheron.authserver.dto.request.PasswordResetRequest;
import com.acheron.authserver.dto.request.UserPatchRequest;
import com.acheron.authserver.dto.request.UserPutRequest;
import com.acheron.authserver.dto.response.UserResponse;
import com.acheron.authserver.entity.User;
import com.acheron.authserver.service.QrCodeService;
import com.acheron.authserver.service.UserService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.jboss.aerogear.security.otp.Totp;
import org.jboss.aerogear.security.otp.api.Base32;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.web.authentication.logout.SecurityContextLogoutHandler;
import org.springframework.web.bind.annotation.*;

import java.awt.image.BufferedImage;

@RestController
@RequiredArgsConstructor
@RequestMapping("/user-info")
public class UserApi {
    private final UserService userService;
    private final QrCodeService qrCodeService;

    @Value("${spring.application.name:AuthServer}")
    private String appName;

    @GetMapping
    public ResponseEntity<UserResponse> getCurrentUser(@AuthenticationPrincipal User user) {
        return userService.getUserInfo(user);
    }

    @PutMapping
    public ResponseEntity<UserResponse> updateUser(
            @AuthenticationPrincipal User user,
            @RequestBody @Valid UserPutRequest request) {
        return userService.updateUser(user, request);
    }

    @PatchMapping
    public ResponseEntity<UserResponse> patchUser(
            @AuthenticationPrincipal User user,
            @RequestBody @Valid UserPatchRequest request) {
        return userService.patchUser(user, request);
    }

    @DeleteMapping
    public ResponseEntity<Void> deleteUser(
            @AuthenticationPrincipal User user,
            HttpServletRequest request,
            HttpServletResponse response) {

        ResponseEntity<Void> result = userService.delete(user);

        if (result.getStatusCode().is2xxSuccessful()) {
            SecurityContextLogoutHandler logoutHandler = new SecurityContextLogoutHandler();
            logoutHandler.logout(request, response, null);
        }
        return result;
    }

    @PostMapping("/confirmEmail")
    public ResponseEntity<String> confirmEmail(@AuthenticationPrincipal User user) {
        return userService.confirmEmail(user.getUsername());
    }

    @GetMapping("/confirm")
    public ResponseEntity<String> confirm(@RequestParam("token") String token) {
        return userService.confirm(token);
    }

    @PostMapping("/resetPassword")
    public ResponseEntity<String> resetPassword(@RequestBody PasswordResetRequest passwordResetRequest) {
        return userService.resetPassword(passwordResetRequest.email());
    }

    @PostMapping("/resetPassword/confirm")
    public ResponseEntity<String> confirmResetPassword(
            @RequestParam("token") String token,
            @RequestParam("password") String newPassword) {
        return userService.resetPasswordWithToken(token, newPassword);
    }

    @GetMapping(value = "/mfa/qr", produces = {MediaType.IMAGE_PNG_VALUE})
    @ResponseBody
    public BufferedImage getQrCode(@AuthenticationPrincipal User user) {
        if (user.getMfaSecret() == null || user.getMfaSecret().isBlank()) {
            throw new IllegalStateException("MFA is not configured for this user. Call POST /user-info/mfa/setup first.");
        }
        return qrCodeService.generateQrCode(appName, user.getEmail(), user.getMfaSecret());
    }

    @PostMapping("/mfa/setup")
    public ResponseEntity<MfaSetupResponse> setupMfa(@AuthenticationPrincipal User user) {
        String secret = Base32.random();
        user.setMfaSecret(secret);
        userService.save(user);
        return ResponseEntity.ok(new MfaSetupResponse(secret));
    }

    @PostMapping("/mfa/verify")
    public ResponseEntity<String> verifyAndEnableMfa(
            @AuthenticationPrincipal User user,
            @RequestParam String code) {
        if (user.getMfaSecret() == null || user.getMfaSecret().isBlank()) {
            return ResponseEntity.badRequest().body("MFA secret not set. Call POST /user-info/mfa/setup first.");
        }
        if (!new Totp(user.getMfaSecret()).verify(code)) {
            return ResponseEntity.badRequest().body("Invalid TOTP code");
        }
        user.setMfaEnabled(true);
        userService.save(user);
        return ResponseEntity.ok("MFA enabled successfully");
    }

    @PostMapping("/mfa/disable")
    public ResponseEntity<String> disableMfa(
            @AuthenticationPrincipal User user,
            @RequestParam String code) {
        if (!user.isMfaEnabled()) {
            return ResponseEntity.badRequest().body("MFA is not enabled");
        }
        if (!new Totp(user.getMfaSecret()).verify(code)) {
            return ResponseEntity.badRequest().body("Invalid TOTP code");
        }
        user.setMfaEnabled(false);
        user.setMfaSecret(null);
        userService.save(user);
        return ResponseEntity.ok("MFA disabled successfully");
    }

    public record MfaSetupResponse(String secret) {}
}