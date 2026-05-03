package com.example.accountservice.service;

import com.example.accountservice.dto.AccountSearchDTO;
import com.example.accountservice.model.Account;
import com.example.accountservice.repository.AccountRepository;
import com.example.accountservice.util.UsernameGenerator;
import jakarta.persistence.EntityNotFoundException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("AccountService Unit Tests - Branch Coverage Level 2")
class AccountServiceTest {

    @Mock
    private AccountRepository mockAccountRepository;

    @Mock
    private UsernameGenerator mockUsernameGenerator;

    @Mock
    private PasswordEncoder mockPasswordEncoder;

    @Mock
    private ApplicationEventPublisher mockApplicationEventPublisher;

    @InjectMocks
    private AccountService serviceUnderTest;

    /**
     * TestCaseID: UN_AS_001
     * Mục tiêu:
     * - Xác minh getAccountByUsername trả đúng dữ liệu khi tài khoản tồn tại và
     * visible = 1.
     * Given:
     * - Repository trả Optional chứa account.
     * Then:
     * - Service trả lại đúng Optional đã tìm được.
     * CheckDB:
     * - Kiểm tra truy cập DB đúng method findByUsernameAndVisible(username, 1).
     * Rollback:
     * - Unit test dùng mock, không thay đổi dữ liệu DB thật.
     */
    @Test
    void getAccountByUsername_shouldReturnAccount_whenAccountExists() {
        Account existingAccount = new Account();
        existingAccount.setId(1L);
        existingAccount.setUsername("alice");

        when(mockAccountRepository.findByUsernameAndVisible("alice", 1))
                .thenReturn(Optional.of(existingAccount));

        Optional<Account> actualAccountOpt = serviceUnderTest.getAccountByUsername("alice");

        assertTrue(actualAccountOpt.isPresent());
        assertSame(existingAccount, actualAccountOpt.get());
        verify(mockAccountRepository, times(1)).findByUsernameAndVisible("alice", 1);
    }

    /**
     * TestCaseID: UN_AS_002
     * Mục tiêu:
     * - Xác minh getAccountByUsername ném EntityNotFoundException khi không tìm
     * thấy tài khoản.
     * Given:
     * - Repository trả Optional.empty().
     * Then:
     * - Service ném EntityNotFoundException theo đặc tả.
     * CheckDB:
     * - Kiểm tra đã truy vấn findByUsernameAndVisible(username, 1).
     * Rollback:
     * - Không có thay đổi DB.
     */
    @Test
    void getAccountByUsername_shouldThrowEntityNotFoundException_whenAccountDoesNotExist() {
        when(mockAccountRepository.findByUsernameAndVisible("missing-user", 1))
                .thenReturn(Optional.empty());

        assertThrows(EntityNotFoundException.class,
                () -> serviceUnderTest.getAccountByUsername("missing-user"));

        verify(mockAccountRepository, times(1)).findByUsernameAndVisible("missing-user", 1);
    }

    /**
     * TestCaseID: UN_AS_003
     * Mục tiêu:
     * - Xác minh existsByEmail trả false khi email null.
     * Then:
     * - Không truy vấn DB vì input không hợp lệ.
     * CheckDB:
     * - verify repository không bị gọi.
     * Rollback:
     * - Không có thay đổi DB.
     */
    @Test
    void existsByEmail_shouldReturnFalse_whenEmailIsNull() {
        boolean actualResult = serviceUnderTest.existsByEmail(null);

        assertFalse(actualResult);
        verify(mockAccountRepository, never()).existsByEmailAndVisible(any(), eq(1));
    }

    /**
     * TestCaseID: UN_AS_004
     * Mục tiêu:
     * - Xác minh existsByEmail trả false khi email chỉ chứa khoảng trắng.
     * Then:
     * - Không truy vấn DB vì input không hợp lệ.
     * CheckDB:
     * - verify repository không bị gọi.
     * Rollback:
     * - Không có thay đổi DB.
     */
    @Test
    void existsByEmail_shouldReturnFalse_whenEmailIsBlank() {
        boolean actualResult = serviceUnderTest.existsByEmail("   ");

        assertFalse(actualResult);
        verify(mockAccountRepository, never()).existsByEmailAndVisible(any(), eq(1));
    }

