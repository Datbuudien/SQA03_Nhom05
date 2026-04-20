package com.example.accountservice.service;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

import com.example.accountservice.enums.Role;
import com.example.accountservice.model.Account;
import com.example.accountservice.repository.AccountRepository;
import com.example.accountservice.util.UsernameGenerator;

import jakarta.persistence.EntityNotFoundException;

/**
 * TestAccountService: Unit Test cho AccountService
 * 
 * Mục tiêu: Đảm bảo các method lấy, tạo, xóa account hoạt động chính xác
 * 
 * Coverage Level 2 (Branch Coverage):
 * - getAccountByUsername: test tìm thấy và không tìm thấy
 * - findByUsernameAndVisible: test tìm thấy và trả về empty
 * - existsByCccd: test tồn tại và không tồn tại
 * - existsByEmail: test email hợp lệ, null, rỗng, tồn tại
 * - createAccount: test tạo account thành công, CCCD trùng, email trùng
 * - deleteAccount: test xóa thành công, không tìm thấy, đã xóa
 */
@DisplayName("TestAccountService - Account Service Business Logic")
@ExtendWith(MockitoExtension.class)
public class TestAccountService {

    @Mock
    private AccountRepository accountRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @Mock
    private UsernameGenerator usernameGenerator;

    @InjectMocks
    private AccountService accountService;

    private Account testAccount;

    @BeforeEach
    public void setUp() {
        // Prepare test account
        testAccount = new Account();
        testAccount.setId(1L);
        testAccount.setUsername("testuser");
        testAccount.setEmail("test@example.com");
        testAccount.setCccd("123456789");
        testAccount.setPassword("encryptedPassword");
        testAccount.setRole(Role.STUDENT);
        testAccount.setVisible(1);
        testAccount.setFirstName("Test");
        testAccount.setLastName("User");
    }

    // ==================== Test getAccountByUsername ====================

    @Test
    @DisplayName("getAccountByUsername_testChuan1 - Get account by username successfully")
    public void getAccountByUsername_testChuan1() {
        // Standard case: account found
        when(accountRepository.findByUsernameAndVisible("testuser", 1))
            .thenReturn(Optional.of(testAccount));

        Optional<Account> result = accountService.getAccountByUsername("testuser");

        assertTrue(result.isPresent());
        assertEquals("testuser", result.get().getUsername());
        assertEquals("test@example.com", result.get().getEmail());
        verify(accountRepository, times(1)).findByUsernameAndVisible("testuser", 1);
    }

    @Test
    @DisplayName("getAccountByUsername_ngoaile1 - Throw exception when account not found")
    public void getAccountByUsername_ngoaile1() {
        // Error case: account not found
        when(accountRepository.findByUsernameAndVisible("nonexistent", 1))
            .thenReturn(Optional.empty());

        assertThrows(EntityNotFoundException.class, () -> {
            accountService.getAccountByUsername("nonexistent");
        });

        verify(accountRepository, times(1)).findByUsernameAndVisible("nonexistent", 1);
    }

    // ==================== Test findByUsernameAndVisible ====================

    @Test
    @DisplayName("findByUsernameAndVisible_testChuan1 - Find account by username returns present optional")
    public void findByUsernameAndVisible_testChuan1() {
        // Standard case: account found
        when(accountRepository.findByUsernameAndVisible("testuser", 1))
            .thenReturn(Optional.of(testAccount));

        Optional<Account> result = accountService.findByUsernameAndVisible("testuser");

        assertTrue(result.isPresent());
        assertEquals("testuser", result.get().getUsername());
    }

    @Test
    @DisplayName("findByUsernameAndVisible_testChuan2 - Find non-existent account returns empty optional")
    public void findByUsernameAndVisible_testChuan2() {
        // Standard case: account not found
        when(accountRepository.findByUsernameAndVisible("nonexistent", 1))
            .thenReturn(Optional.empty());

        Optional<Account> result = accountService.findByUsernameAndVisible("nonexistent");

        assertFalse(result.isPresent());
    }

