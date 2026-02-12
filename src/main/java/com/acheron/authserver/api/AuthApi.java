package com.acheron.authserver.api;

import com.acheron.authserver.entity.Role;
import com.acheron.authserver.entity.User;
import com.acheron.authserver.service.UserService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.jboss.aerogear.security.otp.api.Base32;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;

import java.util.Map;

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
    public String registration() {
        return "registration";
    }

    @PostMapping("/registration")
    public String registrationApi(@RequestParam Map<String, String> params) {
        String mfa_secret = null;
        if (params.getOrDefault("mfa_enabled", "false").equals("true")) {
            mfa_secret = Base32.random();
        }
        User user = User.builder()
                .username(params.get("username"))
                .email(params.get("email"))
                .passwordHash(passwordEncoder.encode(params.get("password")))
                .role(Role.USER)
                .mfaEnabled(false)
                .mfaSecret(null)
                .enabled(true)
                .emailVerified(false)
                .build();

        userService.save(user);
        return "redirect:/login";
    }

    @GetMapping("/spa/logout")
    @ResponseBody
    public void logout(HttpServletRequest request) {
        HttpSession session = request.getSession(true);
        if (session != null) {
            session.invalidate();
        }
        SecurityContextHolder.clearContext();
    }

}