package com.acheron.authserver.api;

import com.acheron.authserver.entity.Role;
import com.acheron.authserver.entity.User;
import com.acheron.authserver.service.QrCodeService;
import com.acheron.authserver.service.UserService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.jboss.aerogear.security.otp.api.Base32;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;

import java.awt.image.BufferedImage;
import java.util.Map;

@Slf4j
@Controller
@RequiredArgsConstructor
public class AuthController {
    private final UserService userService;
    private final PasswordEncoder passwordEncoder;
    private final QrCodeService qrCodeService;

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

    @PostMapping("/reset_password")
    public String resetPassword(@RequestParam String email) {

        userService.resetPassword(email);
        return "login";
    }

    @GetMapping("/reset_password_token")
    @ResponseBody
    public ResponseEntity<String> reset(@RequestParam("token") String token) {
        return ResponseEntity.ok(userService.reset(token));
    }

    @GetMapping("/reset_password")
    public String resetPassword() {
        return "reset_password";
    }

    @GetMapping(value = "/mfa_qr", produces = {MediaType.IMAGE_PNG_VALUE})
    @ResponseBody
    public BufferedImage getQrCode() {
        System.out.println("asd");
        return qrCodeService.generateQrCode("Talent", "aryemfedorov@gmail.com", "K4RJK7LR3FFUSTCG");
    }

}