package com.example.accountservice.annotation;

import com.example.accountservice.enums.Role;

import java.lang.annotation.*;

@Target({ ElementType.METHOD })
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface RequireRole {
    Role[] value(); // cho phép truyền nhiều role
}
