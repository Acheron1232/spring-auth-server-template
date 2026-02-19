package com.acheron.authserver.config;

import com.acheron.authserver.dto.util.ClientRegisteredEvent;
import jakarta.annotation.PostConstruct;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionalEventListener;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;

import java.net.URI;
import java.util.List;
import java.util.Set;
import java.util.concurrent.CopyOnWriteArraySet;

/**
 * Dynamic CORS configuration source that derives allowed origins
 * from the redirect URIs of registered OAuth2 clients in the database.
 * <p>
 * Origins are loaded at startup and updated dynamically when new clients
 * are registered via {@link ClientRegisteredEvent}.
 * No TTL cache — origins are held in a thread-safe Set and updated in real-time.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class DynamicCorsConfigurationSource implements CorsConfigurationSource {

    private final JdbcTemplate jdbcTemplate;
    private final Set<String> allowedOrigins = new CopyOnWriteArraySet<>();

    @PostConstruct
    public void init() {
        log.info("Loading CORS allowed origins from registered clients...");
        try {
            List<String> redirectUris = jdbcTemplate.queryForList(
                    "SELECT redirect_uris FROM oauth2_registered_client", String.class);

            for (String csv : redirectUris) {
                if (csv != null && !csv.isBlank()) {
                    for (String uri : csv.split("\\s+")) {
                        addOriginFromUri(uri.trim());
                    }
                }
            }
            log.info("CORS origins loaded: {}", allowedOrigins);
        } catch (Exception e) {
            log.error("Failed to load CORS origins from database", e);
        }
    }

    @TransactionalEventListener
    public void onClientRegistered(ClientRegisteredEvent event) {
        if (addOriginFromUri(event.redirectUri())) {
            log.info("Dynamically added CORS origin from: {}", event.redirectUri());
        }
    }

    public void addOrigin(String origin) {
        if (origin != null && !origin.isBlank()) {
            allowedOrigins.add(origin);
        }
    }

    @Override
    public CorsConfiguration getCorsConfiguration(HttpServletRequest request) {
        String origin = request.getHeader("Origin");
        if (origin == null || !allowedOrigins.contains(origin)) {
            return null;
        }

        CorsConfiguration config = new CorsConfiguration();
        config.setAllowedOrigins(List.of(origin));
        config.setAllowedMethods(List.of("GET", "POST", "PUT", "DELETE", "PATCH", "OPTIONS"));
        config.setAllowedHeaders(List.of("*"));
        config.setAllowCredentials(true);
        config.setMaxAge(3600L);
        return config;
    }

    private boolean addOriginFromUri(String uriString) {
        if (uriString == null || uriString.isBlank()) return false;
        try {
            URI uri = new URI(uriString);
            String scheme = uri.getScheme();
            String authority = uri.getAuthority();
            if (scheme != null && authority != null) {
                return allowedOrigins.add(scheme + "://" + authority);
            }
        } catch (Exception e) {
            log.warn("Failed to parse URI for CORS origin: '{}' — {}", uriString, e.getMessage());
        }
        return false;
    }
}
