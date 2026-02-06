package com.acheron.authserver.service.auth_server;

import com.acheron.authserver.dto.util.ClientRegisteredEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.security.oauth2.server.authorization.client.RegisteredClient;
import org.springframework.security.oauth2.server.authorization.client.RegisteredClientRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.net.URI;

@Service
@RequiredArgsConstructor
@Slf4j
public class ClientService {
    private final RegisteredClientRepository clientRepository;

    private final ApplicationEventPublisher eventPublisher;

    @Transactional
    public void registerNewClient(RegisteredClient registeredClient) {

        clientRepository.save(registeredClient);

        String url = registeredClient.getRedirectUris().stream().findFirst().orElseThrow();

        eventPublisher.publishEvent(new ClientRegisteredEvent(url));
        log.info("Saved new client: {}", registeredClient.getClientId());

        String origin = extractClientDomain(url);

        if (origin != null) {
            eventPublisher.publishEvent(new ClientRegisteredEvent(origin));
            log.info("Published CORS event for origin: {}", origin);
        }
    }

    private String extractClientDomain(String urlString) {
        try {
            if (urlString == null || urlString.isBlank()) {
                return null;
            }
            URI uri = new URI(urlString);

            String scheme = uri.getScheme();
            String authority = uri.getAuthority();

            if (scheme == null || authority == null) {
                return null;
            }

            return scheme + "://" + authority;

        } catch (Exception e) {
            log.warn("Could not extract domain from URL: {}. Reason: {}", urlString, e.getMessage());
            return null;
        }
    }
}
