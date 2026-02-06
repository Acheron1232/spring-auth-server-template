package com.acheron.authserver.api;

import com.acheron.authserver.service.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;

@RestController
@RequiredArgsConstructor
public class TokenApi {
    private final UserService userService;

    @PostMapping("/confirmEmail")
    public ResponseEntity<String> confirmEmail(@AuthenticationPrincipal Jwt jwt) {
        return userService.confirmEmail((String) jwt.getClaims().get("name"));
    }

    @GetMapping("/confirm")
    public ResponseEntity<String> confirm(@RequestParam("token") String token) {
        return userService.confirm(token);
    }

    @PostMapping("/resetPassword")
    public ResponseEntity<String> resetPassword(@RequestBody EmailDto email) {
        return userService.resetPassword(email.email);
    }

    @GetMapping("/resetPassword")
    public ResponseEntity<String> reset(@RequestParam("token") String token) {
        return ResponseEntity.ok(userService.reset(token));
    }

    public record EmailDto(String email) {
    }
}
