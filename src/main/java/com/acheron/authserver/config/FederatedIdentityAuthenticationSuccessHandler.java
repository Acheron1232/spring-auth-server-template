package com.acheron.authserver.config;

import com.acheron.authserver.service.UserService;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.client.authentication.OAuth2AuthenticationToken;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.security.web.authentication.AuthenticationSuccessHandler;
import org.springframework.security.web.authentication.SavedRequestAwareAuthenticationSuccessHandler;
import org.springframework.stereotype.Component;

import java.io.IOException;

/**
 * An {@link AuthenticationSuccessHandler} implementation for handling successful OAuth2/OIDC logins.
 * <p>
 * This handler is responsible for:
 * <ol>
 * <li><strong>JIT Provisioning:</strong> Persisting or updating the federated user in the local database
 * via the {@link UserService} before the session is fully established.</li>
 * <li><strong>Redirection:</strong> Delegating the final redirect logic to
 * {@link SavedRequestAwareAuthenticationSuccessHandler} to ensure the user is returned to the
 * originally requested resource.</li>
 * </ol>
 * <p>
 * Key advantages of this implementation:
 * <ul>
 * <li>Seamless support for both OIDC ({@code OidcUser}) and standard OAuth2 ({@code OAuth2User}) principals.</li>
 * <li>Adheres to Spring Security best practices for federated account linking.</li>
 * <li>Ensures a smooth user experience by preserving the original request context.</li>
 * </ul>
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class FederatedIdentityAuthenticationSuccessHandler implements AuthenticationSuccessHandler {

    private final AuthenticationSuccessHandler delegate = new SavedRequestAwareAuthenticationSuccessHandler();
    private final UserService userService;

    /**
     * Called when a user has been successfully authenticated.
     * <p>
     * This method intercepts the authentication flow to extract user details from the
     * {@link OAuth2AuthenticationToken} and synchronize them with the local system.
     * After processing, it delegates the response handling to the standard
     * {@link SavedRequestAwareAuthenticationSuccessHandler}.
     *
     * @param request        the request which caused the successful authentication
     * @param response       the response
     * @param authentication the <tt>Authentication</tt> object which was created during the authentication process.
     * @throws IOException      if an input or output exception occurs
     * @throws ServletException if a servlet exception occurs
     */
    @Override
    public void onAuthenticationSuccess(HttpServletRequest request, HttpServletResponse response,
                                        Authentication authentication) throws IOException, ServletException {

        if (authentication instanceof OAuth2AuthenticationToken oauthToken) {
            String registrationId = oauthToken.getAuthorizedClientRegistrationId();
            OAuth2User principal = oauthToken.getPrincipal();

            log.debug("Processing user from provider: {}", registrationId);

            try {
                // Perform JIT provisioning or update existing user
                this.userService.saveOauthUser(registrationId, principal);
            } catch (Exception e) {
                // Log the error but allow the login to proceed (or handle as a fatal error depending on requirements)
                log.error("Error saving OAuth2 user", e);
            }
        }

        // Delegate to the default handler to manage the redirect
        this.delegate.onAuthenticationSuccess(request, response, authentication);
    }
}