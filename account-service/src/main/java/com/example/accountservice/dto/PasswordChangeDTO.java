package com.example.accountservice.dto;

import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class PasswordChangeDTO {
    private String oldPassword;

    @Size(min = 6, message = "Password must be at least 6 characters")
    private String newPassword;

    @Size(min = 6, message = "Password must be at least 6 characters")
    private String confirmPassword;
}
