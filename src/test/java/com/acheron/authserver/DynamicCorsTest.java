package com.acheron.authserver;

import com.acheron.authserver.config.DynamicCorsConfigurationSource;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.options;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;

@Import(TestcontainersConfiguration.class)
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@AutoConfigureMockMvc
@ActiveProfiles("test")
class DynamicCorsTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private DynamicCorsConfigurationSource corsSource;

    @Test
    void cors_allowedOrigin_returnsHeaders() throws Exception {
        corsSource.addOrigin("http://localhost:3000");

        mockMvc.perform(options("/oauth2/authorize")
                        .header("Origin", "http://localhost:3000")
                        .header("Access-Control-Request-Method", "GET"))
                .andExpect(header().string("Access-Control-Allow-Origin", "http://localhost:3000"))
                .andExpect(header().string("Access-Control-Allow-Credentials", "true"));
    }

    @Test
    void cors_unknownOrigin_noHeaders() throws Exception {
        mockMvc.perform(options("/oauth2/authorize")
                        .header("Origin", "http://evil.com")
                        .header("Access-Control-Request-Method", "GET"))
                .andExpect(header().doesNotExist("Access-Control-Allow-Origin"));
    }

    @Test
    void cors_dynamicallyAddedOrigin_works() throws Exception {
        String newOrigin = "http://dynamic-app.example.com";
        corsSource.addOrigin(newOrigin);

        mockMvc.perform(options("/oauth2/authorize")
                        .header("Origin", newOrigin)
                        .header("Access-Control-Request-Method", "GET"))
                .andExpect(header().string("Access-Control-Allow-Origin", newOrigin));
    }
}
