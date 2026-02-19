package com.acheron.authserver.config;

import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.oauth2.core.OAuth2AuthenticationException;
import org.springframework.security.oauth2.core.OAuth2Error;
import org.springframework.security.oauth2.core.OAuth2ErrorCodes;
import org.springframework.security.oauth2.core.endpoint.OAuth2ParameterNames;
import org.springframework.security.oauth2.server.authorization.OAuth2Authorization;
import org.springframework.security.oauth2.server.authorization.OAuth2AuthorizationService;
import org.springframework.security.oauth2.server.authorization.OAuth2TokenType;
import org.springframework.security.web.authentication.AuthenticationFailureHandler;

import java.io.IOException;
import java.nio.charset.StandardCharsets;

@Slf4j
@RequiredArgsConstructor
public class RefreshTokenReuseDetectionErrorHandler implements AuthenticationFailureHandler {

    private final OAuth2AuthorizationService authorizationService;

    @Override
    public void onAuthenticationFailure(HttpServletRequest request, HttpServletResponse response,
                                        AuthenticationException exception) throws IOException, ServletException {

        try {
            String grantType = request.getParameter(OAuth2ParameterNames.GRANT_TYPE);
            boolean isRefresh = OAuth2ParameterNames.REFRESH_TOKEN.equals(grantType);

            if (isRefresh && exception instanceof OAuth2AuthenticationException oauth2ex) {
                if (OAuth2ErrorCodes.INVALID_GRANT.equals(oauth2ex.getError().getErrorCode())) {
                    String refreshToken = request.getParameter(OAuth2ParameterNames.REFRESH_TOKEN);
                    if (refreshToken != null && !refreshToken.isBlank()) {
                        OAuth2Authorization authorization = authorizationService.findByToken(refreshToken, OAuth2TokenType.REFRESH_TOKEN);
                        if (authorization != null) {
                            log.warn("Refresh token reuse suspected. Revoking authorization id={} principal={}", authorization.getId(), authorization.getPrincipalName());
                            authorizationService.remove(authorization);
                        }
                    }
                }
            }
        } catch (Exception e) {
            log.warn("Failed during refresh token reuse detection", e);
        }

        response.setHeader("Cache-Control", "no-store");
        response.setHeader("Pragma", "no-cache");

        OAuth2Error error;
        if (exception instanceof OAuth2AuthenticationException oauth2ex) {
            error = oauth2ex.getError();
        } else {
            error = new OAuth2Error(OAuth2ErrorCodes.SERVER_ERROR);
        }

        response.setStatus(HttpStatus.BAD_REQUEST.value());
        response.setCharacterEncoding(StandardCharsets.UTF_8.name());
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        response.getWriter().write(toJson(error));
    }

    private static String toJson(OAuth2Error error) {
        String code = escape(error.getErrorCode());
        String desc = error.getDescription() != null ? escape(error.getDescription()) : null;

        if (desc == null || desc.isBlank()) {
            return "{\"error\":\"" + code + "\"}";
        }
        return "{\"error\":\"" + code + "\",\"error_description\":\"" + desc + "\"}";
    }

    private static String escape(String value) {
        return value.replace("\\", "\\\\").replace("\"", "\\\"");
    }
}
