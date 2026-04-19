package com.example.accountservice.service;

import java.util.Collections;
import java.util.List;
import java.util.Optional;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import com.example.accountservice.dto.AccountSearchDTO;
import com.example.accountservice.dto.PasswordChangeDTO;
import com.example.accountservice.enums.Role;
import com.example.accountservice.model.Account;
import com.example.accountservice.repository.AccountRepository;
import com.example.accountservice.specification.AccountSpecification;
import com.example.accountservice.util.ValidateUtil;
import com.example.accountservice.util.UsernameGenerator;

import jakarta.persistence.EntityNotFoundException;
import jakarta.persistence.criteria.Predicate;
import jakarta.transaction.Transactional;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
public class AccountService {

    @Autowired
    private AccountRepository accountRepository;

    @Autowired
    private UsernameGenerator usernameGenerator;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private ApplicationEventPublisher applicationEventPublisher;

    // Lấy tài khoản theo ID, chỉ lấy tài khoản chưa xóa (visible = 1)
    // Trả về exception nếu không tìm thấy (do sai id hoặc tài khoản đã bị xóa)
    public Optional<Account> getAccountByUsername(String un) {
        Optional<Account> accountOpt = accountRepository.findByUsernameAndVisible(un, 1);
        if (accountOpt.isEmpty()) {
            throw new EntityNotFoundException("Account not found with username: " + un);
        }
        return accountOpt;
    }

    // Lấy tài khoản theo tên người dùng, chỉ lấy tài khoản chưa xóa (visible = 1)
    public Optional<Account> findByUsernameAndVisible(String username) {
        return accountRepository.findByUsernameAndVisible(username, 1);
    }

    // Kiểm tra có trùng CCCD không
    public boolean existsByCccd(String cccd) {
        return accountRepository.existsByCccdAndVisible(cccd, 1);
    }

    // Kiểm tra có trùng email không
    public boolean existsByEmail(String email) {
        if (email == null || email.trim().isEmpty()) {
            return false;
        }
        return accountRepository.existsByEmailAndVisible(email, 1);
    }

    // Tìm kiếm theo CCCD
    public Optional<Account> findByCccd(String cccd) {
        return accountRepository.findByCccdAndVisible(cccd, 1);
    }

    // Tìm kiếm theo email
    public Optional<Account> findByEmail(String email) {
        return accountRepository.findByEmailAndVisible(email, 1);
    }

    public Optional<Account> findStudentByCccd(String cccd) {
        return accountRepository.findByCccdAndVisibleAndRole(cccd, 1, Role.STUDENT);
    }

    public Optional<Account> findTeacherByCccd(String cccd) {
        return accountRepository.findByCccdAndVisibleAndRole(cccd, 1, Role.TEACHER);
    }

    /*
     * SOFT DELETE
     * Chức năng: Xóa mềm tài khoản (set visible = 0)
     * Conditions:
     * - Tài khoản tồn tại và chưa bị xóa (visible = 1)
     * Exception:
     * - Tài khoản không tồn tại: trả về EntityNotFoundException
     * - Tài khoản đã bị xóa (visible = 0): trả về IllegalArgumentException
     */
    @Transactional
    public Boolean deleteAccount(Long id) {
        Optional<Account> accountOpt = accountRepository.findById(id);
        if (accountOpt.isEmpty()) {
            throw new EntityNotFoundException("Account not found with id: " + id);
        }
        Account account = accountOpt.get();
        if (account.getVisible() == 0) {
            throw new IllegalArgumentException("Account with id: " + id + " already deleted");
        }
        account.setVisible(0);
        Account deletedAccount = accountRepository.save(account);
        log.info("Soft deleted account id: {}", id);
        // applicationEventPublisher.publishEvent(new UserDeletedEvent(deletedAccount));
        return true;
    }

    /*
     * CREATE ACCOUNT
     * Chức năng: Tạo tài khoản mới với username và password tự động sinh
     * Conditions:
     * - Không có tài khoản nào trùng CCCD và email (nếu có)
     * Exception:
     * - Tài khoản đã tồn tại (trùng CCCD hoặc email): trả về
     * IllegalArgumentException
     */
    @Transactional
    public Account createAccount(Account account) {
        if (existsByCccd(account.getCccd())) {
            throw new IllegalArgumentException("CCCD already exists");
        }

        // Kiểm tra email trùng lặp (nếu có email)
        if (existsByEmail(account.getEmail())) {
            throw new IllegalArgumentException("Email already exists");
        }

        // Tạo username tự động theo format
        String generatedUsername = usernameGenerator.generateUsername(
                account.getFirstName(),
                account.getLastName());
        account.setUsername(generatedUsername);

        // Set password mặc định "123456Aa@"
        String password = usernameGenerator.getDefaultPassword();
        account.setPassword(passwordEncoder.encode(password));

        Account savedAccount = saveAccount(account);

        // Bắn sự kiện sau khi commit
        // applicationEventPublisher.publishEvent(new
        // UserRegisteredEvent(savedAccount));
        return savedAccount;
    }

