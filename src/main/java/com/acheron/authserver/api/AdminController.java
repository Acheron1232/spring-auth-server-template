package com.acheron.authserver.api;

import com.acheron.authserver.entity.Role;
import com.acheron.authserver.repository.AuthHistoryRepository;
import com.acheron.authserver.service.SessionManagementService;
import com.acheron.authserver.service.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@Controller
@RequestMapping("/admin")
@RequiredArgsConstructor
@PreAuthorize("hasRole('ADMIN')")
public class AdminController {

    private final UserService userService;
    private final AuthHistoryRepository authHistoryRepository;
    private final SessionManagementService sessionManagementService;

    @GetMapping
    public String dashboard(Model model) {
        long totalUsers = userService.countUsers();
        model.addAttribute("totalUsers", totalUsers);
        return "admin/dashboard";
    }

    @GetMapping("/users")
    public String users(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(required = false) String search,
            Model model) {
        model.addAttribute("users", userService.findAllPaged(
                search, PageRequest.of(page, size, Sort.by("createdAt").descending())));
        model.addAttribute("search", search);
        model.addAttribute("roles", Role.values());
        return "admin/users";
    }

    @GetMapping("/users/{id}")
    public String userDetail(@PathVariable UUID id, Model model) {
        model.addAttribute("user", userService.findById(id));
        model.addAttribute("loginHistory", authHistoryRepository.findTop10ByUserOrderByTimestampDesc(userService.findById(id)));
        model.addAttribute("roles", Role.values());
        return "admin/user-detail";
    }

    @PostMapping("/users/{id}/lock")
    public String toggleLock(@PathVariable UUID id, @RequestParam boolean locked) {
        userService.setLocked(id, locked);
        return "redirect:/admin/users/" + id + "?updated=true";
    }

    @PostMapping("/users/{id}/enable")
    public String toggleEnable(@PathVariable UUID id, @RequestParam boolean enabled) {
        userService.setEnabled(id, enabled);
        return "redirect:/admin/users/" + id + "?updated=true";
    }

    @PostMapping("/users/{id}/role")
    public String changeRole(@PathVariable UUID id, @RequestParam Role role) {
        userService.changeRole(id, role);
        return "redirect:/admin/users/" + id + "?updated=true";
    }

    @PostMapping("/users/{id}/revoke")
    public String revokeSessions(@PathVariable UUID id) {
        sessionManagementService.revokeAllSessions(userService.findById(id));
        return "redirect:/admin/users/" + id + "?revoked=true";
    }

    @PostMapping("/users/{id}/delete")
    public String deleteUser(@PathVariable UUID id) {
        userService.deleteById(id);
        return "redirect:/admin/users?deleted=true";
    }

    @GetMapping("/clients")
    public String clients(Model model) {
        return "admin/clients";
    }
}
