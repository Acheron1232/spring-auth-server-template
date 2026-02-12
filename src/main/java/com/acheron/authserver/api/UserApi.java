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
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.web.authentication.logout.SecurityContextLogoutHandler;
import org.springframework.web.bind.annotation.*;

import java.awt.image.BufferedImage;

@RestController
@RequiredArgsConstructor
@RequestMapping("/user-info")
public class UserApi {
    private final UserService userService;
    private final QrCodeService qrCodeService;

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
    public ResponseEntity<String> confirmEmail(@AuthenticationPrincipal Jwt jwt) {
        return userService.confirmEmail((String) jwt.getClaims().get("name"));
    }

    @GetMapping("/confirm")
    public ResponseEntity<String> confirm(@RequestParam("token") String token) {
        return userService.confirm(token);
    }

    @PostMapping("/resetPassword")
    public ResponseEntity<String> resetPassword(@RequestBody PasswordResetRequest passwordResetRequest) {
        return userService.resetPassword(passwordResetRequest.email());
    }

    @GetMapping("/resetPassword")
    public ResponseEntity<String> reset(@RequestParam("token") String token) {
        return ResponseEntity.ok(userService.reset(token));
    }

    @GetMapping(value = "/mfa_qr", produces = {MediaType.IMAGE_PNG_VALUE})
    @ResponseBody
    public BufferedImage getQrCode() {
        System.out.println("asd");
        return qrCodeService.generateQrCode("Talent", "aryemfedorov@gmail.com", "K4RJK7LR3FFUSTCG");
    }
}
