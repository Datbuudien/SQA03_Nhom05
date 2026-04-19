package com.example.accountservice.controller;

import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.notNullValue;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import static org.mockito.ArgumentMatchers.anyString;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.web.servlet.MockMvc;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.cookie;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import com.example.accountservice.service.RedisTokenService;

@DisplayName("TestAuthController - Authentication Controller")
@ExtendWith(MockitoExtension.class)
public class TestAuthController {

    @Mock
    private RedisTokenService redisTokenService;

    @InjectMocks
    private AuthController authController;

    private MockMvc mockMvc;

    @BeforeEach
    public void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(authController).build();
    }

    @Test
    @DisplayName("logout_testChuan1 - Logout successfully with valid username")
    public void logout_testChuan1() throws Exception {
        doNothing().when(redisTokenService).revokeToken("testuser");

        mockMvc.perform(post("/account/logout")
                .header("X-Username", "testuser"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message", containsString("Logout successful")))
                .andExpect(jsonPath("$.message", containsString("Cookie cleared")));

        verify(redisTokenService, times(1)).revokeToken("testuser");
    }

    @Test
    @DisplayName("logout_testChuan2 - Logout successfully for different users")
    public void logout_testChuan2() throws Exception {
        doNothing().when(redisTokenService).revokeToken(anyString());

        mockMvc.perform(post("/account/logout")
                .header("X-Username", "user1"))
                .andExpect(status().isOk());

        mockMvc.perform(post("/account/logout")
                .header("X-Username", "user2"))
                .andExpect(status().isOk());

        verify(redisTokenService, times(1)).revokeToken("user1");
        verify(redisTokenService, times(1)).revokeToken("user2");
    }

    @Test
    @DisplayName("logout_testChuan3 - Logout response includes JWT cookie clear")
    public void logout_testChuan3() throws Exception {
        doNothing().when(redisTokenService).revokeToken("testuser");

        mockMvc.perform(post("/account/logout")
                .header("X-Username", "testuser"))
                .andExpect(status().isOk())
                .andExpect(cookie().exists("jwt"))
                .andExpect(cookie().maxAge("jwt", 0))
                .andExpect(cookie().httpOnly("jwt", true));

        verify(redisTokenService, times(1)).revokeToken("testuser");
    }

    @Test
    @DisplayName("logout_ngoaile1 - Missing X-Username header returns bad request")
    public void logout_ngoaile1() throws Exception {
        mockMvc.perform(post("/account/logout"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error", containsString("Missing X-Username header")));

        verify(redisTokenService, never()).revokeToken(anyString());
    }

    @Test
    @DisplayName("logout_ngoaile2 - Blank X-Username header returns bad request")
    public void logout_ngoaile2() throws Exception {
        mockMvc.perform(post("/account/logout")
                .header("X-Username", "   "))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error", containsString("Missing X-Username header")));

        verify(redisTokenService, never()).revokeToken(anyString());
    }

    @Test
    @DisplayName("logout_ngoaile3 - Empty X-Username header returns bad request")
    public void logout_ngoaile3() throws Exception {
        mockMvc.perform(post("/account/logout")
                .header("X-Username", ""))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error", containsString("Missing X-Username header")));

        verify(redisTokenService, never()).revokeToken(anyString());
    }

    @Test
    @DisplayName("logout_ngoaile4 - Handle exception during token revocation")
    public void logout_ngoaile4() throws Exception {
        doThrow(new RuntimeException("Redis connection failed"))
            .when(redisTokenService).revokeToken("testuser");

        mockMvc.perform(post("/account/logout")
                .header("X-Username", "testuser"))
                .andExpect(status().isInternalServerError())
                .andExpect(jsonPath("$.error", containsString("Logout failed")))
                .andExpect(jsonPath("$.message", notNullValue()));

        verify(redisTokenService, times(1)).revokeToken("testuser");
    }

    @Test
    @DisplayName("logout_testChuan4 - Logout with username containing numbers")
    public void logout_testChuan4() throws Exception {
        doNothing().when(redisTokenService).revokeToken("user_123");

        mockMvc.perform(post("/account/logout")
                .header("X-Username", "user_123"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message", containsString("Logout successful")));

        verify(redisTokenService, times(1)).revokeToken("user_123");
    }

    @Test
    @DisplayName("logout_testChuan5 - Multiple logout requests for same user")
    public void logout_testChuan5() throws Exception {
        doNothing().when(redisTokenService).revokeToken("testuser");

        mockMvc.perform(post("/account/logout")
                .header("X-Username", "testuser"))
                .andExpect(status().isOk());

        mockMvc.perform(post("/account/logout")
                .header("X-Username", "testuser"))
                .andExpect(status().isOk());

        verify(redisTokenService, times(2)).revokeToken("testuser");
    }
}
