package com.example.apigateway.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.example.apigateway.model.RedisTokenInfo;
import com.example.apigateway.repository.RedisTokenRepository;

import lombok.extern.slf4j.Slf4j;

import java.util.Optional;

@Slf4j
@Service
public class RedisTokenService {

    @Autowired
    private RedisTokenRepository redisTokenRepository;

    /**
     * Kiểm tra token có tồn tại trong Redis và lấy thông tin
     */
    public Optional<RedisTokenInfo> getTokenInfo(String username) {
        try {
            Optional<RedisTokenInfo> tokenInfo = redisTokenRepository.findById(username);

            if (tokenInfo.isPresent()) {
                log.debug("Found token info in Redis - username: {}, jti: {}, Role: {}",
                        username, tokenInfo.get().getJti(), tokenInfo.get().getRole());
                return tokenInfo;
            } else {
                log.debug("Token not found in Redis - username: {}", username);
                return Optional.empty();
            }
        } catch (Exception e) {
            log.error("Failed to get token info from Redis - username: {}, Error: {}", username, e.getMessage());
            return Optional.empty();
        }
    }

    /**
     * Kiểm tra token có hợp lệ không (tồn tại trong Redis)
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

}