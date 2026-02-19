package com.acheron.authserver.config;

import com.acheron.authserver.entity.User;
import com.acheron.authserver.service.AuthHistoryService;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.web.authentication.AuthenticationSuccessHandler;
import org.springframework.security.web.authentication.SavedRequestAwareAuthenticationSuccessHandler;
import org.springframework.stereotype.Component;

import java.io.IOException;

@Slf4j
@Component
@RequiredArgsConstructor
public class LoggingAuthenticationSuccessHandler implements AuthenticationSuccessHandler {

    private final AuthenticationSuccessHandler delegate = new SavedRequestAwareAuthenticationSuccessHandler();
    private final AuthHistoryService authHistoryService;

    @Override
    public void onAuthenticationSuccess(HttpServletRequest request, HttpServletResponse response,
                                        Authentication authentication) throws IOException, ServletException {
        try {
            Object principal = authentication.getPrincipal();
            if (principal instanceof User user) {
                authHistoryService.recordLogin(user, request, "FORM");
            } else if (principal instanceof UserDetails userDetails) {
                log.debug("Form login principal is UserDetails but not User entity: {}", userDetails.getUsername());
            }
        } catch (Exception e) {
            log.warn("Failed to record login history", e);
        }
        delegate.onAuthenticationSuccess(request, response, authentication);
    }
}
