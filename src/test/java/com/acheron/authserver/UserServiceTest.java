package com.acheron.authserver;

import com.acheron.authserver.entity.Role;
import com.acheron.authserver.entity.User;
import com.acheron.authserver.repository.UserRepository;
import com.acheron.authserver.service.UserService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import java.util.NoSuchElementException;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@Import(TestcontainersConfiguration.class)
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("test")
@Transactional
class UserServiceTest {

    @Autowired
    private UserService userService;

    @Autowired
    private UserRepository userRepository;

    private User testUser;

    @BeforeEach
    void setUp() {
        testUser = User.builder()
                .username("testuser")
                .email("test@example.com")
                .passwordHash("$2a$10$dummyhash")
                .role(Role.USER)
                .enabled(true)
                .emailVerified(false)
                .mfaEnabled(false)
                .build();
        testUser = userRepository.save(testUser);
    }

    @Test
    void loadUserByUsername_existingUser_returnsUser() {
        UserDetails userDetails = userService.loadUserByUsername("testuser");
        assertThat(userDetails).isNotNull();
        assertThat(userDetails.getUsername()).isEqualTo("testuser");
    }

    @Test
    void loadUserByUsername_nonExistentUser_throws() {
        assertThatThrownBy(() -> userService.loadUserByUsername("nonexistent"))
                .isInstanceOf(NoSuchElementException.class);
    }

    @Test
    void findByEmail_existingEmail_returnsUser() {
        User found = userService.findByEmail("test@example.com");
        assertThat(found).isNotNull();
        assertThat(found.getEmail()).isEqualTo("test@example.com");
    }

    @Test
    void existsByEmail_existingEmail_returnsTrue() {
        assertThat(userService.existsByEmail("test@example.com")).isTrue();
    }

    @Test
    void existsByEmail_nonExistentEmail_returnsFalse() {
        assertThat(userService.existsByEmail("nonexistent@example.com")).isFalse();
    }

    @Test
    void getUserInfo_returnsCorrectResponse() {
        var response = userService.getUserInfo(testUser);
        assertThat(response.getStatusCode().is2xxSuccessful()).isTrue();
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().email()).isEqualTo("test@example.com");
        assertThat(response.getBody().username()).isEqualTo("testuser");
        assertThat(response.getBody().role()).isEqualTo(Role.USER);
    }

    @Test
    void deleteUser_softDeletes() {
        var response = userService.delete(testUser);
        assertThat(response.getStatusCode().is2xxSuccessful()).isTrue();
        assertThat(userRepository.findUserByUsername("testuser")).isEmpty();
    }

    @Test
    void saveUser_persistsCorrectly() {
        User newUser = User.builder()
                .username("newuser")
                .email("new@example.com")
                .passwordHash("hash")
                .role(Role.USER)
                .enabled(true)
                .emailVerified(true)
                .mfaEnabled(false)
                .build();
        User saved = userService.save(newUser);
        assertThat(saved.getId()).isNotNull();
        assertThat(saved.getCreatedAt()).isNotNull();
    }
}