    /**
     * TestCaseID: UN_AS_005
     * Mục tiêu:
     * - Xác minh existsByEmail truy vấn DB và trả true khi email đã tồn tại.
     * CheckDB:
     * - verify gọi existsByEmailAndVisible(email, 1).
     * Rollback:
     * - Không có thay đổi DB.
     */
    @Test
    void existsByEmail_shouldReturnTrue_whenEmailExists() {
        when(mockAccountRepository.existsByEmailAndVisible("alice@example.com", 1)).thenReturn(true);

        boolean actualResult = serviceUnderTest.existsByEmail("alice@example.com");

        assertTrue(actualResult);
        verify(mockAccountRepository, times(1)).existsByEmailAndVisible("alice@example.com", 1);
    }

    /**
     * TestCaseID: UN_AS_006
     * Mục tiêu:
     * - Xác minh deleteAccount ném EntityNotFoundException khi id không tồn tại.
     * CheckDB:
     * - Truy cập findById(id), không được save.
     * Rollback:
     * - Không có thay đổi DB.
     */
    @Test
    void deleteAccount_shouldThrowEntityNotFoundException_whenAccountIdDoesNotExist() {
        when(mockAccountRepository.findById(10L)).thenReturn(Optional.empty());

        assertThrows(EntityNotFoundException.class, () -> serviceUnderTest.deleteAccount(10L));
        verify(mockAccountRepository, times(1)).findById(10L);
        verify(mockAccountRepository, never()).save(any(Account.class));
    }

    /**
     * TestCaseID: UN_AS_007
     * Mục tiêu:
     * - Xác minh deleteAccount ném IllegalArgumentException khi account đã
     * soft-deleted.
     * CheckDB:
     * - Truy cập findById(id), không được save vì trạng thái không hợp lệ.
     * Rollback:
     * - Không có thay đổi DB.
     */
    @Test
    void deleteAccount_shouldThrowIllegalArgumentException_whenAccountAlreadyDeleted() {
        Account deletedAccount = new Account();
        deletedAccount.setId(11L);
        deletedAccount.setVisible(0);

        when(mockAccountRepository.findById(11L)).thenReturn(Optional.of(deletedAccount));

        assertThrows(IllegalArgumentException.class, () -> serviceUnderTest.deleteAccount(11L));
        verify(mockAccountRepository, times(1)).findById(11L);
        verify(mockAccountRepository, never()).save(any(Account.class));
    }

    /**
     * TestCaseID: UN_AS_008
     * Mục tiêu:
     * - Xác minh deleteAccount thực hiện soft delete thành công.
     * Given:
     * - Account visible = 1.
     * Then:
     * - visible được set thành 0 và save về repository.
     * CheckDB:
     * - verify save(account) được gọi đúng 1 lần, trạng thái visible sau save là 0.
     * Rollback:
     * - Unit test mock repository, không commit dữ liệu thật.
     */
    @Test
    void deleteAccount_shouldSoftDeleteAndReturnTrue_whenAccountIsActive() {
        Account activeAccount = new Account();
        activeAccount.setId(12L);
        activeAccount.setVisible(1);

        when(mockAccountRepository.findById(12L)).thenReturn(Optional.of(activeAccount));
        when(mockAccountRepository.save(activeAccount)).thenReturn(activeAccount);

        Boolean actualResult = serviceUnderTest.deleteAccount(12L);

        assertTrue(actualResult);
        assertEquals(0, activeAccount.getVisible());
        verify(mockAccountRepository, times(1)).findById(12L);
        verify(mockAccountRepository, times(1)).save(activeAccount);
    }

    /**
     * TestCaseID: UN_AS_009
     * Mục tiêu:
     * - Xác minh createAccount chặn tạo mới khi CCCD đã tồn tại.
     * CheckDB:
     * - verify có check existsByCccdAndVisible và không save.
     * Rollback:
     * - Không có thay đổi DB.
     */
    @Test
    void createAccount_shouldThrowIllegalArgumentException_whenCccdAlreadyExists() {
        Account newAccount = new Account();
        newAccount.setCccd("012345678901");

        when(mockAccountRepository.existsByCccdAndVisible("012345678901", 1)).thenReturn(true);

        assertThrows(IllegalArgumentException.class, () -> serviceUnderTest.createAccount(newAccount));
        verify(mockAccountRepository, times(1)).existsByCccdAndVisible("012345678901", 1);
        verify(mockAccountRepository, never()).save(any(Account.class));
    }

