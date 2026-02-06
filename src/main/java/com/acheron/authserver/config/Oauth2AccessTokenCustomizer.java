package com.acheron.authserver.config;

import com.acheron.authserver.service.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.oauth2.core.AuthorizationGrantType;
import org.springframework.security.oauth2.server.authorization.token.JwtEncodingContext;
import org.springframework.security.oauth2.server.authorization.token.OAuth2TokenCustomizer;
import org.springframework.stereotype.Component;

@RequiredArgsConstructor
@Component
public class Oauth2AccessTokenCustomizer implements OAuth2TokenCustomizer<JwtEncodingContext> {

    private final UserService userService;

    @Override
    public void customize(JwtEncodingContext context) {
        if (!context.getAuthorizationGrantType().getValue().equals(AuthorizationGrantType.AUTHORIZATION_CODE.getValue())) {
            return;
        }
//        if (OAuth2TokenType.ACCESS_TOKEN.equals(context.getTokenType())) {
//            context.getClaims().claims(claims -> {
//                Object principal = context.getPrincipal().getPrincipal();
//                UserDto user;
//                if (principal instanceof UserDetails) {
//                    user = userService.findByUsername(((UserDetails) principal).getUsername());
//                } else if (principal instanceof DefaultOAuth2User oidcUser) {
//                    user = userService.findByUsername(
//                            oidcUser.getAttribute("login") == null
//                                    ? oidcUser.getAttribute("name")
//                                    : oidcUser.getAttribute("login"));
//                } else {
//                    return;
//                }
//                claims.put("id", user.getId());
//                claims.put("roles", user.getRole());
//                claims.put("name", user.getUsername());
//            });
//        }
    }
}