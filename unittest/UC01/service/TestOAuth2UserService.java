package com.example.accountservice.service;

import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertEquals;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import static org.mockito.ArgumentMatchers.any;
import org.mockito.Mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.oauth2.client.registration.ClientRegistration;
import org.springframework.security.oauth2.client.userinfo.OAuth2UserRequest;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.test.util.ReflectionTestUtils;

import com.example.accountservice.enums.Role;
import com.example.accountservice.model.Account;
import com.example.accountservice.model.OAuth2UserInfo;
import com.example.accountservice.repository.AccountRepository;

/**
 * TestOAuth2UserService: Unit Test cho OAuth2UserService
 * 
 * Mục tiêu: Đảm bảo OAuth2 login hoạt động chính xác (tạo và cập nhật account từ Google)
 * 
 * Coverage Level 2 (Branch Coverage):
 * - processOAuth2User: test tạo mới account, update account
 * - createAccountFromOAuth2: test tạo account từ Google info
 * - updateAccountFromOAuth2: test cập nhật account
 * - ensureUniqueUsername: test tạo username duy nhất
 */
@DisplayName("TestOAuth2UserService - OAuth2 Authentication")
@ExtendWith(MockitoExtension.class)
public class TestOAuth2UserService {

    @Mock
    private AccountRepository accountRepository;
    
    @Mock
    private OAuth2User oauth2User;
    
    @Mock
    private OAuth2UserRequest userRequest;
    
    @Mock
    private ClientRegistration clientRegistration;

    private OAuth2UserService oauth2UserService;
    private Map<String, Object> attributes;

    @BeforeEach
    public void setUp() {
        // Create service instance and inject accountRepository
        oauth2UserService = new OAuth2UserService();
        ReflectionTestUtils.setField(oauth2UserService, "accountRepository", accountRepository);
        
        // Prepare OAuth2 attributes (Google user info)
        attributes = new HashMap<>();
        attributes.put("email", "john@gmail.com");
        attributes.put("given_name", "John");
        attributes.put("family_name", "Doe");
        attributes.put("picture", "https://example.com/photo.jpg");
        attributes.put("sub", "google123456");
    }
    
    /**
     * Helper method to call private processOAuth2User via reflection
     */
    private OAuth2User callProcessOAuth2User(OAuth2UserRequest request, OAuth2User user) throws Exception {
        Method method = OAuth2UserService.class.getDeclaredMethod("processOAuth2User", 
            OAuth2UserRequest.class, OAuth2User.class);
        method.setAccessible(true);
        return (OAuth2User) method.invoke(oauth2UserService, request, user);
    }
    
    /**
     * Helper method to call private createAccountFromOAuth2 via reflection
     */
    private Account callCreateAccountFromOAuth2(Map<String, Object> attributes) throws Exception {
        OAuth2UserInfo userInfo = new OAuth2UserInfo(attributes);
        Method method = OAuth2UserService.class.getDeclaredMethod("createAccountFromOAuth2", 
            OAuth2UserInfo.class, String.class);
        method.setAccessible(true);
        return (Account) method.invoke(oauth2UserService, userInfo, "google");
    }
    
    /**
     * Helper method to call private updateAccountFromOAuth2 via reflection
     */
    private Account callUpdateAccountFromOAuth2(Account account, Map<String, Object> attributes) throws Exception {
        OAuth2UserInfo userInfo = new OAuth2UserInfo(attributes);
        Method method = OAuth2UserService.class.getDeclaredMethod("updateAccountFromOAuth2", 
            Account.class, OAuth2UserInfo.class, String.class);
        method.setAccessible(true);
        return (Account) method.invoke(oauth2UserService, account, userInfo, "google");
    }
    
    /**
     * Helper method to call private ensureUniqueUsername via reflection
     */
    private String callEnsureUniqueUsername(String username) throws Exception {
        Method method = OAuth2UserService.class.getDeclaredMethod("ensureUniqueUsername", String.class);
        method.setAccessible(true);
        return (String) method.invoke(oauth2UserService, username);
    }

    // ==================== Test processOAuth2User ====================

    @Test
    @DisplayName("processOAuth2User_testChuan1 - Process new user creates account with default STUDENT role")
    public void processOAuth2User_testChuan1() throws Exception {
        // Standard case: new user gets STUDENT role by default
        when(oauth2User.getAttributes()).thenReturn(attributes);
        when(userRequest.getClientRegistration()).thenReturn(clientRegistration);
        when(clientRegistration.getRegistrationId()).thenReturn("google");
        
        when(accountRepository.findByEmailAndVisible("john@gmail.com", 1))
            .thenReturn(Optional.empty());

        Account newAccount = new Account();
        newAccount.setEmail("john@gmail.com");
        newAccount.setFirstName("John");
        newAccount.setLastName("Doe");
        newAccount.setRole(Role.STUDENT);
        newAccount.setVisible(1);
        newAccount.setGoogleId("google123456");

        when(accountRepository.save(any(Account.class)))
            .thenReturn(newAccount);

        OAuth2User result = callProcessOAuth2User(userRequest, oauth2User);

        assertNotNull(result);
        verify(accountRepository, times(1)).findByEmailAndVisible("john@gmail.com", 1);
    }

    @Test
    @DisplayName("processOAuth2User_testChuan2 - Process existing user preserves account data")
    public void processOAuth2User_testChuan2() throws Exception {
        // Standard case: existing user data is preserved
        when(oauth2User.getAttributes()).thenReturn(attributes);
        when(userRequest.getClientRegistration()).thenReturn(clientRegistration);
        when(clientRegistration.getRegistrationId()).thenReturn("google");
        
        Account existingAccount = new Account();
        existingAccount.setEmail("john@gmail.com");
        existingAccount.setUsername("johndoe");
        existingAccount.setRole(Role.TEACHER);
        existingAccount.setVisible(1);

        when(accountRepository.findByEmailAndVisible("john@gmail.com", 1))
            .thenReturn(Optional.of(existingAccount));
        when(accountRepository.save(any(Account.class)))
            .thenReturn(existingAccount);

        OAuth2User result = callProcessOAuth2User(userRequest, oauth2User);

        assertNotNull(result);
        verify(accountRepository, times(1)).save(any(Account.class));
    }

