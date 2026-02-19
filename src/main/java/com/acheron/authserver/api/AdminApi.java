package com.acheron.authserver.api;

import com.acheron.authserver.dto.request.ClientRegistrationRequest;
import com.acheron.authserver.dto.response.UserResponse;
import com.acheron.authserver.entity.Role;
import com.acheron.authserver.entity.User;
import com.acheron.authserver.service.SessionManagementService;
import com.acheron.authserver.service.UserService;
import com.acheron.authserver.service.auth_server.ClientService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/admin/api")
@RequiredArgsConstructor
@PreAuthorize("hasRole('ADMIN')")
public class AdminApi {

    private final UserService userService;
    private final SessionManagementService sessionManagementService;
    private final ClientService clientService;

    @GetMapping("/users")
    public ResponseEntity<Page<UserResponse>> listUsers(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(required = false) String search) {
        return ResponseEntity.ok(userService.findAllPaged(search, PageRequest.of(page, size, Sort.by("createdAt").descending())));
    }

    @GetMapping("/users/{id}")
    public ResponseEntity<UserResponse> getUser(@PathVariable UUID id) {
        return ResponseEntity.ok(UserResponse.fromEntity(userService.findById(id)));
    }

    @PatchMapping("/users/{id}/role")
    public ResponseEntity<UserResponse> changeRole(
            @PathVariable UUID id,
            @RequestParam Role role) {
        return ResponseEntity.ok(UserResponse.fromEntity(userService.changeRole(id, role)));
    }

    @PatchMapping("/users/{id}/lock")
    public ResponseEntity<UserResponse> toggleLock(
            @PathVariable UUID id,
            @RequestParam boolean locked) {
        return ResponseEntity.ok(UserResponse.fromEntity(userService.setLocked(id, locked)));
    }

    @PatchMapping("/users/{id}/enable")
    public ResponseEntity<UserResponse> toggleEnable(
            @PathVariable UUID id,
            @RequestParam boolean enabled) {
        return ResponseEntity.ok(UserResponse.fromEntity(userService.setEnabled(id, enabled)));
    }

    @DeleteMapping("/users/{id}")
    public ResponseEntity<Void> deleteUser(@PathVariable UUID id) {
        userService.deleteById(id);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/users/{id}/revoke-sessions")
    public ResponseEntity<Void> revokeSessions(@PathVariable UUID id) {
        User user = userService.findById(id);
        sessionManagementService.revokeAllSessions(user);
        return ResponseEntity.ok().build();
    }

    @PostMapping("/clients")
    public ResponseEntity<Void> registerClient(@Valid @RequestBody ClientRegistrationRequest request) {
        clientService.registerNewClientFromRequest(request);
        return ResponseEntity.ok().build();
    }

    @DeleteMapping("/clients/{clientId}")
    public ResponseEntity<Void> deleteClient(@PathVariable String clientId) {
        clientService.deleteClient(clientId);
        return ResponseEntity.noContent().build();
    }
}
