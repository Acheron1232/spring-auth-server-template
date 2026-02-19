package com.acheron.authserver.api;

import com.acheron.authserver.dto.request.RegistrationRequest;
import com.acheron.authserver.entity.Role;
import com.acheron.authserver.entity.User;
import com.acheron.authserver.service.UserService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.jboss.aerogear.security.otp.api.Base32;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.authentication.logout.SecurityContextLogoutHandler;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.ResponseBody;

@Slf4j
@Controller
@RequiredArgsConstructor
public class AuthApi {
    private final UserService userService;
    private final PasswordEncoder passwordEncoder;

    @GetMapping("/login")
    public String login() {
        return "login";
    }

    @GetMapping("/registration")
    public String registration(Model model) {
        model.addAttribute("registrationRequest", new RegistrationRequest("", "", "", false));
        return "registration";
    }

    @PostMapping("/registration")
    public String registrationApi(
            @Valid @ModelAttribute("registrationRequest") RegistrationRequest request,
            BindingResult bindingResult,
            Model model) {

        if (bindingResult.hasErrors()) {
            return "registration";
        }

        if (userService.existsByEmail(request.email())) {
            bindingResult.rejectValue("email", "duplicate", "Email is already in use");
            return "registration";
        }
        if (userService.existsByUsername(request.username())) {
            bindingResult.rejectValue("username", "duplicate", "Username is already in use");
            return "registration";
        }

        String mfaSecret = null;
        boolean mfaEnabled = false;
        if (Boolean.TRUE.equals(request.mfaEnabled())) {
            mfaSecret = Base32.random();
            mfaEnabled = true;
        }

        User user = User.builder()
                .username(request.username())
                .email(request.email())
                .passwordHash(passwordEncoder.encode(request.password()))
                .role(Role.USER)
                .mfaEnabled(mfaEnabled)
                .mfaSecret(mfaSecret)
                .enabled(true)
                .emailVerified(false)
                .build();

        userService.save(user);
        log.info("New user registered: {}", user.getUsername());
        return "redirect:/login?registered=true";
    }

    @PostMapping("/spa/logout")
    @ResponseBody
    public ResponseEntity<Void> logout(HttpServletRequest request, HttpServletResponse response) {
        HttpSession session = request.getSession(false);
        if (session != null) {
            session.invalidate();
        }
        new SecurityContextLogoutHandler().logout(request, response,
                SecurityContextHolder.getContext().getAuthentication());
        SecurityContextHolder.clearContext();
        return ResponseEntity.ok().build();
    }
}