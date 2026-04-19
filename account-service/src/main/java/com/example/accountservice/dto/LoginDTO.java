package com.example.accountservice.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class LoginDTO {
    @NotBlank(message = "usename is required")
    private String username;

    @NotBlank(message = "password is required")
    private String password;

}
