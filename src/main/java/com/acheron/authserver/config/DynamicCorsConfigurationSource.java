package com.acheron.authserver.config;

import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;

import java.util.List;

@Component
@RequiredArgsConstructor
public class DynamicCorsConfigurationSource implements CorsConfigurationSource {

    private final AllowedOriginsCache originsCache;

    @Override
    public CorsConfiguration getCorsConfiguration(HttpServletRequest request) {
        String origin = request.getHeader("Origin");
        if (origin == null) return null;

        if (!originsCache.isAllowed(origin)) {
            return null;
        }

        CorsConfiguration config = new CorsConfiguration();
        config.setAllowedOrigins(List.of(origin));
        config.setAllowedMethods(List.of("*"));
        config.setAllowedHeaders(List.of("*"));
        config.setAllowCredentials(true);
        return config;
    }
}