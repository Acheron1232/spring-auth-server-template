package com.acheron.authserver.config;

import com.acheron.authserver.dto.util.ClientRegisteredEvent;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionalEventListener;

import java.net.URI;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

/**
 * High-performance, in-memory cache for CORS Allowed Origins.
 * <p>
 * This component loads allowed origins (extracted from Redirect URIs) into a Set
 * at startup and listens for new client registrations to update the cache dynamically.
 * This prevents hitting the database on every OPTIONS/Pre-flight request.
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class AllowedOriginsCache {

    private final JdbcTemplate jdbcTemplate;

    // Thread-safe Set to handle concurrent read/write operations
    private final Set<String> originCache = ConcurrentHashMap.newKeySet();

    /**
     * Initializes the cache during application startup.
     * Loads all existing 'redirect_uris' from the database and extracts their origins.
     */
    @PostConstruct
    public void init() {
        log.info("Initializing CORS Allowed Origins Cache from database...");
        long startTime = System.currentTimeMillis();

        try {
            // Fetch raw CSV strings of redirect URIs from the standard OAuth2 table
            List<String> allRedirectUris = jdbcTemplate.queryForList(
                    "SELECT redirect_uris FROM oauth2_registered_client",
                    String.class
            );

            log.debug("Found {} rows of client configurations in database.", allRedirectUris.size());

            if (allRedirectUris.isEmpty()) {
                log.warn("No clients found in the database. CORS cache will be empty!");
                return;
            }

            for (String urisCommaSeparated : allRedirectUris) {
                if (urisCommaSeparated != null && !urisCommaSeparated.isBlank()) {
                    String[] uris = urisCommaSeparated.split(",");
                    for (String uri : uris) {
                        // Attempt to parse and cache each URI
                        extractAndAddOrigin(uri.trim());
                    }
                }
            }

            long duration = System.currentTimeMillis() - startTime;
            log.info("CORS Cache initialized successfully in {}ms. Total allowed origins: {}",
                    duration, originCache.size());

            // Helpful for debugging: print all loaded origins if log level is trace
            if (log.isTraceEnabled()) {
                log.trace("Loaded Origins: {}", originCache);
            }

        } catch (Exception e) {
            log.error("CRITICAL: Failed to load initial CORS origins. All cross-origin requests might fail!", e);
        }
    }

    /**
     * Listens for the successful creation of a new OAuth2 client.
     * Updates the cache immediately without requiring a restart.
     *
     * @param event The event containing the new client's redirect URI.
     */
    @TransactionalEventListener
    public void handleClientRegistration(ClientRegisteredEvent event) {
        log.debug("Received ClientRegisteredEvent for URI: {}", event.redirectUri());

        boolean added = extractAndAddOrigin(event.redirectUri());

        if (added) {
            log.info("Dynamically added new CORS origin for: {}", event.redirectUri());
        } else {
            log.trace("URI was either invalid or origin already exists in cache: {}", event.redirectUri());
        }
    }

    /**
     * Checks if the incoming origin is allowed.
     * This method is called on every CORS request, so it must be fast.
     *
     * @param origin The 'Origin' header from the HTTP request (e.g., "http://localhost:3000").
     * @return true if allowed, false otherwise.
     */
    public boolean isAllowed(String origin) {
        boolean allowed = originCache.contains(origin);

        // Use TRACE level here because this will spam logs on every HTTP request
        if (log.isTraceEnabled()) {
            log.trace("CORS Check: Origin='{}' -> Allowed={}", origin, allowed);
        }

        return allowed;
    }

    /**
     * Parses a full URL to extract the Scheme, Host, and Port (the "Origin").
     * Example: "https://my-app.com/login/callback" -> "https://my-app.com"
     *
     * @param urlString The full redirect URI.
     * @return true if a new origin was added, false if it existed or was invalid.
     */
    private boolean extractAndAddOrigin(String urlString) {
        if (urlString == null || urlString.isBlank()) {
            log.trace("Skipping empty URI string.");
            return false;
        }

        try {
            URI uri = new URI(urlString);
            String scheme = uri.getScheme();
            String authority = uri.getAuthority(); // Returns "host:port"

            if (scheme != null && authority != null) {
                String origin = scheme + "://" + authority;

                boolean isNew = originCache.add(origin);
                if (isNew) {
                    log.debug("Parsed and cached new origin: {}", origin);
                }
                return isNew;
            } else {
                log.warn("Skipping invalid URI structure (missing scheme or authority): {}", urlString);
            }
        } catch (Exception e) {
            // Log at WARN because this implies 'redirect_uris' in DB has bad data
            log.warn("Failed to parse URI: '{}'. Error: {}", urlString, e.getMessage());
        }
        return false;
    }
}