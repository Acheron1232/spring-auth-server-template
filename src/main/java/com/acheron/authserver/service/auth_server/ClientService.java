package com.acheron.authserver.service.auth_server;

import com.acheron.authserver.dto.request.ClientRegistrationRequest;
import com.acheron.authserver.dto.util.ClientRegisteredEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.oauth2.core.AuthorizationGrantType;
import org.springframework.security.oauth2.core.ClientAuthenticationMethod;
import org.springframework.security.oauth2.server.authorization.client.RegisteredClient;
import org.springframework.security.oauth2.server.authorization.client.RegisteredClientRepository;
import org.springframework.security.oauth2.server.authorization.settings.ClientSettings;
import org.springframework.security.oauth2.server.authorization.settings.TokenSettings;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.net.URI;
import java.time.Duration;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class ClientService {
    private final RegisteredClientRepository clientRepository;
    private final ApplicationEventPublisher eventPublisher;
    private final PasswordEncoder passwordEncoder;

    @Transactional
    public void registerNewClient(RegisteredClient registeredClient) {
        clientRepository.save(registeredClient);

        registeredClient.getRedirectUris().forEach(url -> {
            eventPublisher.publishEvent(new ClientRegisteredEvent(url));
            String origin = extractClientDomain(url);
            if (origin != null) {
                eventPublisher.publishEvent(new ClientRegisteredEvent(origin));
                log.info("Published CORS event for origin: {}", origin);
            }
        });

        log.info("Saved new client: {}", registeredClient.getClientId());
    }

    @Transactional
    public void registerNewClientFromRequest(ClientRegistrationRequest request) {
        boolean isPublic = request.clientSecret() == null || request.clientSecret().isBlank();

        RegisteredClient.Builder builder = RegisteredClient.withId(UUID.randomUUID().toString())
                .clientId(request.clientId())
                .clientAuthenticationMethod(isPublic
                        ? ClientAuthenticationMethod.NONE
                        : ClientAuthenticationMethod.CLIENT_SECRET_BASIC)
                .authorizationGrantType(AuthorizationGrantType.AUTHORIZATION_CODE)
                .tokenSettings(TokenSettings.builder()
                        .accessTokenTimeToLive(Duration.ofMinutes(
                                request.accessTokenTtlMinutes() > 0 ? request.accessTokenTtlMinutes() : 5))
                        .refreshTokenTimeToLive(Duration.ofDays(
                                request.refreshTokenTtlDays() > 0 ? request.refreshTokenTtlDays() : 20))
                        .reuseRefreshTokens(false)
                        .build())
                .clientSettings(ClientSettings.builder()
                        .requireProofKey(request.requirePkce())
                        .requireAuthorizationConsent(request.requireConsent())
                        .build());

        if (!isPublic) {
            builder.clientSecret(passwordEncoder.encode(request.clientSecret()));
            builder.authorizationGrantType(AuthorizationGrantType.REFRESH_TOKEN);
        }

        request.redirectUris().forEach(builder::redirectUri);
        request.scopes().forEach(builder::scope);

        registerNewClient(builder.build());
    }

    @Transactional
    public void deleteClient(String clientId) {
        RegisteredClient client = clientRepository.findByClientId(clientId);
        if (client != null) {
            clientRepository.save(RegisteredClient.from(client).build());
            log.info("Deleted client: {}", clientId);
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