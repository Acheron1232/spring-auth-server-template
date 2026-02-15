package com.acheron.gateway;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.reactive.server.WebTestClient;

@Import(TestcontainersConfiguration.class)
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("test")
class GatewaySecurityTest {

    @Autowired
    private WebTestClient webTestClient;

    @Test
    void unauthenticatedRequest_redirectsToLogin() {
        webTestClient.get().uri("/api/test")
                .exchange()
                .expectStatus().is3xxRedirection();
    }

    @Test
    void corsPreflightRequest_returnsHeaders() {
        webTestClient.options().uri("/api/test")
                .header("Origin", "http://localhost:5173")
                .header("Access-Control-Request-Method", "GET")
                .exchange()
                .expectHeader().valueEquals("Access-Control-Allow-Origin", "http://localhost:5173")
                .expectHeader().valueEquals("Access-Control-Allow-Credentials", "true");
    }

    @Test
    void corsPreflightRequest_unknownOrigin_rejected() {
        webTestClient.options().uri("/api/test")
                .header("Origin", "http://evil.com")
                .header("Access-Control-Request-Method", "GET")
                .exchange()
                .expectHeader().doesNotExist("Access-Control-Allow-Origin");
    }
}