    /**
     * TestCaseID: UN_AS_010
     * Mục tiêu:
     * - Xác minh createAccount chặn tạo mới khi email đã tồn tại.
     * CheckDB:
     * - verify check existsByCccd, existsByEmail và không save.
     * Rollback:
     * - Không có thay đổi DB.
     */
    @Test
    void createAccount_shouldThrowIllegalArgumentException_whenEmailAlreadyExists() {
        Account newAccount = new Account();
        newAccount.setCccd("012345678901");
        newAccount.setEmail("duplicate@example.com");

        when(mockAccountRepository.existsByCccdAndVisible("012345678901", 1)).thenReturn(false);
        when(mockAccountRepository.existsByEmailAndVisible("duplicate@example.com", 1)).thenReturn(true);

        assertThrows(IllegalArgumentException.class, () -> serviceUnderTest.createAccount(newAccount));
        verify(mockAccountRepository, times(1)).existsByCccdAndVisible("012345678901", 1);
        verify(mockAccountRepository, times(1)).existsByEmailAndVisible("duplicate@example.com", 1);
        verify(mockAccountRepository, never()).save(any(Account.class));
    }

    /**
     * TestCaseID: UN_AS_011
     * Mục tiêu:
     * - Xác minh createAccount tạo tài khoản thành công với username sinh tự động
     * và
     * password mã hóa.
     * Given:
     * - Không trùng CCCD, không trùng email.
     * Then:
     * - Service set username/password và save thành công.
     * CheckDB:
     * - verify save(account) được gọi và dữ liệu save đúng theo đặc tả.
     * Rollback:
     * - Không có DB thật, repository được mock.
     */
    @Test
    void createAccount_shouldGenerateUsernameEncodePasswordAndSave_whenInputIsValid() {
        Account newAccount = new Account();
        newAccount.setFirstName("Alice");
        newAccount.setLastName("Nguyen");
        newAccount.setCccd("012345678901");
        newAccount.setEmail("alice@example.com");

        when(mockAccountRepository.existsByCccdAndVisible("012345678901", 1)).thenReturn(false);
        when(mockAccountRepository.existsByEmailAndVisible("alice@example.com", 1)).thenReturn(false);
        when(mockUsernameGenerator.generateUsername("Alice", "Nguyen")).thenReturn("alice.nguyen");
        when(mockUsernameGenerator.getDefaultPassword()).thenReturn("123456Aa@");
        when(mockPasswordEncoder.encode("123456Aa@")).thenReturn("encoded-password");
        when(mockAccountRepository.save(any(Account.class))).thenAnswer(invocation -> invocation.getArgument(0));

        Account savedAccount = serviceUnderTest.createAccount(newAccount);

        assertEquals("alice.nguyen", savedAccount.getUsername());
        assertEquals("encoded-password", savedAccount.getPassword());
        assertEquals(1, savedAccount.getVisible());
        verify(mockUsernameGenerator, times(1)).generateUsername("Alice", "Nguyen");
        verify(mockPasswordEncoder, times(1)).encode("123456Aa@");
        verify(mockAccountRepository, times(1)).save(newAccount);
    }

    /**
     * TestCaseID: UN_AS_012
     * Mục tiêu:
     * - Xác minh updateAccount ném EntityNotFoundException khi username không tồn
     * tại.
     * CheckDB:
     * - verify truy vấn findByUsernameAndVisible và không save.
     * Rollback:
     * - Không có thay đổi DB.
     */
    @Test
    void updateAccount_shouldThrowEntityNotFoundException_whenAccountDoesNotExistByUsername() {
        Account updateRequest = new Account();
        updateRequest.setUsername("unknown-user");

        when(mockAccountRepository.findByUsernameAndVisible("unknown-user", 1)).thenReturn(Optional.empty());

        assertThrows(EntityNotFoundException.class, () -> serviceUnderTest.updateAccount(updateRequest));
        verify(mockAccountRepository, times(1)).findByUsernameAndVisible("unknown-user", 1);
        verify(mockAccountRepository, never()).save(any(Account.class));
    }

