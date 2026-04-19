package com.example.accountservice.controller;

import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Collectors;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.example.accountservice.annotation.RequireRole;
import com.example.accountservice.dto.AccountCreateRequest;
import com.example.accountservice.dto.AccountDTO;
import com.example.accountservice.dto.AccountSearchDTO;
import com.example.accountservice.dto.PasswordChangeDTO;
import com.example.accountservice.enums.Role;
import com.example.accountservice.model.Account;
import com.example.accountservice.service.AccountService;

import jakarta.validation.Valid;
import jakarta.ws.rs.NotFoundException;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@RestController
@RequestMapping("/account")
public class AccountController {

    @Autowired
    private AccountService accountService;

    /**
     * Tìm kiếm tài khoản theo từ khóa, theo filter role hoặc position với phân
     * trang
     * Quyền: Chỉ ADMIN mới được sử dụng
     * 
     * Input:
     * - keyword (optional): Từ khóa tìm kiếm trong username, tên, email, cccd, sdt
     * - role (optional): Lọc theo vai trò (STUDENT, TEACHER, ADMIN)
     * - positionIds (optional): Danh sách ID các vị trí
     * - searchFields (optional): Các trường cụ thể muốn tìm kiếm theo keyword
     * - pageable: Thông tin phân trang (page, size, sort)
     * 
     * Output:
     * - Page<Account>: Danh sách tài khoản phân trang với metadata
     */
    @GetMapping("/search")
    @RequireRole({ Role.ADMIN })
    public Page<Account> searchAccounts(
            @RequestParam(value = "keyword", required = false) String keyword,
            @RequestParam(value = "role", required = false) Role role,
            @RequestParam(value = "positionIds", required = false) List<Long> positionIds,
            @RequestParam(value = "searchFields", required = false) List<String> searchFields,
            Pageable pageable) {

        AccountSearchDTO searchDTO = new AccountSearchDTO();
        searchDTO.setKeyword(keyword);
        searchDTO.setRole(role);
        searchDTO.setPositionIds(positionIds);
        searchDTO.setSearchFields(searchFields);

        return accountService.universalSearch(searchDTO, pageable);
    }

    /**
     * Xóa tài khoản (soft delete)
     * Chức năng: Xóa mềm tài khoản (set visible = 0)
     * Quyền: Chỉ ADMIN mới được sử dụng
     * 
     * Input:
     * - id (path variable): ID của tài khoản cần xóa
     * 
     * Output:
     * - String: Thông báo kết quả xóa
     */

    @DeleteMapping("/{id}")
    @RequireRole({ Role.ADMIN })
    public String deleteAccount(@PathVariable Long id) {
        Boolean success = accountService.deleteAccount(id);
        return success ? "Account Deleted Successfully!" : "An Error Occurred!";
    }

    /**
     * Lấy thông tin tài khoản theo ID
     * Chức năng: Truy xuất thông tin chi tiết của một tài khoản bất kì
     * Quyền: ADMIN và TEACHER có thể sử dụng
     * 
     * Input:
     * - id (path variable): ID của tài khoản cần lấy thông tin
     * 
     * Output:
     * - Optional<Account>: Thông tin tài khoản hoặc empty nếu không tìm thấy
     */
    @GetMapping("/{username}")
    @RequireRole({ Role.ADMIN, Role.TEACHER })
    public Optional<Account> getAccountById(@PathVariable String username) {
        log.info("get user by username: " + username);
        return accountService.getAccountByUsername(username);
    }

    /**
     * User xem thông tin của chính mình
     * 
     * Input:
     * - X-Username (header): ID của user hiện tại từ JWT token
     * 
     * Output:
     * - Optional<Account>: Thông tin tài khoản của user hiện tại
     */
    @GetMapping("/me")
    public Optional<Account> getAccountByMe(@RequestHeader(value = "X-Username", required = false) String username) {

        log.info("get user by username: " + username);
        return accountService.getAccountByUsername(username);
    }

    /**
     * Tạo tài khoản mới
     * Chức năng: Tạo tài khoản mới với username và password tự động sinh
     * Quyền: Chỉ ADMIN mới được sử dụng
     * 
     * Input:
     * - account (request body): Thông tin tài khoản mới (firstName, lastName, cccd,
     * role, email,...)
     * - KHÔNG TRUYỀN VÀO ID, USERNAME, PASSWORD, VISIBLE (Không hiển thị các trường
     * này ở FE)
     * - firstName, lastName, cccd bắt buộc phải điền
     * - Username sẽ được tự động sinh từ họ tên
     * - Password mặc định là "123456Aa@"
     * 
     * Output:
     * - Account: Thông tin tài khoản vừa được tạo (bao gồm username đã sinh)
     */
    @PostMapping
    @RequireRole({ Role.ADMIN })
    public Account createAccount(@Valid @RequestBody AccountCreateRequest request) {
        Account account = new Account();
        account.setCccd(request.getCccd());
        account.setFirstName(request.getFirstName());
        account.setLastName(request.getLastName());
        Account savedAccount = accountService.createAccount(account);
        return savedAccount;
    }

