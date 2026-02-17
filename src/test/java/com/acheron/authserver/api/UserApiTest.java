package com.acheron.authserver.api;

import com.acheron.authserver.dto.response.UserResponse;
import com.acheron.authserver.entity.User;
import com.acheron.authserver.service.QrCodeService;
import com.acheron.authserver.service.UserService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.http.ResponseEntity;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(UserApi.class)
public class UserApiTest {
    @MockitoBean
    private UserService userService;
    @MockitoBean
    private QrCodeService qrCodeService;
    @Autowired
    private MockMvc mockMvc;

    @Test
    @DisplayName("Should return 200 OK and User JSON when authenticated")
    void shouldReturnCurrentUser() throws Exception {
        User mockUser = new User();
        mockUser.setEmail("test@gmail.com");

        UserResponse response = UserResponse.fromEntity(mockUser);

        given(userService.getUserInfo(any(User.class)))
                .willReturn(ResponseEntity.ok(response));

        mockMvc.perform(get("/user-info").with(user(mockUser)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.email").value("test@gmail.com"));
    }
}