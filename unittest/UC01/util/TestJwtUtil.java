package com.example.accountservice.util;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import com.example.accountservice.enums.Role;
import com.example.accountservice.model.Account;

import io.jsonwebtoken.JwtException;

/**
 * TestJwtUtil: Unit Test cho JwtUtil
 * 
 * Mục tiêu: Đảm bảo JWT token được tạo, giải mã và xác thực chính xác
 * 
 * Coverage Level 2 (Branch Coverage):
 * - generateToken: test các trường hợp khác nhau với các account khác nhau
 * - getUsernameFromToken: test token hợp lệ và không hợp lệ
 * - getJwtIdFromToken: test token hợp lệ
 * - extractDomain: test URL với protocol, không có protocol, null input
 */
@DisplayName("TestJwtUtil - JWT Token Utilities")
public class TestJwtUtil {

    private JwtUtil jwtUtil;
    private Account account;

    @BeforeEach
    public void setUp() {
        jwtUtil = new JwtUtil();
        // Cấu hình JWT secret và expiration
        ReflectionTestUtils.setField(jwtUtil, "jwtSecret", 
            "mySecretKeyForJwtTokenThatShouldBeAtLeast256BitsLongToEnsureSecurityAndProperFunctioning");
        ReflectionTestUtils.setField(jwtUtil, "jwtExpiration", 86400000L); // 24 hours
        
        // Tạo test account
        account = new Account();
        account.setUsername("testuser");
        account.setRole(Role.STUDENT);
    }

    // ==================== Test generateToken ====================
    
    @Test
    @DisplayName("generateToken_testChuan1 - Generate valid token for STUDENT account")
    public void generateToken_testChuan1() {
        // Standard case: generate token for STUDENT
        Account studentAccount = new Account();
        studentAccount.setUsername("student01");
        studentAccount.setRole(Role.STUDENT);
        
        String token = jwtUtil.generateToken(studentAccount);
        
        assertNotNull(token);
        assertFalse(token.isBlank());
        assertTrue(token.contains("."));
        
        // Verify token contains 3 parts (header.payload.signature)
        String[] parts = token.split("\\.");
        assertEquals(3, parts.length);
    }

    @Test
    @DisplayName("generateToken_testChuan2 - Generate valid token for TEACHER account")
    public void generateToken_testChuan2() {
        // Standard case: generate token for TEACHER
        Account teacherAccount = new Account();
        teacherAccount.setUsername("teacher01");
        teacherAccount.setRole(Role.TEACHER);
        
        String token = jwtUtil.generateToken(teacherAccount);
        
        assertNotNull(token);
        assertFalse(token.isBlank());
        
        // Verify username can be extracted
        String extractedUsername = jwtUtil.getUsernameFromToken(token);
        assertEquals("teacher01", extractedUsername);
    }

    @Test
    @DisplayName("generateToken_testChuan3 - Generate valid token for ADMIN account")
    public void generateToken_testChuan3() {
        // Standard case: generate token for ADMIN
        Account adminAccount = new Account();
        adminAccount.setUsername("admin01");
        adminAccount.setRole(Role.ADMIN);
        
        String token = jwtUtil.generateToken(adminAccount);
        
        assertNotNull(token);
        assertFalse(token.isBlank());
        
        String extractedUsername = jwtUtil.getUsernameFromToken(token);
        assertEquals("admin01", extractedUsername);
    }

    @Test
    @DisplayName("generateToken_testChuan4 - Each generated token has unique JWT ID")
    public void generateToken_testChuan4() {
        // Test that each token has unique JTI
        String token1 = jwtUtil.generateToken(account);
        String token2 = jwtUtil.generateToken(account);
        
        String jti1 = jwtUtil.getJwtIdFromToken(token1);
        String jti2 = jwtUtil.getJwtIdFromToken(token2);
        
        assertNotNull(jti1);
        assertNotNull(jti2);
        assertNotEquals(jti1, jti2);
    }

    // ==================== Test getUsernameFromToken ====================
    
    @Test
    @DisplayName("getUsernameFromToken_testChuan1 - Extract username from valid token")
    public void getUsernameFromToken_testChuan1() {
        // Standard case: extract username from valid token
        Account testAccount = new Account();
        testAccount.setUsername("username123");
        testAccount.setRole(Role.STUDENT);
        
        String token = jwtUtil.generateToken(testAccount);
        String extractedUsername = jwtUtil.getUsernameFromToken(token);
        
        assertEquals("username123", extractedUsername);
    }

    @Test
    @DisplayName("getUsernameFromToken_testChuan2 - Extract username from token with special characters")
    public void getUsernameFromToken_testChuan2() {
        // Edge case: username with numbers
        Account testAccount = new Account();
        testAccount.setUsername("user2024");
        testAccount.setRole(Role.TEACHER);
        
        String token = jwtUtil.generateToken(testAccount);
        String extractedUsername = jwtUtil.getUsernameFromToken(token);
        
        assertEquals("user2024", extractedUsername);
    }