    // ==================== Test existsByCccd ====================

    @Test
    @DisplayName("existsByCccd_testChuan1 - CCCD exists returns true")
    public void existsByCccd_testChuan1() {
        // Standard case: CCCD exists
        when(accountRepository.existsByCccdAndVisible("123456789", 1))
            .thenReturn(true);

        boolean result = accountService.existsByCccd("123456789");

        assertTrue(result);
        verify(accountRepository, times(1)).existsByCccdAndVisible("123456789", 1);
    }

    @Test
    @DisplayName("existsByCccd_testChuan2 - CCCD does not exist returns false")
    public void existsByCccd_testChuan2() {
        // Standard case: CCCD does not exist
        when(accountRepository.existsByCccdAndVisible("999999999", 1))
            .thenReturn(false);

        boolean result = accountService.existsByCccd("999999999");

        assertFalse(result);
    }

    // ==================== Test existsByEmail ====================

    @Test
    @DisplayName("existsByEmail_testChuan1 - Email exists returns true")
    public void existsByEmail_testChuan1() {
        // Standard case: email exists
        when(accountRepository.existsByEmailAndVisible("test@example.com", 1))
            .thenReturn(true);

        boolean result = accountService.existsByEmail("test@example.com");

        assertTrue(result);
        verify(accountRepository, times(1)).existsByEmailAndVisible("test@example.com", 1);
    }

    @Test
    @DisplayName("existsByEmail_testChuan2 - Email does not exist returns false")
    public void existsByEmail_testChuan2() {
        // Standard case: email does not exist
        when(accountRepository.existsByEmailAndVisible("notexist@example.com", 1))
            .thenReturn(false);

        boolean result = accountService.existsByEmail("notexist@example.com");

        assertFalse(result);
    }

    @Test
    @DisplayName("existsByEmail_testChuan3 - Null email returns false without query")
    public void existsByEmail_testChuan3() {
        // Edge case: null email should return false without DB call
        boolean result = accountService.existsByEmail(null);

        assertFalse(result);
        verify(accountRepository, never()).existsByEmailAndVisible(anyString(), anyInt());
    }

    @Test
    @DisplayName("existsByEmail_testChuan4 - Empty email returns false without query")
    public void existsByEmail_testChuan4() {
        // Edge case: empty email should return false without DB call
        boolean result = accountService.existsByEmail("   ");

        assertFalse(result);
        verify(accountRepository, never()).existsByEmailAndVisible(anyString(), anyInt());
    }

    // ==================== Test findByCccd ====================

    @Test
    @DisplayName("findByCccd_testChuan1 - Find account by CCCD returns present optional")
    public void findByCccd_testChuan1() {
        // Standard case: account found
        when(accountRepository.findByCccdAndVisible("123456789", 1))
            .thenReturn(Optional.of(testAccount));

        Optional<Account> result = accountService.findByCccd("123456789");

        assertTrue(result.isPresent());
        assertEquals("123456789", result.get().getCccd());
    }

    @Test
    @DisplayName("findByCccd_testChuan2 - Find non-existent CCCD returns empty optional")
    public void findByCccd_testChuan2() {
        // Standard case: account not found
        when(accountRepository.findByCccdAndVisible("999999999", 1))
            .thenReturn(Optional.empty());

        Optional<Account> result = accountService.findByCccd("999999999");

        assertFalse(result.isPresent());
    }

    // ==================== Test findByEmail ====================

    @Test
    @DisplayName("findByEmail_testChuan1 - Find account by email returns present optional")
    public void findByEmail_testChuan1() {
        // Standard case: account found
        when(accountRepository.findByEmailAndVisible("test@example.com", 1))
            .thenReturn(Optional.of(testAccount));

        Optional<Account> result = accountService.findByEmail("test@example.com");

        assertTrue(result.isPresent());
        assertEquals("test@example.com", result.get().getEmail());
    }

