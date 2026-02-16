package com.acheron.app.api;

import com.acheron.app.entity.AppUser;
import com.acheron.app.repo.AppUserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.Instant;
import java.util.UUID;

@RestController
@RequestMapping("/api/me")
@RequiredArgsConstructor
public class MeApi {

    private final AppUserRepository appUserRepository;

    public record MeResponse(UUID id, String email, String displayName) {
    }

    @GetMapping
    public ResponseEntity<MeResponse> me(@AuthenticationPrincipal Jwt jwt) {
        String email = jwt.getClaimAsString("email");
        if (email == null || email.isBlank()) {
            email = jwt.getClaimAsString("sub");
        }

        String name = jwt.getClaimAsString("name");
        if (name == null || name.isBlank()) {
            name = email;
        }

        String finalEmail = email;
        String finalName = name;

        AppUser user = appUserRepository.findByEmail(finalEmail)
                .orElseGet(() -> new AppUser(UUID.randomUUID(), finalEmail, finalName, Instant.now()));

        user.setDisplayName(finalName);
        AppUser saved = appUserRepository.save(user);

        return ResponseEntity.ok(new MeResponse(saved.getId(), saved.getEmail(), saved.getDisplayName()));
    }
}
