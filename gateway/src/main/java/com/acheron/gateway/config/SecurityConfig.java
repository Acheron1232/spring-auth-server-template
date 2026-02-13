package com.acheron.gateway.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.env.Environment;
import org.springframework.core.env.Profiles;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.web.reactive.EnableWebFluxSecurity;
import org.springframework.security.config.web.server.ServerHttpSecurity;
import org.springframework.security.web.csrf.CsrfTokenRequestAttributeHandler;
import org.springframework.security.web.server.SecurityWebFilterChain;
import org.springframework.security.web.server.csrf.CookieServerCsrfTokenRepository;
import org.springframework.security.web.server.csrf.ServerCsrfTokenRequestAttributeHandler;

@Configuration
@EnableWebFluxSecurity
public class SecurityConfig {
    private final Environment env;

    public SecurityConfig(Environment env) {
        this.env = env;
    }
    @Bean
    public SecurityWebFilterChain springSecurityFilterChain(ServerHttpSecurity http) {
        CookieServerCsrfTokenRepository csrfTokenRepository = CookieServerCsrfTokenRepository.withHttpOnlyFalse();
        boolean isDev = env.acceptsProfiles(Profiles.of("dev"));
        csrfTokenRepository.setCookieCustomizer(cookie -> {
            if (isDev) {
                cookie.sameSite("None");
            } else {
                cookie.sameSite("Lax");
            }
            cookie.secure(true);
        });
        http.csrf(csrf->csrf.
                csrfTokenRepository(csrfTokenRepository).
                csrfTokenRequestHandler(new ServerCsrfTokenRequestAttributeHandler()));

        http
            .oauth2Login(Customizer.withDefaults());
        return http.build();
    }
}