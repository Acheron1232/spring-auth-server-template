package com.acheron.authserver;

import com.acheron.authserver.entity.Role;
import com.acheron.authserver.entity.User;
import com.acheron.authserver.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@Import(TestcontainersConfiguration.class)
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
class UserApiTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    private User testUser;

    @BeforeEach
    void setUp() {
        testUser = User.builder()
                .username("apiuser")
                .email("apiuser@example.com")
                .passwordHash(passwordEncoder.encode("password"))
                .role(Role.USER)
                .enabled(true)
                .emailVerified(true)
                .mfaEnabled(false)
                .build();
        testUser = userRepository.save(testUser);
    }

    @Test
    void getCurrentUser_authenticated_returnsUser() throws Exception {
        mockMvc.perform(get("/user-info").with(user(testUser)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.email").value("apiuser@example.com"))
                .andExpect(jsonPath("$.username").value("apiuser"))
                .andExpect(jsonPath("$.role").value("USER"));
    }

    @Test
    void getCurrentUser_unauthenticated_returns401() throws Exception {
        mockMvc.perform(get("/user-info"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void updateUser_validRequest_returnsUpdated() throws Exception {
        mockMvc.perform(put("/user-info")
                        .with(user(testUser))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                    "email": "updated@example.com",
                                    "username": "updateduser",
                                    "enabled": true,
                                    "locked": false
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.email").value("updated@example.com"))
                .andExpect(jsonPath("$.username").value("updateduser"));
    }

    @Test
    void patchUser_partialUpdate_returnsPatched() throws Exception {
        mockMvc.perform(patch("/user-info")
                        .with(user(testUser))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                    "username": "patcheduser"
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.username").value("patcheduser"))
                .andExpect(jsonPath("$.email").value("apiuser@example.com"));
    }

    @Test
    void deleteUser_authenticated_returns204() throws Exception {
        mockMvc.perform(delete("/user-info").with(user(testUser)))
                .andExpect(status().isNoContent());
    }
}
