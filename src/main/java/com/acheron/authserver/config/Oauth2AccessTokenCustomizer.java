package com.acheron.authserver.config;

import com.acheron.authserver.entity.User;
import com.acheron.authserver.service.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.oauth2.core.AuthorizationGrantType;
import org.springframework.security.oauth2.core.oidc.endpoint.OidcParameterNames;
import org.springframework.security.oauth2.core.user.DefaultOAuth2User;
import org.springframework.security.oauth2.server.authorization.OAuth2TokenType;
import org.springframework.security.oauth2.server.authorization.token.JwtEncodingContext;
import org.springframework.security.oauth2.server.authorization.token.OAuth2TokenCustomizer;
import org.springframework.stereotype.Component;

@RequiredArgsConstructor
@Component
public class Oauth2AccessTokenCustomizer implements OAuth2TokenCustomizer<JwtEncodingContext> {

    private final UserService userService;

    @Override
    public void customize(JwtEncodingContext context) {
        if (!AuthorizationGrantType.AUTHORIZATION_CODE.equals(context.getAuthorizationGrantType())) {
            return;
        }

        if (OAuth2TokenType.ACCESS_TOKEN.equals(context.getTokenType())
                || OidcParameterNames.ID_TOKEN.equals(context.getTokenType().getValue())) {
            context.getClaims().claims(claims -> {
                Object principal = context.getPrincipal().getPrincipal();
                User user;
                if (principal instanceof UserDetails userDetails) {
                    user = (User) userService.loadUserByUsername(userDetails.getUsername());
                } else if (principal instanceof DefaultOAuth2User oAuth2User) {
                    String username = oAuth2User.getAttribute("login") != null
                            ? oAuth2User.getAttribute("login")
                            : oAuth2User.getAttribute("name");
                    user = (User) userService.loadUserByUsername(username);
                } else {
                    return;
                }
                claims.put("user_id", user.getId().toString());
                claims.put("roles", user.getRole().name());
                claims.put("token_version", user.getTokenVersion().toString());
            });
        }
    }
}