    /*
     * UPDATE ACCOUNT
     * Chức năng: Cập nhật thông tin tài khoản (giữ nguyên password và visible)
     * Conditions:
     * - Tài khoản tồn tại và chưa bị xóa (visible = 1)
     * - Không có tài khoản nào trùng với data CCCD và email được cập nhật
     * Exception:
     * - Tài khoản không tồn tại (sai id hoặc đã xóa): trả về
     * EntityNotFoundException
     * - Trùng thông tin: trả về IllegalArgumentException
     */
    @Transactional
    public Account updateAccount(Account account) {
        Optional<Account> existingAccount = getAccountByUsername(account.getUsername());
        if (existingAccount.isEmpty()) {
            throw new EntityNotFoundException("Account not found with id: " + account.getId());
        }

        // Kiểm tra CCCD trùng lặp (trừ chính nó)
        Optional<Account> accountWithSameCccd = findByCccd(account.getCccd());
        if (accountWithSameCccd.isPresent() && !accountWithSameCccd.get().getId().equals(account.getId())) {
            throw new IllegalArgumentException("CCCD already exists");
        }

        // Kiểm tra email trùng lặp (nếu có email)
        if (account.getEmail() != null && !account.getEmail().trim().isEmpty()) {
            Optional<Account> accountWithSameEmail = findByEmail(account.getEmail());
            if (accountWithSameEmail.isPresent() && !accountWithSameEmail.get().getId().equals(account.getId())) {
                throw new IllegalArgumentException("Email already exists");
            }
        }

        // Giữ nguyên password cũ
        account.setPassword(existingAccount.get().getPassword());

        // Giữ nguyên visible từ account cũ
        account.setVisible(existingAccount.get().getVisible());

        Account savedAccount = saveAccount(account);
        // applicationEventPublisher.publishEvent(new UserUpdatedEvent(savedAccount));
        return savedAccount;
    }

    /*
     * UPDATE PASSWORD BY USER
     * Tương tự hàm của ADMIN nhưng có thêm bước kiểm tra oldPassword
     * Cmt lại do USER KHÔNG ĐƯỢC ĐỔI PASSWORD (Có thể cập nhật lại sau này)
     */

    // @Transactional
    // public Boolean updatePasswordByUser(Long accountId, PasswordChangeDTO
    // passwordChangeDTO) {
    // Account account = getAccountById(accountId)
    // .orElseThrow(() -> new EntityNotFoundException("Account not found with id: "
    // + accountId));

    // if (!passwordEncoder.matches(passwordChangeDTO.getOldPassword(),
    // account.getPassword())) {
    // throw new IllegalArgumentException("Old password is wrong");
    // }

    // if
    // (!passwordChangeDTO.getNewPassword().equals(passwordChangeDTO.getConfirmPassword()))
    // {
    // throw new IllegalArgumentException("New password and confirm password do not
    // match");
    // }

    // String encodedPassword =
    // passwordEncoder.encode(passwordChangeDTO.getNewPassword());
    // account.setPassword(encodedPassword);
    // accountRepository.save(account);

    // return true;
    // }

    public Account saveAccount(Account account) {
        // Set visible = 1 nếu chưa có giá trị
        if (account.getVisible() == null) {
            account.setVisible(1);
        }

        return accountRepository.save(account);
    }

    /*
     * UNIVERSAL SEARCH
     * Chức năng: Tìm kiếm tài khoản theo nhiều tiêu chí khác nhau với phân trang
     * 
     * Exceptions:
     * - Nếu không tìm thấy kết quả nào thì trả về EntityNotFoundException
     */
    public Page<Account> universalSearch(AccountSearchDTO searchDTO, Pageable pageable) {
        // Validate input bằng ValidateUtil
        searchDTO.setKeyword(ValidateUtil.validateKeyword(searchDTO.getKeyword()));
        searchDTO.setPositionIds(ValidateUtil.cleanPositionIds(searchDTO.getPositionIds()));
        searchDTO.setSearchFields(ValidateUtil.validateSearchFields(searchDTO.getSearchFields()));

        Sort sort = pageable.getSort().and(Sort.by(Sort.Direction.DESC, "createdAt"));
        pageable = PageRequest.of(pageable.getPageNumber(), pageable.getPageSize(), sort);
        Specification<Account> spec = AccountSpecification.build(searchDTO);
        Page<Account> accountPage = accountRepository.findAll(spec, pageable);

        if (accountPage.isEmpty()) {
            throw new EntityNotFoundException("No accounts found matching the criteria");
        }

        return accountPage;
    }

    /**
     * Lấy danh sách tài khoản theo nhiều ID
     * 
     * @param ids Danh sách ID cần lấy
     * @return Danh sách tài khoản
     */
    public List<Account> getAccountsByIds(List<Long> ids) {
        if (ids == null || ids.isEmpty()) {
            return Collections.emptyList();
        }

        // Sử dụng findAllById của JpaRepository và filter theo visible = 1
        return accountRepository.findAll(
                (root, query, criteriaBuilder) -> {
                    Predicate idPredicate = root.get("id").in(ids);
                    Predicate visiblePredicate = criteriaBuilder.equal(root.get("visible"), 1);
                    return criteriaBuilder.and(idPredicate, visiblePredicate);
                });
    }

}