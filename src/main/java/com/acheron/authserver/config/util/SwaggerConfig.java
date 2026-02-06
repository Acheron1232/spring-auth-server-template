package com.acheron.authserver.config.util;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.info.License;
import io.swagger.v3.oas.models.servers.Server;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.List;

@Configuration
public class SwaggerConfig {

    @Value("${server.port:8080}")
    private String serverPort;

    @Bean
    public OpenAPI customOpenAPI() {

        List<Server> servers = List.of(
                new Server().url("http://localhost:" + serverPort).description("Local development server")
        );

        return new OpenAPI()
                .servers(servers)
                .info(new Info()
                        .title("Spring Orders API")
                        .version("1.0.0")
                        .description("""
                                REST API for managing customers and orders.
                                Features:
                                - Filtering
                                - Pagination
                                """)
                        .contact(new Contact()
                                .name("Acheron")
                                .email("aryemfedorov@gmail.com")
                                .url("https://github.com/Acheron1232"))
                        .license(new License()
                                .name("Apache 2.0")
                                .url("https://www.apache.org/licenses/LICENSE-2.0.html")))
                ;
    }
}
