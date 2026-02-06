package com.acheron.app.api;

import org.springframework.security.core.Authentication;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api")
public class SecuredApi {
    @GetMapping("/test")
    public String test(Authentication authentication) {
        assert authentication instanceof JwtAuthenticationToken;
        JwtAuthenticationToken jwtAuthenticationToken = (JwtAuthenticationToken) authentication;

        String username = authentication.getName();
        String jwtString = jwtAuthenticationToken.getToken().getTokenValue();
        return username;
    }
}
