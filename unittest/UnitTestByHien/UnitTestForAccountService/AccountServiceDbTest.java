package com.example.accountservice.service;

import com.example.accountservice.dto.AccountSearchDTO;
import com.example.accountservice.model.Account;
import com.example.accountservice.repository.AccountRepository;
import com.example.accountservice.util.UsernameGenerator;
import jakarta.persistence.EntityNotFoundException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.SpringBootConfiguration;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest(classes = AccountServiceDbTest.JpaTestApplication.class, properties = {
        "spring.datasource.url=jdbc:h2:mem:account_service_db_test;MODE=MySQL;DB_CLOSE_DELAY=-1;DB_CLOSE_ON_EXIT=FALSE",
        "spring.datasource.driver-class-name=org.h2.Driver",
        "spring.datasource.username=sa",
        "spring.datasource.password=",
        "spring.jpa.hibernate.ddl-auto=create-drop",
        "spring.jpa.database-platform=org.hibernate.dialect.H2Dialect",
        "spring.jpa.properties.hibernate.dialect=org.hibernate.dialect.H2Dialect",
        "spring.jpa.show-sql=false",
        "eureka.client.enabled=false",
        "spring.kafka.bootstrap-servers=",
        "spring.autoconfigure.exclude=org.springframework.boot.autoconfigure.kafka.KafkaAutoConfiguration"
}, webEnvironment = SpringBootTest.WebEnvironment.NONE)
@Import({ AccountService.class, UsernameGenerator.class })
@Transactional
@ActiveProfiles("test")
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.ANY)
@DisplayName("AccountService Integration Tests - H2 + Transaction Rollback")
class AccountServiceDbTest {

    @SpringBootConfiguration
    @EnableAutoConfiguration
    @EntityScan(basePackages = "com.example.accountservice.model")
    @EnableJpaRepositories(basePackages = "com.example.accountservice.repository")
    static class JpaTestApplication {
        @Bean
        PasswordEncoder passwordEncoder() {
            return new BCryptPasswordEncoder();
        }
    }

    @Autowired
    private AccountService serviceUnderTest;

    @Autowired
    private AccountRepository accountRepository;

    /**
     * TestCaseID: UN_AS_DB_001
     * Mục tiêu:
     * - Kiểm chứng getAccountByUsername đọc đúng tài khoản visible = 1 trong DB H2.
     * Given:
     * - Đã có account lưu trong DB với username hợp lệ.
     * When:
     * - Gọi getAccountByUsername(username).
     * Then:
     * - Trả về Optional chứa account tương ứng.
     * CheckDB:
     * - Xác minh dữ liệu account đã tồn tại trước khi gọi service.
     * - Kết quả service khớp ID/username bản ghi DB.
     * Rollback:
     * - Dữ liệu test rollback tự động sau khi test kết thúc do @Transactional.
     */
    @Test
    void getAccountByUsername_shouldReturnPersistedVisibleAccount() {
        Account savedAccount = createPersistedAccount("db_user_001", "db001@example.com", "111111111111", 1);

        Optional<Account> actualAccountOpt = serviceUnderTest.getAccountByUsername("db_user_001");

        assertTrue(actualAccountOpt.isPresent());
        assertEquals(savedAccount.getId(), actualAccountOpt.get().getId());
        assertEquals("db_user_001", actualAccountOpt.get().getUsername());
    }

    /**
     * TestCaseID: UN_AS_DB_002
     * Mục tiêu:
     * - Kiểm chứng existsByEmail truy vấn DB và trả về true với email đang tồn tại.
     * CheckDB:
     * - Tạo account trong H2 trước test.
     * - Gọi service và xác minh kết quả phản ánh đúng dữ liệu DB.
     * Rollback:
     * - Không lưu dư dữ liệu sau test vì transaction rollback.
     */
    @Test
    void existsByEmail_shouldReturnTrue_whenEmailExistsInDatabase() {
        createPersistedAccount("db_user_002", "db002@example.com", "222222222222", 1);

        boolean actualResult = serviceUnderTest.existsByEmail("db002@example.com");

        assertTrue(actualResult);
    }

