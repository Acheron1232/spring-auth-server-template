package com.acheron.authserver.service;

import com.acheron.authserver.entity.User;
import com.acheron.authserver.exception.AppException;
import com.acheron.authserver.repository.UserRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.BDDMockito.given;

@ExtendWith(MockitoExtension.class)
public class UserServiceTest {
    @Mock
    private UserRepository userRepository;

    @InjectMocks
    private UserService userService;

    @Test
    @DisplayName("Should throw AppException when the userRepo returns Optional.empty() and should return User when the userRepo returns Optional.of(User)")
    void findByEmailTest() {
        given(userRepository.findUserByEmail(anyString()))
                .willReturn(Optional.of(new User()))
                .willReturn(Optional.empty());

        assertThat(userService.findByEmail(anyString()))
                .isNotNull()
                .isInstanceOf(User.class);

        assertThrows(AppException.class, () -> userService.findByEmail(anyString()));
    }
}
