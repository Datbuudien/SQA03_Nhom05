package com.example.accountservice.service;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import org.mockito.junit.jupiter.MockitoExtension;

import com.example.accountservice.enums.Role;
import com.example.accountservice.model.Account;
import com.example.accountservice.model.RedisTokenInfo;
import com.example.accountservice.repository.RedisTokenRepository;

/**
 * TestRedisTokenService: Unit Test cho RedisTokenService
 * 
 * Mục tiêu: Đảm bảo token được lưu, lấy, xác thực và xóa khỏi Redis chính xác
 * 
 * Coverage Level 2 (Branch Coverage):
 * - saveTokenInfo: test lưu thành công, exception
 * - getTokenInfo: test tìm thấy, không tìm thấy, exception
 * - isTokenValid: test token tồn tại, không tồn tại, exception
 * - revokeToken: test xóa thành công, exception
 */
@DisplayName("TestRedisTokenService - Redis Token Management")
@ExtendWith(MockitoExtension.class)
public class TestRedisTokenService {

    @Mock
    private RedisTokenRepository redisTokenRepository;

    @InjectMocks
    private RedisTokenService redisTokenService;

    private Account testAccount;
    private RedisTokenInfo tokenInfo;

    @BeforeEach
    public void setUp() {
        // Prepare test account
        testAccount = new Account();
        testAccount.setUsername("testuser");
        testAccount.setRole(Role.STUDENT);

        // Prepare token info
        tokenInfo = new RedisTokenInfo("uuid-1234", "testuser", Role.STUDENT);
    }

    // ==================== Test saveTokenInfo ====================

    @Test
    @DisplayName("saveTokenInfo_testChuan1 - Save token info successfully for STUDENT")
    public void saveTokenInfo_testChuan1() {
        // Standard case: save token for student
        testAccount.setRole(Role.STUDENT);
        when(redisTokenRepository.save(any(RedisTokenInfo.class)))
            .thenReturn(tokenInfo);

        redisTokenService.saveTokenInfo("uuid-1234", testAccount);

        verify(redisTokenRepository, times(1)).deleteById("testuser");
        verify(redisTokenRepository, times(1)).save(any(RedisTokenInfo.class));
    }

    @Test
    @DisplayName("saveTokenInfo_testChuan2 - Save token info successfully for TEACHER")
    public void saveTokenInfo_testChuan2() {
        // Standard case: save token for teacher
        testAccount.setRole(Role.TEACHER);
        RedisTokenInfo teacherToken = new RedisTokenInfo("uuid-5678", "teacher01", Role.TEACHER);
        when(redisTokenRepository.save(any(RedisTokenInfo.class)))
            .thenReturn(teacherToken);

        redisTokenService.saveTokenInfo("uuid-5678", testAccount);

        verify(redisTokenRepository, times(1)).deleteById("testuser");
        verify(redisTokenRepository, times(1)).save(any(RedisTokenInfo.class));
    }

    @Test
    @DisplayName("saveTokenInfo_testChuan3 - Save token info successfully for ADMIN")
    public void saveTokenInfo_testChuan3() {
        // Standard case: save token for admin
        testAccount.setRole(Role.ADMIN);
        RedisTokenInfo adminToken = new RedisTokenInfo("uuid-9999", "admin01", Role.ADMIN);
        when(redisTokenRepository.save(any(RedisTokenInfo.class)))
            .thenReturn(adminToken);

        redisTokenService.saveTokenInfo("uuid-9999", testAccount);

        verify(redisTokenRepository, times(1)).deleteById("testuser");
        verify(redisTokenRepository, times(1)).save(any(RedisTokenInfo.class));
    }

    @Test
    @DisplayName("saveTokenInfo_ngoaile1 - Handle exception when deleting old token")
    public void saveTokenInfo_ngoaile1() {
        // Error case: exception during deleteById
        doThrow(new RuntimeException("Redis error"))
            .when(redisTokenRepository).deleteById(anyString());

        // Should not throw, but catch exception internally
        assertDoesNotThrow(() -> {
            redisTokenService.saveTokenInfo("uuid-1234", testAccount);
        });

        verify(redisTokenRepository, times(1)).deleteById("testuser");
    }

    @Test
    @DisplayName("saveTokenInfo_ngoaile2 - Handle exception when saving token")
    public void saveTokenInfo_ngoaile2() {
        // Error case: exception during save
        doThrow(new RuntimeException("Redis connection failed"))
            .when(redisTokenRepository).save(any(RedisTokenInfo.class));

        // Should not throw, but catch exception internally
        assertDoesNotThrow(() -> {
            redisTokenService.saveTokenInfo("uuid-1234", testAccount);
        });

        verify(redisTokenRepository, times(1)).deleteById("testuser");
        verify(redisTokenRepository, times(1)).save(any(RedisTokenInfo.class));
    }