    /**
     * TestCaseID: UN_AS_013
     * Mục tiêu:
     * - Xác minh updateAccount chặn khi CCCD mới trùng với account khác.
     * CheckDB:
     * - verify có gọi findByCccdAndVisible và không save.
     * Rollback:
     * - Không có thay đổi DB.
     */
    @Test
    void updateAccount_shouldThrowIllegalArgumentException_whenCccdBelongsToAnotherAccount() {
        Account existingAccount = new Account();
        existingAccount.setId(1L);
        existingAccount.setUsername("alice");
        existingAccount.setPassword("old-encoded-password");
        existingAccount.setVisible(1);

        Account duplicatedCccdAccount = new Account();
        duplicatedCccdAccount.setId(99L);

        Account updateRequest = new Account();
        updateRequest.setId(1L);
        updateRequest.setUsername("alice");
        updateRequest.setCccd("012345678901");

        when(mockAccountRepository.findByUsernameAndVisible("alice", 1)).thenReturn(Optional.of(existingAccount));
        when(mockAccountRepository.findByCccdAndVisible("012345678901", 1))
                .thenReturn(Optional.of(duplicatedCccdAccount));

        assertThrows(IllegalArgumentException.class, () -> serviceUnderTest.updateAccount(updateRequest));
        verify(mockAccountRepository, never()).save(any(Account.class));
    }

    /**
     * TestCaseID: UN_AS_014
     * Mục tiêu:
     * - Xác minh updateAccount chặn khi email mới trùng với account khác.
     * CheckDB:
     * - verify có gọi findByEmailAndVisible và không save.
     * Rollback:
     * - Không có thay đổi DB.
     */
    @Test
    void updateAccount_shouldThrowIllegalArgumentException_whenEmailBelongsToAnotherAccount() {
        Account existingAccount = new Account();
        existingAccount.setId(1L);
        existingAccount.setUsername("alice");
        existingAccount.setPassword("old-encoded-password");
        existingAccount.setVisible(1);

        Account duplicatedEmailAccount = new Account();
        duplicatedEmailAccount.setId(99L);

        Account updateRequest = new Account();
        updateRequest.setId(1L);
        updateRequest.setUsername("alice");
        updateRequest.setCccd("012345678901");
        updateRequest.setEmail("taken@example.com");

        when(mockAccountRepository.findByUsernameAndVisible("alice", 1)).thenReturn(Optional.of(existingAccount));
        when(mockAccountRepository.findByCccdAndVisible("012345678901", 1))
                .thenReturn(Optional.of(updateRequest));
        when(mockAccountRepository.findByEmailAndVisible("taken@example.com", 1))
                .thenReturn(Optional.of(duplicatedEmailAccount));

        assertThrows(IllegalArgumentException.class, () -> serviceUnderTest.updateAccount(updateRequest));
        verify(mockAccountRepository, never()).save(any(Account.class));
    }

    /**
     * TestCaseID: UN_AS_015
     * Mục tiêu:
     * - Xác minh updateAccount thành công và giữ nguyên password/visible từ bản ghi
     * cũ.
     * CheckDB:
     * - verify save(account) gọi đúng 1 lần.
     * - verify account trước khi save giữ password/visible như dữ liệu cũ.
     * Rollback:
     * - Unit test dùng mock nên không commit DB.
     */
    @Test
    void updateAccount_shouldKeepOldPasswordAndVisibleAndSave_whenInputIsValid() {
        Account existingAccount = new Account();
        existingAccount.setId(1L);
        existingAccount.setUsername("alice");
        existingAccount.setPassword("old-encoded-password");
        existingAccount.setVisible(1);

        Account updateRequest = new Account();
        updateRequest.setId(1L);
        updateRequest.setUsername("alice");
        updateRequest.setCccd("012345678901");
        updateRequest.setEmail("alice-new@example.com");
        updateRequest.setFirstName("Alice");

        when(mockAccountRepository.findByUsernameAndVisible("alice", 1)).thenReturn(Optional.of(existingAccount));
        when(mockAccountRepository.findByCccdAndVisible("012345678901", 1)).thenReturn(Optional.of(updateRequest));
        when(mockAccountRepository.findByEmailAndVisible("alice-new@example.com", 1))
                .thenReturn(Optional.of(updateRequest));
        when(mockAccountRepository.save(any(Account.class))).thenAnswer(invocation -> invocation.getArgument(0));

        Account updatedAccount = serviceUnderTest.updateAccount(updateRequest);

        assertEquals("old-encoded-password", updatedAccount.getPassword());
        assertEquals(1, updatedAccount.getVisible());
        verify(mockAccountRepository, times(1)).save(updateRequest);
    }

