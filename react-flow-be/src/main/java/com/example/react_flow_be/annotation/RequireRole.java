package com.example.react_flow_be.annotation;

import java.lang.annotation.*;

import com.example.react_flow_be.enums.Role;

@Target({ ElementType.METHOD })
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface RequireRole {
    Role[] value(); // cho phép truyền nhiều role
}