    // ==================== Test getTokenInfo ====================

    @Test
    @DisplayName("getTokenInfo_testChuan1 - Get token info returns present optional")
    public void getTokenInfo_testChuan1() {
        // Standard case: token found
        when(redisTokenRepository.findById("testuser"))
            .thenReturn(Optional.of(tokenInfo));

        Optional<RedisTokenInfo> result = redisTokenService.getTokenInfo("testuser");

        assertTrue(result.isPresent());
        assertEquals("uuid-1234", result.get().getJti());
        assertEquals("testuser", result.get().getUsername());
        assertEquals(Role.STUDENT, result.get().getRole());
        verify(redisTokenRepository, times(1)).findById("testuser");
    }

    @Test
    @DisplayName("getTokenInfo_testChuan2 - Get non-existent token returns empty optional")
    public void getTokenInfo_testChuan2() {
        // Standard case: token not found
        when(redisTokenRepository.findById("nonexistent"))
            .thenReturn(Optional.empty());

        Optional<RedisTokenInfo> result = redisTokenService.getTokenInfo("nonexistent");

        assertFalse(result.isPresent());
        verify(redisTokenRepository, times(1)).findById("nonexistent");
    }

    @Test
    @DisplayName("getTokenInfo_ngoaile1 - Handle exception when getting token")
    public void getTokenInfo_ngoaile1() {
        // Error case: exception during findById
        when(redisTokenRepository.findById("testuser"))
            .thenThrow(new RuntimeException("Redis error"));

        Optional<RedisTokenInfo> result = redisTokenService.getTokenInfo("testuser");

        assertFalse(result.isPresent());
        verify(redisTokenRepository, times(1)).findById("testuser");
    }

    // ==================== Test isTokenValid ====================

    @Test
    @DisplayName("isTokenValid_testChuan1 - Valid token exists returns true")
    public void isTokenValid_testChuan1() {
        // Standard case: token exists
        when(redisTokenRepository.existsById("testuser"))
            .thenReturn(true);

        boolean result = redisTokenService.isTokenValid("testuser");

        assertTrue(result);
        verify(redisTokenRepository, times(1)).existsById("testuser");
    }

    @Test
    @DisplayName("isTokenValid_testChuan2 - Invalid token does not exist returns false")
    public void isTokenValid_testChuan2() {
        // Standard case: token does not exist
        when(redisTokenRepository.existsById("invalidtoken"))
            .thenReturn(false);

        boolean result = redisTokenService.isTokenValid("invalidtoken");

        assertFalse(result);
        verify(redisTokenRepository, times(1)).existsById("invalidtoken");
    }

    @Test
    @DisplayName("isTokenValid_ngoaile1 - Handle exception when checking token existence")
    public void isTokenValid_ngoaile1() {
        // Error case: exception during existsById
        when(redisTokenRepository.existsById("testuser"))
            .thenThrow(new RuntimeException("Redis error"));

        boolean result = redisTokenService.isTokenValid("testuser");

        assertFalse(result);
        verify(redisTokenRepository, times(1)).existsById("testuser");
    }

    // ==================== Test revokeToken ====================

    @Test
    @DisplayName("revokeToken_testChuan1 - Revoke token successfully")
    public void revokeToken_testChuan1() {
        // Standard case: revoke token
        doNothing().when(redisTokenRepository).deleteById("testuser");

        redisTokenService.revokeToken("testuser");

        verify(redisTokenRepository, times(1)).deleteById("testuser");
    }

    @Test
    @DisplayName("revokeToken_testChuan2 - Revoke non-existent token succeeds")
    public void revokeToken_testChuan2() {
        // Standard case: revoke non-existent token (should not throw)
        doNothing().when(redisTokenRepository).deleteById("nonexistent");

        redisTokenService.revokeToken("nonexistent");

        verify(redisTokenRepository, times(1)).deleteById("nonexistent");
    }

    @Test
    @DisplayName("revokeToken_ngoaile1 - Handle exception when revoking token")
    public void revokeToken_ngoaile1() {
        // Error case: exception during deleteById
        doThrow(new RuntimeException("Redis error"))
            .when(redisTokenRepository).deleteById("testuser");

        // Should not throw, but catch exception internally
        assertDoesNotThrow(() -> {
            redisTokenService.revokeToken("testuser");
        });

        verify(redisTokenRepository, times(1)).deleteById("testuser");
    }

    @Test
    @DisplayName("revokeToken_testChuan3 - Revoke multiple tokens successfully")
    public void revokeToken_testChuan3() {
        // Test revoking multiple tokens sequentially
        doNothing().when(redisTokenRepository).deleteById(anyString());

        redisTokenService.revokeToken("user1");
        redisTokenService.revokeToken("user2");
        redisTokenService.revokeToken("user3");

        verify(redisTokenRepository, times(3)).deleteById(anyString());
    }
}