    /**
     * TestCaseID: UN_AS_016
     * Defect cần ghi nhận:
     * - updateAccount tìm bản ghi cũ bằng username trong body, không đối chiếu với
     * id (id thường gán từ PUT /account/{id}).
     * - Nếu body.username thuộc user khác với id trong body, service vẫn copy
     * password/visible từ user được load theo username rồi save theo id → trộn/ghi
     * đè sai tài khoản.
     * Kỳ vọng nghiệp vụ:
     * - Từ chối cập nhật (IllegalArgumentException) khi id và username không cùng
     * một tài khoản.
     * Hiện trạng:
     * - Test disabled: implementation hiện tại không chặn và vẫn gọi save.
     */
    @Test
    void updateAccount_shouldThrowIllegalArgumentException_whenUsernameDoesNotMatchAccountId() {
        Account userAlice = new Account();
        userAlice.setId(1L);
        userAlice.setUsername("alice");
        userAlice.setCccd("012345678901");
        userAlice.setPassword("pw-alice-encoded");
        userAlice.setVisible(1);

        Account userBob = new Account();
        userBob.setId(2L);
        userBob.setUsername("bob");
        userBob.setCccd("987654321098");
        userBob.setPassword("pw-bob-encoded");
        userBob.setVisible(1);

        Account updateRequest = new Account();
        updateRequest.setId(1L);
        updateRequest.setUsername("bob");
        updateRequest.setCccd("012345678901");
        updateRequest.setEmail("alice@example.com");

        when(mockAccountRepository.findByUsernameAndVisible("bob", 1)).thenReturn(Optional.of(userBob));
        when(mockAccountRepository.findByCccdAndVisible("012345678901", 1)).thenReturn(Optional.of(userAlice));
        when(mockAccountRepository.findByEmailAndVisible("alice@example.com", 1)).thenReturn(Optional.of(userAlice));

        assertThrows(IllegalArgumentException.class, () -> serviceUnderTest.updateAccount(updateRequest));
        verify(mockAccountRepository, never()).save(any(Account.class));
    }

    /**
     * TestCaseID: UN_AS_017
     * Mục tiêu:
     * - Xác minh saveAccount tự set visible = 1 khi visible đang null.
     * CheckDB:
     * - verify accountRepository.save được gọi với visible = 1.
     * Rollback:
     * - Mock repository, không thay đổi DB thật.
     */
    @Test
    void saveAccount_shouldSetVisibleToOne_whenVisibleIsNull() {
        Account accountToSave = new Account();
        accountToSave.setVisible(null);

        when(mockAccountRepository.save(accountToSave)).thenReturn(accountToSave);

        Account savedAccount = serviceUnderTest.saveAccount(accountToSave);

        assertEquals(1, savedAccount.getVisible());
        verify(mockAccountRepository, times(1)).save(accountToSave);
    }

    /**
     * TestCaseID: UN_AS_018
     * Mục tiêu:
     * - Xác minh saveAccount giữ nguyên visible khi đã có giá trị.
     * CheckDB:
     * - verify save được gọi và không bị thay đổi visible hiện có.
     * Rollback:
     * - Không có thay đổi DB thật.
     */
    @Test
    void saveAccount_shouldKeepVisibleValue_whenVisibleAlreadyProvided() {
        Account accountToSave = new Account();
        accountToSave.setVisible(0);

        when(mockAccountRepository.save(accountToSave)).thenReturn(accountToSave);

        Account savedAccount = serviceUnderTest.saveAccount(accountToSave);

        assertEquals(0, savedAccount.getVisible());
        verify(mockAccountRepository, times(1)).save(accountToSave);
    }

    /**
     * TestCaseID: UN_AS_019
     * Mục tiêu:
     * - Xác minh universalSearch ném EntityNotFoundException khi không có kết quả.
     * CheckDB:
     * - verify findAll(spec, pageable) được gọi đúng 1 lần.
     * Rollback:
     * - Đây là thao tác đọc, không có thay đổi DB.
     */
    @Test
    void universalSearch_shouldThrowEntityNotFoundException_whenNoResultsFound() {
        AccountSearchDTO searchDTO = new AccountSearchDTO();
        searchDTO.setKeyword("nobody");
        searchDTO.setPositionIds(new ArrayList<>(List.of(1L)));
        searchDTO.setSearchFields(new ArrayList<>(List.of("username")));

        Pageable pageable = PageRequest.of(0, 10, Sort.by("id"));

        when(mockAccountRepository.findAll(any(Specification.class), any(Pageable.class)))
                .thenReturn(Page.empty(pageable));

        assertThrows(EntityNotFoundException.class, () -> serviceUnderTest.universalSearch(searchDTO, pageable));
        verify(mockAccountRepository, times(1)).findAll(any(Specification.class), any(Pageable.class));
    }

