package com.acheron.authserver.config.oauth_provider_handlers;

import com.acheron.authserver.dto.util.UnifiedUserDto;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.security.oauth2.client.OAuth2AuthorizedClient;
import org.springframework.security.oauth2.client.OAuth2AuthorizedClientService;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;
import tools.jackson.core.type.TypeReference;
import tools.jackson.databind.json.JsonMapper;

import java.util.List;
import java.util.Map;

@Slf4j
@Component
@RequiredArgsConstructor
public class GitHubHandler implements OAuth2UserHandler {

    private final OAuth2AuthorizedClientService authorizedClientService;
    private final JsonMapper objectMapper = new JsonMapper();
    private final RestTemplate restTemplate = new RestTemplate();

    @Override
    public boolean supports(String registrationId) {
        return "github".equalsIgnoreCase(registrationId);
    }

    @Override
    public UnifiedUserDto extract(String registrationId, OAuth2User user) {
        Map<String, Object> attributes = user.getAttributes();

        String login = (String) attributes.get("login");
        String name = (String) attributes.get("name");
        String avatarUrl = (String) attributes.get("avatar_url");
        Integer id = (Integer) attributes.get("id");

        String email = (String) attributes.get("email");

        if (email == null || email.isBlank()) {
            try {
                email = getPrimaryEmailForGitHub(registrationId, user.getName());
            } catch (Exception e) {
                log.error("Failed to fetch GitHub email for user {}", login, e);

                throw new RuntimeException("Could not fetch email from GitHub", e);
            }
        }

        return UnifiedUserDto.builder()
                .providerId(registrationId)
                .providerUserId(String.valueOf(id))
                .email(email)
                .firstName(name != null ? name : login)
                .lastName(null)
                .imageUrl(avatarUrl)
                .build();
    }


    private String getPrimaryEmailForGitHub(String registrationId, String principalName) {
        OAuth2AuthorizedClient client = authorizedClientService.loadAuthorizedClient(
                registrationId,
                principalName
        );

        if (client == null) {
            throw new IllegalStateException("OAuth2AuthorizedClient not found for GitHub user: " + principalName);
        }

        String accessToken = client.getAccessToken().getTokenValue();

        HttpHeaders headers = new HttpHeaders();
        headers.add("Authorization", "Bearer " + accessToken);
        HttpEntity<String> entity = new HttpEntity<>(headers);

        String url = "https://api.github.com/user/emails";

        try {
            ResponseEntity<String> response = restTemplate.exchange(url, HttpMethod.GET, entity, String.class);
            String json = response.getBody();

            if (json == null) return null;

            List<Map<String, Object>> emails = objectMapper.readValue(json, new TypeReference<>() {});

            return emails.stream()
                    .filter(entry -> Boolean.TRUE.equals(entry.get("primary")))
                    .map(entry -> (String) entry.get("email"))
                    .findFirst()
                    .orElseThrow(() -> new IllegalStateException("No primary email found in GitHub account"));

        } catch (Exception e) {
            throw new RuntimeException("Error communicating with GitHub API", e);
        }
    }
}