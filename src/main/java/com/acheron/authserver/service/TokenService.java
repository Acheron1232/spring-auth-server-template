package com.acheron.authserver.service;

import com.acheron.authserver.entity.Token;
import com.acheron.authserver.entity.User;
import com.acheron.authserver.repository.TokenRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

@Service
@Slf4j
@RequiredArgsConstructor
public class TokenService {
    private final TokenRepository tokenRepository;

    public Optional<Token> getToken(String token) {
        return tokenRepository.findByToken(token);
    }

    public Token generateToken(User user, Token.TokenType tokenType) {
        Token token = new Token(null, UUID.randomUUID().toString(), user, Instant.now().plus(Duration.ofHours(24)), Token.TokenStatus.ACTIVE, tokenType);
        log.info("Generating {} token for {}", tokenType.name().toLowerCase(), user.getUsername());
        return tokenRepository.save(token);
    }

    public void delete(Token token) {
        tokenRepository.delete(token);
    }
}