    @Test
    @DisplayName("findByEmail_testChuan2 - Find non-existent email returns empty optional")
    public void findByEmail_testChuan2() {
        // Standard case: account not found
        when(accountRepository.findByEmailAndVisible("notexist@example.com", 1))
            .thenReturn(Optional.empty());

        Optional<Account> result = accountService.findByEmail("notexist@example.com");

        assertFalse(result.isPresent());
    }

    // ==================== Test findStudentByCccd ====================

    @Test
    @DisplayName("findStudentByCccd_testChuan1 - Find student by CCCD returns student account")
    public void findStudentByCccd_testChuan1() {
        // Standard case: student found
        testAccount.setRole(Role.STUDENT);
        when(accountRepository.findByCccdAndVisibleAndRole("123456789", 1, Role.STUDENT))
            .thenReturn(Optional.of(testAccount));

        Optional<Account> result = accountService.findStudentByCccd("123456789");

        assertTrue(result.isPresent());
        assertEquals(Role.STUDENT, result.get().getRole());
    }

    @Test
    @DisplayName("findStudentByCccd_testChuan2 - Find non-existent student returns empty optional")
    public void findStudentByCccd_testChuan2() {
        // Standard case: student not found
        when(accountRepository.findByCccdAndVisibleAndRole("999999999", 1, Role.STUDENT))
            .thenReturn(Optional.empty());

        Optional<Account> result = accountService.findStudentByCccd("999999999");

        assertFalse(result.isPresent());
    }

    // ==================== Test findTeacherByCccd ====================

    @Test
    @DisplayName("findTeacherByCccd_testChuan1 - Find teacher by CCCD returns teacher account")
    public void findTeacherByCccd_testChuan1() {
        // Standard case: teacher found
        testAccount.setRole(Role.TEACHER);
        when(accountRepository.findByCccdAndVisibleAndRole("123456789", 1, Role.TEACHER))
            .thenReturn(Optional.of(testAccount));

        Optional<Account> result = accountService.findTeacherByCccd("123456789");

        assertTrue(result.isPresent());
        assertEquals(Role.TEACHER, result.get().getRole());
    }

    @Test
    @DisplayName("findTeacherByCccd_testChuan2 - Find non-existent teacher returns empty optional")
    public void findTeacherByCccd_testChuan2() {
        // Standard case: teacher not found
        when(accountRepository.findByCccdAndVisibleAndRole("999999999", 1, Role.TEACHER))
            .thenReturn(Optional.empty());

        Optional<Account> result = accountService.findTeacherByCccd("999999999");

        assertFalse(result.isPresent());
    }

    // ==================== Test deleteAccount ====================

    @Test
    @DisplayName("deleteAccount_testChuan1 - Delete account successfully sets visible to 0")
    public void deleteAccount_testChuan1() {
        // Standard case: delete account (soft delete)
        testAccount.setVisible(1);
        when(accountRepository.findById(1L))
            .thenReturn(Optional.of(testAccount));
        when(accountRepository.save(any(Account.class)))
            .thenReturn(testAccount);

        Boolean result = accountService.deleteAccount(1L);

        assertTrue(result);
        assertEquals(0, testAccount.getVisible());
        verify(accountRepository, times(1)).findById(1L);
        verify(accountRepository, times(1)).save(testAccount);
    }

    @Test
    @DisplayName("deleteAccount_ngoaile1 - Throw exception when account not found")
    public void deleteAccount_ngoaile1() {
        // Error case: account not found
        when(accountRepository.findById(999L))
            .thenReturn(Optional.empty());

        assertThrows(EntityNotFoundException.class, () -> {
            accountService.deleteAccount(999L);
        });

        verify(accountRepository, times(1)).findById(999L);
        verify(accountRepository, never()).save(any(Account.class));
    }

