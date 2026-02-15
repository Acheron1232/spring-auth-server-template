package com.acheron.authserver.config;

import lombok.extern.slf4j.Slf4j;
import org.springframework.security.authentication.AuthenticationProvider;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.oauth2.core.ClientAuthenticationMethod;
import org.springframework.security.oauth2.core.OAuth2AuthenticationException;
import org.springframework.security.oauth2.core.OAuth2Error;
import org.springframework.security.oauth2.core.OAuth2ErrorCodes;
import org.springframework.security.oauth2.core.endpoint.OAuth2ParameterNames;
import org.springframework.security.oauth2.server.authorization.OAuth2AuthorizationService;
import org.springframework.security.oauth2.server.authorization.authentication.OAuth2ClientAuthenticationToken;
import org.springframework.security.oauth2.server.authorization.client.RegisteredClient;
import org.springframework.security.oauth2.server.authorization.client.RegisteredClientRepository;
import org.springframework.util.Assert;

import java.util.Set;

@Slf4j
public final class PublicClientRefreshTokenAuthenticationProvider implements AuthenticationProvider {

    private static final String ERROR_URI = "https://datatracker.ietf.org/doc/html/rfc6749#section-3.2.1";

    private static final Set<String> REFRESH_TOKEN_ALLOWED_SUFFIXES = Set.of("_mobile", "_test", "_with_refresh");

    private final RegisteredClientRepository registeredClientRepository;

    public PublicClientRefreshTokenAuthenticationProvider(
            RegisteredClientRepository registeredClientRepository,
            OAuth2AuthorizationService authorizationService) {
        Assert.notNull(registeredClientRepository, "registeredClientRepository cannot be null");
        Assert.notNull(authorizationService, "authorizationService cannot be null");
        this.registeredClientRepository = registeredClientRepository;
    }

    @Override
    public Authentication authenticate(Authentication authentication) throws AuthenticationException {

        OAuth2ClientAuthenticationToken clientAuthentication = (OAuth2ClientAuthenticationToken) authentication;

        if (!ClientAuthenticationMethod.NONE.equals(clientAuthentication.getClientAuthenticationMethod())) {
            return null;
        }

        String clientId = clientAuthentication.getPrincipal().toString();
        RegisteredClient registeredClient = this.registeredClientRepository.findByClientId(clientId);
        if (registeredClient == null) {
            throwInvalidClient(OAuth2ParameterNames.CLIENT_ID);
        }

        log.trace("Retrieved registered client");

        if (!registeredClient
                .getClientAuthenticationMethods()
                .contains(clientAuthentication
                        .getClientAuthenticationMethod())) {
            throwInvalidClient("authentication_method");
        }

        if (!isRefreshTokenAllowed(clientId)) {
            log.debug("Public client '{}' is not allowed to use refresh tokens. " +
                    "Client ID must end with one of: {}", clientId, REFRESH_TOKEN_ALLOWED_SUFFIXES);
            throwInvalidClient("refresh_token_not_allowed");
        }

        log.trace("Validated client authentication parameters");
        log.trace("Authenticated public client with refresh token capability");

        return new OAuth2ClientAuthenticationToken(
                registeredClient,
                clientAuthentication.getClientAuthenticationMethod(),
                null);
    }

    @Override
    public boolean supports(Class<?> authentication) {
        return OAuth2ClientAuthenticationToken.class.isAssignableFrom(authentication);
    }

    private static boolean isRefreshTokenAllowed(String clientId) {
        if (clientId == null) return false;
        String lowerClientId = clientId.toLowerCase();
        return REFRESH_TOKEN_ALLOWED_SUFFIXES.stream().anyMatch(lowerClientId::endsWith);
    }

    private static void throwInvalidClient(String parameterName) {
        OAuth2Error error = new OAuth2Error(
                OAuth2ErrorCodes.INVALID_CLIENT,
                "Client authentication failed: " + parameterName, ERROR_URI);
        throw new OAuth2AuthenticationException(error);
    }
}
