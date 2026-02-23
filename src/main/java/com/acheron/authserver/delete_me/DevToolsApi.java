package com.acheron.authserver.delete_me;

import com.acheron.authserver.config.DynamicCorsConfigurationSource;
import lombok.RequiredArgsConstructor;
import lombok.SneakyThrows;
import org.springframework.context.annotation.Profile;
import org.springframework.security.oauth2.jwt.JwtClaimsSet;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.JwtEncoderParameters;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.security.MessageDigest;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.*;

@RestController
@RequiredArgsConstructor
@Profile("dev")
public class DevToolsApi {

    private final DynamicCorsConfigurationSource corsConfigurationSource;
    private final JwtEncoder jwtEncoder;

    @PostMapping("/dev/token")
    public String generateLongLivedToken(@RequestParam String username) {
        
        Instant now = Instant.now();

        JwtClaimsSet claims = JwtClaimsSet.builder()
                .issuer("http://localhost:9000")
                .issuedAt(now)
                .expiresAt(now.plus(30, ChronoUnit.DAYS))
                .subject(username)
                .claim("scope", List.of("openid", "profile", "message.read"))
                .claim("authorities", List.of("ROLE_USER", "ROLE_ADMIN"))
                .build();

        return jwtEncoder.encode(JwtEncoderParameters.from(claims)).getTokenValue();
    }

    @GetMapping("/dev")
    public Map<String,String> dev() {
        Map<String, String> data = new HashMap<>();

        // Generate a JWT
        Instant now = Instant.now();
        JwtClaimsSet claims = JwtClaimsSet.builder()
                .issuer("http://localhost:9000")
                .issuedAt(now)
                .expiresAt(now.plus(30, ChronoUnit.DAYS))
                .subject("username")
                .claim("scope", List.of("openid", "profile", "message.read"))
                .claim("authorities", List.of("ROLE_USER", "ROLE_ADMIN"))
                .build();

        String token = jwtEncoder.encode(JwtEncoderParameters.from(claims)).getTokenValue();
        data.put("token", token);
        //end

        // cors
        data.put("cors", corsConfigurationSource.toString());

        return data;
    }

    @SneakyThrows
    @GetMapping("/dev/t")
    public void v(){
        String codeVerifier = UUID.randomUUID().toString().replace("-", "")
                + UUID.randomUUID().toString().replace("-", "");

        MessageDigest md = MessageDigest.getInstance("SHA-256");
        byte[] digest = md.digest(codeVerifier.getBytes("US-ASCII"));

        String codeChallenge = Base64.getUrlEncoder().withoutPadding().encodeToString(digest);

        System.out.println( codeVerifier);
        System.out.println(codeChallenge);
    }
}