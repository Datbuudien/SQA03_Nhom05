package com.example.accountservice.model;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import org.springframework.data.annotation.Id;
import org.springframework.data.redis.core.RedisHash;
import org.springframework.data.redis.core.TimeToLive;

import com.example.accountservice.enums.Role;

import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
@RedisHash("token_info")
public class RedisTokenInfo {

    @Id
    private String username;

    private String jti;
    private Role role;

    @TimeToLive
    private Long ttl;

    public RedisTokenInfo(String jti, String username, Role role) {
        this.jti = jti;
        this.username = username;
        this.role = role;
        this.ttl = 86400L;
    }
}