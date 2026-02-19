package com.acheron.authserver.service;

import com.acheron.authserver.entity.AuthHistory;
import com.acheron.authserver.entity.User;
import com.acheron.authserver.repository.AuthHistoryRepository;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
public class AuthHistoryService {
    private final AuthHistoryRepository authHistoryRepository;

    public void recordLogin(User user, HttpServletRequest request, String loginMethod) {
        String ip = resolveClientIp(request);
        String ua = request.getHeader("User-Agent");
        if (ua == null || ua.isBlank()) ua = "unknown";

        AuthHistory history = AuthHistory.builder()
                .user(user)
                .ipAddress(ip)
                .userAgent(ua.length() > 512 ? ua.substring(0, 512) : ua)
                .timestamp(LocalDateTime.now())
                .location("-")
                .loginMethod(loginMethod)
                .build();
        authHistoryRepository.save(history);
    }

    private static String resolveClientIp(HttpServletRequest request) {
        String forwarded = request.getHeader("X-Forwarded-For");
        if (forwarded != null && !forwarded.isBlank()) {
            String first = forwarded.split(",")[0].trim();
            if (!first.isBlank()) return first;
        }
        String realIp = request.getHeader("X-Real-IP");
        if (realIp != null && !realIp.isBlank()) {
            return realIp.trim();
        }
        return request.getRemoteAddr();
    }
}
