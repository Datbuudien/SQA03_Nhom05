package com.example.accountservice.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.example.accountservice.enums.Role;
import com.example.accountservice.model.Account;
import com.example.accountservice.model.RedisTokenInfo;
import com.example.accountservice.repository.RedisTokenRepository;

import lombok.extern.slf4j.Slf4j;

import java.util.Optional;

@Slf4j
@Service
public class RedisTokenService {

    @Autowired
    private RedisTokenRepository redisTokenRepository;

    /**
     * Lưu thông tin token vào Redis
     */
    public void saveTokenInfo(String jti, Account account) {
        String username = account.getUsername();
        Role role = account.getRole();
        try {
            redisTokenRepository.deleteById(username);
            RedisTokenInfo tokenInfo = new RedisTokenInfo(jti, username, role);
            redisTokenRepository.save(tokenInfo);
            log.info("Saved token info to Redis - JTI: {}, username: {}, Role: {}",
                    jti, username, role);
        } catch (Exception e) {
            log.error("Failed to save token info to Redis - JTI: {}, Error: {}", jti, e.getMessage());
        }
    }

    /**
     * Lấy thông tin token từ Redis
     */
    public Optional<RedisTokenInfo> getTokenInfo(String username) {
        try {
            return redisTokenRepository.findById(username);
        } catch (Exception e) {
            log.error("Failed to get token info from Redis - username: {}, Error: {}", username, e.getMessage());
            return Optional.empty();
        }
    }

    /**
     * Kiểm tra token có tồn tại trong Redis không
     */
    public boolean isTokenValid(String username) {
        try {
            boolean exists = redisTokenRepository.existsById(username);
            log.debug("Token validation - username: {}, Valid: {}", username, exists);
            return exists;
        } catch (Exception e) {
            log.error("Failed to validate token - username: {}, Error: {}", username, e.getMessage());
            return false;
        }
    }

    /**
     * Xóa token khỏi Redis (logout)
     */
    public void revokeToken(String username) {
        try {
            redisTokenRepository.deleteById(username);
            log.info("Revoked token - username: {}", username);
        } catch (Exception e) {
            log.error("Failed to revoke token - username: {}, Error: {}", username, e.getMessage());
        }
    }

}