package com.acheron.authserver.config;

import com.acheron.authserver.dto.ProfileCreationDTO;
import com.acheron.authserver.dto.UserCreateDto;
import com.acheron.authserver.dto.UserCreationDto;
import com.acheron.authserver.entity.Role;
import com.acheron.authserver.service.UserService;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.client.OAuth2AuthorizedClient;
import org.springframework.security.oauth2.client.OAuth2AuthorizedClientService;
import org.springframework.security.oauth2.client.authentication.OAuth2AuthenticationToken;
import org.springframework.security.oauth2.core.user.DefaultOAuth2User;
import org.springframework.security.web.authentication.SavedRequestAwareAuthenticationSuccessHandler;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Component
@Slf4j
public class OAuth2LoginSuccessHandler extends SavedRequestAwareAuthenticationSuccessHandler {

    private final OAuth2AuthorizedClientService authorizedClientService;
    private final UserService userService;

    // Default redirect for direct OAuth2 login (not from Gateway)
    private static final String DEFAULT_TARGET_URL = "http://localhost:5173/";

    public OAuth2LoginSuccessHandler(OAuth2AuthorizedClientService authorizedClientService, UserService userService) {
        setDefaultTargetUrl(DEFAULT_TARGET_URL);
        this.authorizedClientService = authorizedClientService;
        this.userService = userService;
    }

    @Override
    public void onAuthenticationSuccess(HttpServletRequest request,
                                        HttpServletResponse response,
                                        Authentication authentication) throws ServletException, IOException {

        if (authentication instanceof OAuth2AuthenticationToken oAuth2Token) {
            String provider = oAuth2Token.getAuthorizedClientRegistrationId();
            try {
                // 1. Отримуємо дані користувача (GitHub/Google)
                DefaultOAuth2User principal = (DefaultOAuth2User) authentication.getPrincipal();
                Map<String, Object> attributes = new HashMap<>(principal.getAttributes());

                String email = extractEmail(provider, attributes, authentication);
                String avatar = extractAvatar(provider, attributes);

                // 2. Створюємо юзера в БД, якщо немає
                ensureUserExists(email, attributes, provider, avatar);

                log.info("OAuth2 Login success for email: {}", email);

            } catch (Exception e) {
                log.error("Error processing OAuth2 user", e);
                getRedirectStrategy().sendRedirect(request, response, "/login?error");
                return;
            }
        }

        super.onAuthenticationSuccess(request, response, authentication);
    }

    /**
     * Extract email from OAuth2 provider attributes
     */
    private String extractEmail(String provider, Map<String, Object> attributes, Authentication authentication)
            throws JsonProcessingException {

        if ("github".equals(provider)) {
            Object emailAttr = attributes.get("email");
            if (emailAttr != null && !emailAttr.toString().isEmpty()) {
                return emailAttr.toString();
            }
            // GitHub sometimes doesn't include email in user info - fetch separately
            return getPrimaryEmailForGitHub(authentication);

        } else { // google
            Object emailAttr = attributes.get("email");
            if (emailAttr == null || emailAttr.toString().isEmpty()) {
                throw new IllegalStateException("Email not found in Google attributes");
            }
            return emailAttr.toString();
        }
    }

    /**
     * Extract avatar URL from provider attributes
     */
    private String extractAvatar(String provider, Map<String, Object> attributes) {
        if ("github".equals(provider)) {
            Object avatarUrl = attributes.get("avatar_url");
            return avatarUrl != null ? avatarUrl.toString() : null;
        } else { // google
            Object picture = attributes.get("picture");
            return picture != null ? picture.toString() : null;
        }
    }

    /**
     * Create user account if doesn't exist
     */
    private void ensureUserExists(String email, Map<String, Object> attributes, String provider, String avatar) {
        if (!userService.existsByEmail(email)) {
            String username = extractUsername(attributes);

            UserCreationDto newUser = new UserCreationDto(
                    username,
                    email,
                    null, // password - null for OAuth2 users
                    true, // emailConfirmed
                    Role.USER.toString(),
                    "github".equals(provider) ? "GITHUB" : "GOOGLE",
                    false, // mfaEnabled
                    null  // mfaSecret
            );

            ProfileCreationDTO profile = new ProfileCreationDTO(
                    null, // id
                    username, // firstName
                    username, // lastName - you may want to parse this properly
                    avatar
            );

            UserCreateDto dto = new UserCreateDto(profile, newUser);
            userService.saveOauthUser(dto);

            log.info("Created new user from {} OAuth2: {}", provider, email);
        }
    }

    /**
     * Extract username from provider attributes
     */
    private String extractUsername(Map<String, Object> attributes) {
        Object name = attributes.get("name");
        if (name != null && !name.toString().isEmpty()) {
            return name.toString();
        }

        // Fallback to login/email
        Object login = attributes.get("login");
        if (login != null) {
            return login.toString();
        }

        Object email = attributes.get("email");
        if (email != null) {
            return email.toString().split("@")[0];
        }

        return "user_" + System.currentTimeMillis();
    }

    /**
     * Fetch primary email from GitHub API
     */
    private String getPrimaryEmailForGitHub(Authentication authentication) throws JsonProcessingException {
        OAuth2AuthorizedClient client = authorizedClientService.loadAuthorizedClient(
                ((OAuth2AuthenticationToken) authentication).getAuthorizedClientRegistrationId(),
                authentication.getName()
        );

        if (client == null) {
            throw new IllegalStateException("OAuth2AuthorizedClient not found for GitHub");
        }

        String accessToken = client.getAccessToken().getTokenValue();

        RestTemplate restTemplate = new RestTemplate();
        HttpHeaders headers = new HttpHeaders();
        headers.add("Authorization", "Bearer " + accessToken);

        HttpEntity<String> entity = new HttpEntity<>(headers);
        String url = "https://api.github.com/user/emails";

        ResponseEntity<String> response = restTemplate.exchange(url, HttpMethod.GET, entity, String.class);
        String json = response.getBody();

        if (json == null) {
            throw new IllegalStateException("GitHub API returned empty response");
        }

        ObjectMapper objectMapper = new ObjectMapper();
        List<Map<String, Object>> emails = objectMapper.readValue(json, new TypeReference<>() {
        });

        return emails.stream()
                .filter(email -> Boolean.TRUE.equals(email.get("primary")))
                .map(email -> (String) email.get("email"))
                .findFirst()
                .orElseThrow(() -> new IllegalStateException("No primary email found in GitHub account"));
    }
}