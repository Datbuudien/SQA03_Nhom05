package com.example.accountservice.model;

import java.time.LocalDateTime;
import java.util.List;

import com.example.accountservice.enums.Role;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;

import jakarta.persistence.*;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.Size;
import lombok.Data;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

@Entity
@Data
public class Account {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Size(min = 3, max = 50, message = "Username must be between 3 and 50 characters")
    @Column(unique = true, nullable = false)
    private String username;

    @JsonProperty(access = JsonProperty.Access.WRITE_ONLY)
    @Size(min = 6, message = "Password must be at least 6 characters")
    private String password; // Có thể null với OAuth2 login

    private String firstName;

    private String lastName;

    private LocalDateTime birthDay;

    private String phoneNumber;

    @Email(message = "Email should be valid")
    private String email; // Bắt buộc với OAuth2

    @Size(min = 9, max = 12, message = "CCCD must be between 9 and 12 characters")
    private String cccd; // Có thể null với OAuth2

    private String note;

    // OAuth2 fields
    @Column(unique = true)
    private String googleId; // Google's unique identifier (sub)

    private String picture; // Profile picture URL from Google

    private String provider; // "google" hoặc "local"

    private Integer visible = 1; // 1: hiển thị, 0: đã xóa (soft delete)

    @Enumerated(EnumType.STRING)
    private Role role;

    @JsonIgnore
    @CreationTimestamp
    @Column(updatable = false)
    private LocalDateTime createdAt;

    @JsonIgnore
    @UpdateTimestamp
    private LocalDateTime updatedAt;
}