    @Test
    @DisplayName("getUsernameFromToken_ngoaile1 - Throw exception for invalid token format")
    public void getUsernameFromToken_ngoaile1() {
        // Error case: token with invalid format
        String invalidToken = "invalid.token.format";
        
        assertThrows(JwtException.class, () -> {
            jwtUtil.getUsernameFromToken(invalidToken);
        });
    }

    @Test
    @DisplayName("getUsernameFromToken_ngoaile2 - Throw exception for malformed token")
    public void getUsernameFromToken_ngoaile2() {
        // Error case: completely invalid token
        String invalidToken = "not-a-jwt-token";
        
        assertThrows(JwtException.class, () -> {
            jwtUtil.getUsernameFromToken(invalidToken);
        });
    }

    // ==================== Test getJwtIdFromToken ====================
    
    @Test
    @DisplayName("getJwtIdFromToken_testChuan1 - Extract JWT ID from valid token")
    public void getJwtIdFromToken_testChuan1() {
        // Standard case: extract JTI from valid token
        String token = jwtUtil.generateToken(account);
        String jwtId = jwtUtil.getJwtIdFromToken(token);
        
        assertNotNull(jwtId);
        assertFalse(jwtId.isBlank());
        
        // JWT ID should be UUID format (contain hyphens)
        assertTrue(jwtId.contains("-") || jwtId.length() > 20);
    }

    @Test
    @DisplayName("getJwtIdFromToken_testChuan2 - Multiple tokens have different JWT IDs")
    public void getJwtIdFromToken_testChuan2() {
        // Standard case: each token should have unique JTI
        String token1 = jwtUtil.generateToken(account);
        String token2 = jwtUtil.generateToken(account);
        
        String jti1 = jwtUtil.getJwtIdFromToken(token1);
        String jti2 = jwtUtil.getJwtIdFromToken(token2);
        
        assertNotEquals(jti1, jti2);
    }

    @Test
    @DisplayName("getJwtIdFromToken_ngoaile1 - Throw exception for invalid token")
    public void getJwtIdFromToken_ngoaile1() {
        // Error case: invalid token format
        String invalidToken = "invalid.jwt.token";
        
        assertThrows(JwtException.class, () -> {
            jwtUtil.getJwtIdFromToken(invalidToken);
        });
    }

    // ==================== Test extractDomain ====================
    
    @Test
    @DisplayName("extractDomain_testChuan1 - Extract domain from URL with https protocol")
    public void extractDomain_testChuan1() {
        // Standard case: URL with https
        String url = "https://example.com/path";
        String domain = jwtUtil.extractDomain(url);
        
        assertEquals("example.com", domain);
    }

    @Test
    @DisplayName("extractDomain_testChuan2 - Extract domain from URL with http protocol")
    public void extractDomain_testChuan2() {
        // Standard case: URL with http
        String url = "http://example.com";
        String domain = jwtUtil.extractDomain(url);
        
        assertEquals("example.com", domain);
    }

    @Test
    @DisplayName("extractDomain_testChuan3 - Extract domain from URL with www prefix")
    public void extractDomain_testChuan3() {
        // Standard case: URL with www prefix (should be removed)
        String url = "https://www.example.com";
        String domain = jwtUtil.extractDomain(url);
        
        assertEquals("example.com", domain);
    }

    @Test
    @DisplayName("extractDomain_testChuan4 - Extract domain from URL without protocol")
    public void extractDomain_testChuan4() {
        // Standard case: URL without protocol (http added automatically)
        String url = "example.com";
        String domain = jwtUtil.extractDomain(url);
        
        assertEquals("example.com", domain);
    }

    @Test
    @DisplayName("extractDomain_testChuan5 - Extract domain with subdomain")
    public void extractDomain_testChuan5() {
        // Standard case: subdomain URL
        String url = "https://api.example.com";
        String domain = jwtUtil.extractDomain(url);
        
        assertEquals("api.example.com", domain);
    }

    @Test
    @DisplayName("extractDomain_ngoaile1 - Return null for null URL")
    public void extractDomain_ngoaile1() {
        // Error case: null URL
        String domain = jwtUtil.extractDomain(null);
        
        assertNull(domain);
    }

    @Test
    @DisplayName("extractDomain_ngoaile2 - Return null for empty URL")
    public void extractDomain_ngoaile2() {
        // Error case: empty URL
        String domain = jwtUtil.extractDomain("");
        
        assertNull(domain);
    }

    @Test
    @DisplayName("extractDomain_ngoaile3 - Return null for invalid URL format")
    public void extractDomain_ngoaile3() {
        // Error case: invalid URL with special characters
        String domain = jwtUtil.extractDomain("://invalid");
        
        assertNull(domain);
    }

    @Test
    @DisplayName("extractDomain_ngoaile4 - Handle URL with port number")
    public void extractDomain_ngoaile4() {
        // Edge case: URL with port
        String url = "http://example.com:8080";
        String domain = jwtUtil.extractDomain(url);
        
        assertEquals("example.com", domain);
    }
}
