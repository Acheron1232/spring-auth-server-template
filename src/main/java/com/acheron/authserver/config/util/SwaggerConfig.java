package com.acheron.authserver.config.util;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.ExternalDocumentation;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.info.License;
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
        final String securitySchemeName = "bearerAuth";

        return new OpenAPI()
                .addSecurityItem(new SecurityRequirement().addList(securitySchemeName))
                .components(new Components()
                        .addSecuritySchemes(securitySchemeName,
                                new SecurityScheme()
                                        .name(securitySchemeName)
                                        .type(SecurityScheme.Type.HTTP)
                                        .scheme("bearer")
                                        .bearerFormat("JWT")
                                        .description("Enter your JWT Access Token here to access protected endpoints.")
                        )
                )
                .servers(List.of(
                        new Server().url("http://localhost:" + serverPort).description("Local Development")
//                        new Server().url("https://auth.acheron.com").description("Production Server")
                ))
                .info(new Info()
                        .title(applicationName + " API")
                        .version("1.0.0")
                        .description("""
                                ### OAuth2 & OpenID Connect Authorization Server
                                
                                This is a production-ready Authorization Server based on Spring Security.
                                
                                **Key Features:**
                                * **OAuth 2.1 Support** (Authorization Code, Refresh Token, Client Credentials)
                                * **OIDC Compliant** (UserInfo, ID Token)
                                * **MFA Protection** (Time-based OTP via Google Authenticator)
                                * **JWK Source** (Rotational keys support)
                                
                                **How to test:**
                                1. Obtain an access token via `/oauth2/token`.
                                2. Click **Authorize** button above and paste the token.
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