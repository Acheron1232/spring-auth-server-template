package com.acheron.app.api;

import com.acheron.app.entity.AppUser;
import com.acheron.app.repo.AppUserRepository;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/users")
@RequiredArgsConstructor
public class UserApi {

    private final AppUserRepository userRepository;

    public record UserUpsertRequest(
            @NotBlank @Email @Size(max = 320) String email,
            @NotBlank @Size(max = 120) String displayName
    ) {
    }

    public record UserResponse(UUID id, String email, String displayName) {
        public static UserResponse fromEntity(AppUser u) {
            return new UserResponse(u.getId(), u.getEmail(), u.getDisplayName());
        }
    }

    @GetMapping
    public List<UserResponse> list() {
        return userRepository.findAll().stream().map(UserResponse::fromEntity).toList();
    }

    @GetMapping("/{id}")
    public ResponseEntity<UserResponse> get(@PathVariable UUID id) {
        return userRepository.findById(id)
                .map(UserResponse::fromEntity)
                .map(ResponseEntity::ok)
                .orElseGet(() -> ResponseEntity.notFound().build());
    }

    @PostMapping
    public ResponseEntity<UserResponse> create(@RequestBody @Valid UserUpsertRequest req) {
        if (userRepository.findByEmail(req.email()).isPresent()) {
            return ResponseEntity.badRequest().build();
        }

        AppUser user = new AppUser(UUID.randomUUID(), req.email(), req.displayName(), Instant.now());
        AppUser saved = userRepository.save(user);
        return ResponseEntity.ok(UserResponse.fromEntity(saved));
    }

    @PutMapping("/{id}")
    public ResponseEntity<UserResponse> update(@PathVariable UUID id, @RequestBody @Valid UserUpsertRequest req) {
        return userRepository.findById(id)
                .map(existing -> {
                    existing.setEmail(req.email());
                    existing.setDisplayName(req.displayName());
                    AppUser saved = userRepository.save(existing);
                    return ResponseEntity.ok(UserResponse.fromEntity(saved));
                })
                .orElseGet(() -> ResponseEntity.notFound().build());
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable UUID id) {
        if (!userRepository.existsById(id)) {
            return ResponseEntity.notFound().build();
        }
        userRepository.deleteById(id);
        return ResponseEntity.noContent().build();
    }
}
