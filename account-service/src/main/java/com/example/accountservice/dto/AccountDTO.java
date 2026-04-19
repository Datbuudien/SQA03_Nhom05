package com.example.accountservice.dto;

import com.example.accountservice.enums.Role;

import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import lombok.Data;

@Data
public class AccountDTO {
    private Long id;
    private String username;
    private String picture;
    private String name;
    @Enumerated(EnumType.STRING)
    private Role role;
}
