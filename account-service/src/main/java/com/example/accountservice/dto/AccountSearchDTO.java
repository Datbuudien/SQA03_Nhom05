package com.example.accountservice.dto;

import com.example.accountservice.enums.Role;
import lombok.Data;

import java.util.List;

@Data
public class AccountSearchDTO {
    private String keyword;
    private Role role;
    private List<Long> positionIds;
    private List<String> searchFields;
}
