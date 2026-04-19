package com.example.accountservice.controller;

import java.util.Map;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.example.accountservice.service.RedisTokenService;

import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.transaction.Transactional;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@RestController
@Transactional
@RequestMapping("/account")
public class AuthController {

    @Autowired
    private RedisTokenService redisTokenService;

    @Autowired
    private HttpServletRequest request;

    /**
     * Đăng xuất khỏi hệ thống
     * Xóa thông tin account khỏi redis
     * 
     * Output:
     * - Trả về thông báo thành công hoặc thất bại
     * - Nếu đăng xuất thành công thì response set jwt về null
     */
    @PostMapping("/logout")
    public ResponseEntity<?> logout(HttpServletResponse response,
            @RequestHeader(value = "X-Username", required = false) String username) {

        if (username == null || username.isBlank()) {
            return ResponseEntity.badRequest().body(Map.of("error", "Missing X-Username header"));
        }

        try {
            // Clear token in redis
            redisTokenService.revokeToken(username);

            // Clear JWT cookie
            Cookie jwtCookie = new Cookie("jwt", null);
            jwtCookie.setHttpOnly(true);
            jwtCookie.setSecure(false);
            jwtCookie.setPath("/");
            jwtCookie.setMaxAge(0);
            response.addCookie(jwtCookie);

            log.info("User logged out successfully: {}", username);
            return ResponseEntity.ok(Map.of("message", "Logout successful. Cookie cleared."));

        } catch (Exception e) {
            log.error("Logout failed", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", "Logout failed", "message", e.getMessage()));
        }
    }
}