    /**
     * TestCaseID: UN_AS_DB_003
     * Mục tiêu:
     * - Kiểm chứng createAccount tạo mới bản ghi thật trong DB và mã hóa password.
     * Given:
     * - CCCD và email chưa tồn tại trong DB.
     * When:
     * - Gọi createAccount với thông tin hợp lệ.
     * Then:
     * - DB phát sinh account mới với username được sinh tự động.
     * - Password lưu trong DB là chuỗi đã mã hóa.
     * CheckDB:
     * - Tìm lại account theo username/email và xác minh dữ liệu.
     * Rollback:
     * - Bản ghi tạo mới sẽ bị rollback sau test.
     */
    @Test
    void createAccount_shouldPersistAccountAndEncodePassword() {
        Account accountToCreate = new Account();
        accountToCreate.setFirstName("An");
        accountToCreate.setLastName("Nguyen Van");
        accountToCreate.setEmail("db003@example.com");
        accountToCreate.setCccd("333333333333");
        accountToCreate.setVisible(1);

        Account createdAccount = serviceUnderTest.createAccount(accountToCreate);

        assertNotNull(createdAccount.getId());
        assertNotNull(createdAccount.getUsername());
        assertNotEquals("123456Aa@", createdAccount.getPassword());
        assertTrue(createdAccount.getPassword().length() > 20);

        Optional<Account> reloadedAccount = accountRepository.findById(createdAccount.getId());
        assertTrue(reloadedAccount.isPresent());
        assertEquals("db003@example.com", reloadedAccount.get().getEmail());
        assertEquals("333333333333", reloadedAccount.get().getCccd());
    }

    /**
     * TestCaseID: UN_AS_DB_004
     * Mục tiêu:
     * - Kiểm chứng updateAccount cập nhật dữ liệu thật trong DB và giữ nguyên
     * password/visible.
     * Given:
     * - DB có account gốc và request update hợp lệ cùng id/username.
     * Then:
     * - firstName/lastName/email được cập nhật.
     * - password và visible giữ nguyên từ bản ghi cũ.
     * CheckDB:
     * - Đọc lại DB sau update và so sánh dữ liệu.
     * Rollback:
     * - Tất cả thay đổi rollback sau test.
     */
    @Test
    void updateAccount_shouldUpdateFieldsAndKeepPasswordAndVisible() {
        Account existingAccount = createPersistedAccount("db_user_004", "db004.old@example.com", "444444444444", 1);
        existingAccount.setPassword("encoded-old-password");
        existingAccount = accountRepository.save(existingAccount);

        Account updateRequest = new Account();
        updateRequest.setId(existingAccount.getId());
        updateRequest.setUsername(existingAccount.getUsername());
        updateRequest.setFirstName("Updated");
        updateRequest.setLastName("Name");
        updateRequest.setEmail("db004.new@example.com");
        updateRequest.setCccd(existingAccount.getCccd());

        Account updatedAccount = serviceUnderTest.updateAccount(updateRequest);

        assertEquals("Updated", updatedAccount.getFirstName());
        assertEquals("Name", updatedAccount.getLastName());
        assertEquals("db004.new@example.com", updatedAccount.getEmail());
        assertEquals("encoded-old-password", updatedAccount.getPassword());
        assertEquals(1, updatedAccount.getVisible());

        Account reloadedAccount = accountRepository.findById(existingAccount.getId()).orElseThrow();
        assertEquals("db004.new@example.com", reloadedAccount.getEmail());
        assertEquals("encoded-old-password", reloadedAccount.getPassword());
    }

    /**
     * TestCaseID: UN_AS_DB_005
     * Mục tiêu:
     * - Kiểm chứng deleteAccount thực hiện soft delete (visible: 1 -> 0) trên DB.
     * CheckDB:
     * - Đọc lại account sau gọi service và xác minh visible = 0.
     * Rollback:
     * - Dữ liệu soft-delete được rollback sau test.
     */
    @Test
    void deleteAccount_shouldSoftDeleteAccountInDatabase() {
        Account existingAccount = createPersistedAccount("db_user_005", "db005@example.com", "555555555555", 1);

        Boolean actualResult = serviceUnderTest.deleteAccount(existingAccount.getId());

        assertTrue(actualResult);
        Account reloadedAccount = accountRepository.findById(existingAccount.getId()).orElseThrow();
        assertEquals(0, reloadedAccount.getVisible());
    }