    /**
     * TestCaseID: UN_AS_020
     * Mục tiêu:
     * - Xác minh universalSearch trả Page khi có dữ liệu.
     * - Xác minh pageable được thêm sort createdAt DESC theo đặc tả service.
     * CheckDB:
     * - verify findAll(spec, pageable) và kiểm tra pageable truyền vào repository.
     * Rollback:
     * - Đây là thao tác đọc, không làm thay đổi DB.
     */
    @Test
    void universalSearch_shouldReturnPageAndAppendCreatedAtDescendingSort_whenResultsExist() {
        AccountSearchDTO searchDTO = new AccountSearchDTO();
        searchDTO.setKeyword("alice");
        searchDTO.setPositionIds(new ArrayList<>(List.of(1L)));
        searchDTO.setSearchFields(new ArrayList<>(List.of("username")));

        Pageable inputPageable = PageRequest.of(0, 5, Sort.by(Sort.Direction.ASC, "id"));

        Account account = new Account();
        account.setId(1L);
        Page<Account> expectedPage = new PageImpl<>(List.of(account), inputPageable, 1);

        when(mockAccountRepository.findAll(any(Specification.class), any(Pageable.class)))
                .thenReturn(expectedPage);

        Page<Account> actualPage = serviceUnderTest.universalSearch(searchDTO, inputPageable);

        assertEquals(1, actualPage.getTotalElements());
        ArgumentCaptor<Pageable> pageableCaptor = ArgumentCaptor.forClass(Pageable.class);
        verify(mockAccountRepository, times(1)).findAll(any(Specification.class), pageableCaptor.capture());

        Pageable pageableSentToRepository = pageableCaptor.getValue();
        assertNotNull(pageableSentToRepository.getSort().getOrderFor("createdAt"));
        assertEquals(Sort.Direction.DESC, pageableSentToRepository.getSort().getOrderFor("createdAt").getDirection());
    }

    /**
     * TestCaseID: UN_AS_021
     * Mục tiêu:
     * - Xác minh getAccountsByIds trả danh sách rỗng khi ids = null.
     * CheckDB:
     * - Không được gọi repository.findAll do đầu vào không hợp lệ.
     * Rollback:
     * - Không có thay đổi DB.
     */
    @Test
    void getAccountsByIds_shouldReturnEmptyList_whenIdsIsNull() {
        List<Account> actualAccounts = serviceUnderTest.getAccountsByIds(null);

        assertTrue(actualAccounts.isEmpty());
        verify(mockAccountRepository, never()).findAll(any(Specification.class));
    }

    /**
     * TestCaseID: UN_AS_022
     * Mục tiêu:
     * - Xác minh getAccountsByIds trả danh sách rỗng khi ids trống.
     * CheckDB:
     * - Không được gọi repository.findAll.
     * Rollback:
     * - Không có thay đổi DB.
     */
    @Test
    void getAccountsByIds_shouldReturnEmptyList_whenIdsIsEmpty() {
        List<Account> actualAccounts = serviceUnderTest.getAccountsByIds(Collections.emptyList());

        assertTrue(actualAccounts.isEmpty());
        verify(mockAccountRepository, never()).findAll(any(Specification.class));
    }

    /**
     * TestCaseID: UN_AS_023
     * Mục tiêu:
     * - Xác minh getAccountsByIds trả đúng danh sách account visible = 1 theo ids.
     * CheckDB:
     * - verify repository.findAll(specification) được gọi đúng 1 lần.
     * Rollback:
     * - Đây là thao tác đọc, không thay đổi dữ liệu DB.
     */
    @Test
    void getAccountsByIds_shouldReturnAccounts_whenIdsProvided() {
        List<Long> ids = List.of(1L, 2L, 3L);
        List<Account> expectedAccounts = List.of(new Account(), new Account());

        when(mockAccountRepository.findAll(any(Specification.class))).thenReturn(expectedAccounts);

        List<Account> actualAccounts = serviceUnderTest.getAccountsByIds(ids);

        assertEquals(2, actualAccounts.size());
        assertSame(expectedAccounts, actualAccounts);
        verify(mockAccountRepository, times(1)).findAll(any(Specification.class));
    }
}