    @Test
    @DisplayName("deleteAccount_ngoaile2 - Throw exception when account already deleted")
    public void deleteAccount_ngoaile2() {
        // Error case: account already deleted (visible = 0)
        testAccount.setVisible(0);
        when(accountRepository.findById(1L))
            .thenReturn(Optional.of(testAccount));

        assertThrows(IllegalArgumentException.class, () -> {
            accountService.deleteAccount(1L);
        });

        verify(accountRepository, times(1)).findById(1L);
        verify(accountRepository, never()).save(any(Account.class));
    }

    // ==================== Test createAccount ====================

    @Test
    @DisplayName("createAccount_testChuan1 - Create account successfully with generated username and password")
    public void createAccount_testChuan1() {
        // Standard case: create new account
        Account newAccount = new Account();
        newAccount.setFirstName("John");
        newAccount.setLastName("Doe");
        newAccount.setCccd("111111111");
        newAccount.setEmail("john@example.com");
        newAccount.setRole(Role.STUDENT);

        when(accountRepository.existsByCccdAndVisible("111111111", 1))
            .thenReturn(false);
        when(accountRepository.existsByEmailAndVisible("john@example.com", 1))
            .thenReturn(false);
        when(usernameGenerator.generateUsername("John", "Doe"))
            .thenReturn("johndoe");
        when(usernameGenerator.getDefaultPassword())
            .thenReturn("123456Aa@");
        when(passwordEncoder.encode("123456Aa@"))
            .thenReturn("encodedPassword");
        when(accountRepository.save(any(Account.class)))
            .thenReturn(newAccount);

        Account result = accountService.createAccount(newAccount);

        assertNotNull(result);
        verify(accountRepository, times(1)).existsByCccdAndVisible("111111111", 1);
        verify(accountRepository, times(1)).existsByEmailAndVisible("john@example.com", 1);
        verify(usernameGenerator, times(1)).generateUsername("John", "Doe");
        verify(usernameGenerator, times(1)).getDefaultPassword();
        verify(passwordEncoder, times(1)).encode("123456Aa@");
        verify(accountRepository, times(1)).save(any(Account.class));
    }

    @Test
    @DisplayName("createAccount_ngoaile1 - Throw exception when CCCD already exists")
    public void createAccount_ngoaile1() {
        // Error case: CCCD already exists
        Account newAccount = new Account();
        newAccount.setCccd("111111111");
        newAccount.setEmail("john@example.com");
        newAccount.setFirstName("John");
        newAccount.setLastName("Doe");

        when(accountRepository.existsByCccdAndVisible("111111111", 1))
            .thenReturn(true);

        assertThrows(IllegalArgumentException.class, () -> {
            accountService.createAccount(newAccount);
        });

        verify(accountRepository, times(1)).existsByCccdAndVisible("111111111", 1);
        verify(accountRepository, never()).save(any(Account.class));
    }

    @Test
    @DisplayName("createAccount_ngoaile2 - Throw exception when email already exists")
    public void createAccount_ngoaile2() {
        // Error case: email already exists
        Account newAccount = new Account();
        newAccount.setCccd("111111111");
        newAccount.setEmail("john@example.com");
        newAccount.setFirstName("John");
        newAccount.setLastName("Doe");

        when(accountRepository.existsByCccdAndVisible("111111111", 1))
            .thenReturn(false);
        when(accountRepository.existsByEmailAndVisible("john@example.com", 1))
            .thenReturn(true);

        assertThrows(IllegalArgumentException.class, () -> {
            accountService.createAccount(newAccount);
        });

        verify(accountRepository, times(1)).existsByCccdAndVisible("111111111", 1);
        verify(accountRepository, times(1)).existsByEmailAndVisible("john@example.com", 1);
        verify(accountRepository, never()).save(any(Account.class));
    }
}