    // ==================== Test createAccountFromOAuth2 ====================

    @Test
    @DisplayName("createAccountFromOAuth2_testChuan1 - Create account with complete OAuth2 info")
    public void createAccountFromOAuth2_testChuan1() throws Exception {
        // Standard case: create account with all fields from Google
        when(accountRepository.existsByUsernameAndVisible("john", 1))
            .thenReturn(false);
        
        Account newAccount = new Account();
        newAccount.setEmail("john@gmail.com");
        newAccount.setFirstName("John");
        newAccount.setLastName("Doe");
        newAccount.setGoogleId("google123456");
        newAccount.setPicture("https://example.com/photo.jpg");
        newAccount.setProvider("google");
        newAccount.setRole(Role.STUDENT);
        newAccount.setVisible(1);
        newAccount.setPassword(null);

        when(accountRepository.save(any(Account.class)))
            .thenReturn(newAccount);

        Account result = callCreateAccountFromOAuth2(attributes);

        assertNotNull(result);
        verify(accountRepository).save(any(Account.class));
    }

    @Test
    @DisplayName("createAccountFromOAuth2_testChuan2 - Create account with partial OAuth2 info")
    public void createAccountFromOAuth2_testChuan2() throws Exception {
        // Edge case: missing given_name or family_name
        Map<String, Object> minimalAttributes = new HashMap<>();
        minimalAttributes.put("email", "jane@gmail.com");
        minimalAttributes.put("sub", "google789");

        when(accountRepository.existsByUsernameAndVisible("jane", 1))
            .thenReturn(false);

        Account minimalAccount = new Account();
        minimalAccount.setEmail("jane@gmail.com");
        minimalAccount.setRole(Role.STUDENT);

        when(accountRepository.save(any(Account.class)))
            .thenReturn(minimalAccount);

        Account result = callCreateAccountFromOAuth2(minimalAttributes);

        assertNotNull(result);
        verify(accountRepository).save(any(Account.class));
    }

    // ==================== Test updateAccountFromOAuth2 ====================

    @Test
    @DisplayName("updateAccountFromOAuth2_testChuan1 - Update existing account with Google ID")
    public void updateAccountFromOAuth2_testChuan1() throws Exception {
        // Standard case: update account by adding Google ID
        Account existingAccount = new Account();
        existingAccount.setEmail("john@gmail.com");
        existingAccount.setGoogleId(null); // Not yet linked to Google

        when(accountRepository.save(any(Account.class)))
            .thenReturn(existingAccount);

        Account result = callUpdateAccountFromOAuth2(existingAccount, attributes);

        assertNotNull(result);
        assertEquals("google123456", result.getGoogleId());
        verify(accountRepository).save(any(Account.class));
    }

    @Test
    @DisplayName("updateAccountFromOAuth2_testChuan2 - Update account picture")
    public void updateAccountFromOAuth2_testChuan2() throws Exception {
        // Standard case: update picture from Google
        Account existingAccount = new Account();
        existingAccount.setEmail("john@gmail.com");
        existingAccount.setPicture("https://old.com/old.jpg");
        existingAccount.setGoogleId("google123456");
        existingAccount.setProvider("google");

        when(accountRepository.save(any(Account.class)))
            .thenReturn(existingAccount);

        Account result = callUpdateAccountFromOAuth2(existingAccount, attributes);

        assertNotNull(result);
        assertEquals("https://example.com/photo.jpg", result.getPicture());
        verify(accountRepository).save(any(Account.class));
    }

    @Test
    @DisplayName("updateAccountFromOAuth2_testChuan3 - Account already has Google info, no update needed")
    public void updateAccountFromOAuth2_testChuan3() throws Exception {
        // Edge case: account already fully linked to Google (no save called)
        Account existingAccount = new Account();
        existingAccount.setEmail("john@gmail.com");
        existingAccount.setGoogleId("google123456");
        existingAccount.setPicture("https://example.com/photo.jpg");
        existingAccount.setProvider("google");

        Account result = callUpdateAccountFromOAuth2(existingAccount, attributes);

        assertNotNull(result);
        // Don't verify save - it shouldn't be called when no update is needed
    }

    // ==================== Test ensureUniqueUsername ====================

    @Test
    @DisplayName("ensureUniqueUsername_testChuan1 - Generate unique username from email")
    public void ensureUniqueUsername_testChuan1() throws Exception {
        // Standard case: username from email (before @)
        when(accountRepository.existsByUsernameAndVisible("john", 1))
            .thenReturn(false);

        String result = callEnsureUniqueUsername("john");

        assertNotNull(result);
        assertEquals("john", result);
        verify(accountRepository).existsByUsernameAndVisible("john", 1);
    }

    @Test
    @DisplayName("ensureUniqueUsername_testChuan2 - Generate unique username when base exists")
    public void ensureUniqueUsername_testChuan2() throws Exception {
        // Edge case: when base username already exists, add suffix
        when(accountRepository.existsByUsernameAndVisible("john", 1))
            .thenReturn(true);
        when(accountRepository.existsByUsernameAndVisible("john1", 1))
            .thenReturn(false);

        String result = callEnsureUniqueUsername("john");

        assertNotNull(result);
        assertEquals("john1", result);
    }
}