    /**
     * Cập nhật thông tin tài khoản
     * Chức năng: Cập nhật thông tin tài khoản (không bao gồm password)
     * Quyền: Chỉ ADMIN mới được sử dụng
     * 
     * Input:
     * - id (path variable): ID của tài khoản cần cập nhật
     * - account (request body): Thông tin mới của tài khoản
     * 
     * Output:
     * - Account: Thông tin tài khoản sau khi cập nhật
     */
    @PutMapping("/{id}")
    @RequireRole({ Role.ADMIN })
    public Account updateAccount(@PathVariable Long id, @Valid @RequestBody Account account) {
        account.setId((id));
        return accountService.updateAccount(account);
    }

    /**
     * USER HIỆN KHÔNG THỂ ĐỔI PASSWORD (có thể cập nhật lại sau)
     * --- Đổi password của Admin và User khác nhau ở Input: Admin không cần
     * oldPassword ---
     * 
     * User đổi password của chính mình
     * Chức năng: Cho phép user thay đổi password của chính mình
     * Quyền: Tất cả user đã đăng nhập
     * 
     * Input:
     * - X-Username (header): ID của user hiện tại từ JWT token
     * - passwordChangeDTO (request body):
     * - oldPassword: Mật khẩu cũ (bắt buộc)
     * - newPassword: Mật khẩu mới (tối thiểu 6 ký tự)
     * - confirmPassword: Xác nhận mật khẩu mới
     * 
     * Output:
     * - Boolean: true nếu đổi password thành công, false nếu thất bại
     */
    // @PutMapping("/change-password")
    // public Boolean updatePasswordByUser(
    // @RequestHeader(value = "X-Username", required = false) String username,
    // @Valid @RequestBody PasswordChangeDTO passwordChangeDTO) {

    // if (passwordChangeDTO.getOldPassword() == null ||
    // passwordChangeDTO.getOldPassword().isEmpty()) {
    // throw new IllegalArgumentException("Old password must not be empty");
    // }
    //
    // return accountService.updatePasswordByUser(accountId, passwordChangeDTO);
    // }

    /**
     * Lấy thông tin nhiều tài khoản theo danh sách ID
     * Chức năng: Truy xuất name của nhiều tài khoản cùng lúc
     * 
     * Input:
     * - ids (query parameter): Danh sách ID các tài khoản cần lấy thông tin (e.g.,
     * ?ids=1&ids=2&ids=3)
     * 
     * Output:
     * - List<Account>: Danh sách thông tin tài khoản
     */
    @GetMapping("/bulk")
    @RequireRole({ Role.ADMIN, Role.TEACHER, Role.STUDENT })
    public List<Account> getAccountsByIds(@RequestParam("ids") List<Long> ids) {
        log.info("Get accounts by ids: " + ids);

        if (ids == null || ids.isEmpty()) {
            return Collections.emptyList();
        }

        // Remove duplicates and null values
        List<Long> uniqueIds = ids.stream()
                .filter(Objects::nonNull)
                .distinct()
                .collect(Collectors.toList());

        return accountService.getAccountsByIds(uniqueIds);
    }

    /*
     * Lấy thông tin tài khoản theo số cccd
     */
    @GetMapping("/student/{cccd}")
    @RequireRole({ Role.ADMIN, Role.TEACHER })
    public ResponseEntity<Account> getAccountByCccd(@PathVariable String cccd) {
        return accountService.findStudentByCccd(cccd)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    /*
     * Lấy thông tin tài khoản theo số cccd
     */
    @GetMapping("/teacher/{cccd}")
    @RequireRole({ Role.ADMIN, Role.TEACHER })
    public ResponseEntity<Account> getTeacherByCccd(@PathVariable String cccd) {
        return accountService.findTeacherByCccd(cccd)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    /*
     * Lấy thông tin tài khoản theo email
     */
    @GetMapping("/email/{email}")
    @RequireRole({ Role.ADMIN, Role.STUDENT })
    public ResponseEntity<AccountDTO> getAccountByEmail(@PathVariable String email) {
        Account acc = accountService.findByEmail(email)
                .orElseThrow(() -> new NotFoundException("Not found account"));

        AccountDTO accountDTO = new AccountDTO();
        accountDTO.setUsername(acc.getUsername());
        accountDTO.setName(acc.getLastName() + " " + acc.getFirstName());
        accountDTO.setPicture(acc.getPicture());

        return ResponseEntity.ok(accountDTO);
    }

}