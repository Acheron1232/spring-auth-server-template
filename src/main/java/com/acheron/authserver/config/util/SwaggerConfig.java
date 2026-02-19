package com.acheron.authserver.config.util;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.ExternalDocumentation;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.info.License;
import io.swagger.v3.oas.models.security.OAuthFlow;
import io.swagger.v3.oas.models.security.OAuthFlows;
import io.swagger.v3.oas.models.security.Scopes;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;
import io.swagger.v3.oas.models.servers.Server;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.List;

@Configuration
public class SwaggerConfig {

    @Value("${server.port:9000}")
    private String serverPort;

    @Value("${spring.application.name:Auth Server}")
    private String applicationName;

    @Bean
    public OpenAPI customOpenAPI() {
        final String bearerScheme = "bearerAuth";
        final String oauth2Scheme = "oauth2";
        String serverUrl = "http://localhost:" + serverPort;

        return new OpenAPI()
                .addSecurityItem(new SecurityRequirement().addList(bearerScheme))
                .components(new Components()
                        .addSecuritySchemes(bearerScheme,
                                new SecurityScheme()
                                        .name(bearerScheme)
                                        .type(SecurityScheme.Type.HTTP)
                                        .scheme("bearer")
                                        .bearerFormat("JWT")
                                        .description("Paste your JWT access token here."))
                        .addSecuritySchemes(oauth2Scheme,
                                new SecurityScheme()
                                        .name(oauth2Scheme)
                                        .type(SecurityScheme.Type.OAUTH2)
                                        .flows(new OAuthFlows()
                                                .authorizationCode(new OAuthFlow()
                                                        .authorizationUrl(serverUrl + "/oauth2/authorize")
                                                        .tokenUrl(serverUrl + "/oauth2/token")
                                                        .scopes(new Scopes()
                                                                .addString("openid", "OpenID Connect")
                                                                .addString("profile", "User profile")
                                                                .addString("email", "User email")
                                                                .addString("message.read", "Read access")))))
                )
                .servers(List.of(
                        new Server().url(serverUrl).description("Local Development")
                ))
                .info(new Info()
                        .title(applicationName + " API")
                        .version("1.0.0")
                        .description("""
                                ### OAuth2 & OpenID Connect Authorization Server

                                Production-ready Authorization Server based on Spring Security OAuth2.

                                **Key Features:**
                                * OAuth 2.1 (Authorization Code, Refresh Token, Client Credentials)
                                * OIDC Compliant (UserInfo, ID Token, JWKS)
                                * MFA / TOTP (Google Authenticator)
                                * Refresh token reuse detection with token versioning
                                * Dynamic CORS from registered client redirect URIs
                                * Redis session store
                                * Federated identity (Google, GitHub)

                                **How to test:**
                                1. Obtain an access token via `POST /oauth2/token`.
                                2. Click **Authorize** and paste the token.
                                """)
                        .contact(new Contact()
                                .name("Acheron")
                                .email("aryemfedorov@gmail.com")
                                .url("https://github.com/Acheron1232/spring-auth-server-template"))
                        .license(new License()
                                .name("Apache 2.0")
                                .url("https://www.apache.org/licenses/LICENSE-2.0.html")))
                .externalDocs(new ExternalDocumentation()
                        .description("Spring Authorization Server Docs")
                        .url("https://docs.spring.io/spring-authorization-server/reference/"));
    }
}