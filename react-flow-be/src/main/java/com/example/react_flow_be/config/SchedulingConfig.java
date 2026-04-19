package com.example.react_flow_be.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableScheduling;

/**
 * Configuration to enable Spring Scheduling
 */
@Configuration
@EnableScheduling
public class SchedulingConfig {
    // Spring will automatically detect @Scheduled methods
}