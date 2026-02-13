package com.acheron.authserver.config;

import com.acheron.authserver.service.UserService;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.client.authentication.OAuth2AuthenticationToken;
import org.springframework.security.oauth2.core.oidc.user.OidcUser;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.security.web.authentication.AuthenticationSuccessHandler;
import org.springframework.security.web.authentication.SavedRequestAwareAuthenticationSuccessHandler;
import org.springframework.stereotype.Component;

import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.function.Consumer;

/**
 * FederatedIdentityAuthenticationSuccessHandler - стандартний Spring Security handler для OAuth2 логіну
 * 
 * Переваги порівняно з кастомною реалізацією:
 * 1. Використовує SavedRequestAwareAuthenticationSuccessHandler для коректного редиректу
 * 2. Підтримує як OIDC (OidcUser) так і OAuth2 (OAuth2User) користувачів
 * 3. Дозволяє гнучко налаштовувати обробку користувачів через Consumer
 * 4. Стандартний підхід рекомендований Spring Security
 * 5. Легко розширюється для JIT provisioning або federated account linking
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class FederatedIdentityAuthenticationSuccessHandler implements AuthenticationSuccessHandler {

    private final AuthenticationSuccessHandler delegate = new SavedRequestAwareAuthenticationSuccessHandler();
    private final  UserService userService;

    @Override
    public void onAuthenticationSuccess(HttpServletRequest request, HttpServletResponse response, 
                                        Authentication authentication) throws IOException, ServletException {

        if (authentication instanceof OAuth2AuthenticationToken oauthToken) {
            String registrationId = oauthToken.getAuthorizedClientRegistrationId();

            OAuth2User principal = oauthToken.getPrincipal();

            log.debug("Processing user from provider: {}", registrationId);

            try {
                this.userService.saveOauthUser(registrationId, principal);
            } catch (Exception e) {
                log.error("Error saving OAuth2 user", e);
            }
        }
        
        this.delegate.onAuthenticationSuccess(request, response, authentication);
    }
}
