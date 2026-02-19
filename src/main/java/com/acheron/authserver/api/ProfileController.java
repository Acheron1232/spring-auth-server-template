package com.acheron.authserver.api;

import com.acheron.authserver.dto.request.UserPatchRequest;
import com.acheron.authserver.repository.AuthHistoryRepository;
import com.acheron.authserver.service.SessionManagementService;
import com.acheron.authserver.entity.User;
import com.acheron.authserver.service.UserService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.Pattern;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;

@Controller
@RequiredArgsConstructor
public class ProfileController {
    private final UserService userService;
    private final AuthHistoryRepository authHistoryRepository;
    private final SessionManagementService sessionManagementService;

    public record ProfileUpdateForm(
            @Pattern(regexp = "^[a-zA-Z0-9._-]{3,}$", message = "Username must be at least 3 chars")
            String username,

            @Email(message = "Invalid email format")
            String email,

            Boolean mfaEnabled
    ) {
    }

    @GetMapping("/profile")
    public String profile(
            @AuthenticationPrincipal User user,
            @RequestParam(value = "updated", required = false) String updated,
            @RequestParam(value = "revoked", required = false) String revoked,
            Model model) {

        ProfileUpdateForm form = new ProfileUpdateForm(user.getUsername(), user.getEmail(), user.isMfaEnabled());
        model.addAttribute("profile", form);
        model.addAttribute("updated", updated != null);
        model.addAttribute("revoked", revoked != null);
        model.addAttribute("emailVerified", user.isEmailVerified());
        model.addAttribute("loginHistory", authHistoryRepository.findTop10ByUserOrderByTimestampDesc(user));
        return "profile";
    }

    @PostMapping("/profile/revoke")
    public String revokeAllSessions(@AuthenticationPrincipal User user) {
        sessionManagementService.revokeAllSessions(user);
        return "redirect:/profile?revoked=true";
    }

    @PostMapping("/profile")
    public String updateProfile(
            @AuthenticationPrincipal User user,
            @Valid @ModelAttribute("profile") ProfileUpdateForm form,
            BindingResult bindingResult,
            Model model) {

        if (bindingResult.hasErrors()) {
            model.addAttribute("updated", false);
            model.addAttribute("revoked", false);
            model.addAttribute("emailVerified", user.isEmailVerified());
            model.addAttribute("loginHistory", authHistoryRepository.findTop10ByUserOrderByTimestampDesc(user));
            return "profile";
        }

        UserPatchRequest request = new UserPatchRequest(
                form.username(),
                form.email(),
                null,
                null,
                form.mfaEnabled() != null ? form.mfaEnabled() : Boolean.FALSE
        );

        userService.patchUser(user, request);
        return "redirect:/profile?updated=true";
    }
}