    /**
     * TestCaseID: UN_AS_DB_006
     * Mục tiêu:
     * - Kiểm chứng saveAccount set visible=1 khi giá trị visible đang null và
     * persist DB.
     * CheckDB:
     * - Bản ghi đọc lại từ DB phải có visible = 1.
     * Rollback:
     * - Bản ghi test rollback sau khi test kết thúc.
     */
    @Test
    void saveAccount_shouldSetVisibleOneAndPersist_whenVisibleIsNull() {
        Account accountToSave = new Account();
        accountToSave.setUsername("db_user_006");
        accountToSave.setPassword("encoded-pass-006");
        accountToSave.setFirstName("Save");
        accountToSave.setLastName("Case");
        accountToSave.setEmail("db006@example.com");
        accountToSave.setCccd("666666666666");
        accountToSave.setVisible(null);

        Account savedAccount = serviceUnderTest.saveAccount(accountToSave);

        assertNotNull(savedAccount.getId());
        assertEquals(1, savedAccount.getVisible());

        Account reloadedAccount = accountRepository.findById(savedAccount.getId()).orElseThrow();
        assertEquals(1, reloadedAccount.getVisible());
    }

    /**
     * TestCaseID: UN_AS_DB_007
     * Mục tiêu:
     * - Kiểm chứng universalSearch đọc dữ liệu đúng từ DB với tiêu chí tìm kiếm.
     * Given:
     * - DB có account visible=1 chứa keyword cần tìm.
     * When:
     * - Gọi universalSearch với keyword + searchFields.
     * Then:
     * - Trả về Page không rỗng và chứa account phù hợp.
     * CheckDB:
     * - Kết quả truy vấn khớp bản ghi thật trong H2.
     * Rollback:
     * - Chỉ đọc DB, không tạo thay đổi bền vững sau test.
     */
    @Test
    void universalSearch_shouldReturnMatchingAccountsFromDatabase() {
        createPersistedAccount("db_user_007_alice", "db007@example.com", "777777777777", 1);

        AccountSearchDTO searchDTO = new AccountSearchDTO();
        searchDTO.setKeyword("alice");
        searchDTO.setSearchFields(List.of("username"));

        Pageable pageable = PageRequest.of(0, 10);
        Page<Account> resultPage = serviceUnderTest.universalSearch(searchDTO, pageable);

        assertFalse(resultPage.isEmpty());
        assertTrue(resultPage.getContent().stream()
                .anyMatch(account -> "db_user_007_alice".equals(account.getUsername())));
    }

    /**
     * TestCaseID: UN_AS_DB_008
     * Mục tiêu:
     * - Kiểm chứng getAccountsByIds chỉ trả về account visible=1 theo danh sách id.
     * Given:
     * - Có account visible=1 và visible=0 trong DB.
     * When:
     * - Gọi getAccountsByIds([id1, id2]).
     * Then:
     * - Chỉ trả account visible=1.
     * CheckDB:
     * - Kết quả đọc DB phải lọc đúng theo điều kiện visible = 1.
     * Rollback:
     * - Dữ liệu test rollback tự động.
     */
    @Test
    void getAccountsByIds_shouldReturnOnlyVisibleAccounts() {
        Account visibleAccount = createPersistedAccount("db_user_008_visible", "db008a@example.com", "888888888881", 1);
        Account hiddenAccount = createPersistedAccount("db_user_008_hidden", "db008b@example.com", "888888888882", 0);

        List<Account> resultAccounts = serviceUnderTest.getAccountsByIds(
                List.of(visibleAccount.getId(), hiddenAccount.getId()));

        assertEquals(1, resultAccounts.size());
        assertEquals(visibleAccount.getId(), resultAccounts.get(0).getId());
    }

    /**
     * TestCaseID: UN_AS_DB_009
     * Mục tiêu:
     * - Kiểm chứng universalSearch ném EntityNotFoundException khi không có dữ liệu
     * phù hợp.
     * CheckDB:
     * - Đảm bảo dataset không chứa keyword cần tìm trước khi gọi.
     * - Service ném đúng exception theo đặc tả.
     * Rollback:
     * - Không có thay đổi DB thực tế.
     */
    @Test
    void universalSearch_shouldThrowEntityNotFoundException_whenNoMatchInDatabase() {
        createPersistedAccount("db_user_009", "db009@example.com", "999999999999", 1);

        AccountSearchDTO searchDTO = new AccountSearchDTO();
        searchDTO.setKeyword("not_found_keyword");
        searchDTO.setSearchFields(List.of("username"));

        Pageable pageable = PageRequest.of(0, 10);

        assertThrows(EntityNotFoundException.class, () -> serviceUnderTest.universalSearch(searchDTO, pageable));
    }

    private Account createPersistedAccount(String username, String email, String cccd, Integer visible) {
        Account account = new Account();
        account.setUsername(username);
        account.setPassword("encoded-password");
        account.setFirstName("First");
        account.setLastName("Last");
        account.setEmail(email);
        account.setCccd(cccd);
        account.setVisible(visible);
        return accountRepository.save(account);
    }
}
