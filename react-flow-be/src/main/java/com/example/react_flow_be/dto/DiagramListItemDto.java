package com.example.react_flow_be.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class DiagramListItemDto {
    private Long id;
    private String name;

    // Owner info
    private String ownerUsername;
    private String ownerFullName;
    private String ownerAvatar;

    // Last modified info
    private String updatedByUsername;
    private String updatedByFullName;
    private String updatedByAvatar;
    private LocalDateTime updatedAt;

    // Creation info
    private LocalDateTime createdAt;
    private String createdByUsername;
    private String createdByFullName;

    // Additional info
    private Boolean isStarred; // TODO: Implement starred feature
    private Boolean hasCollaborators;
    private String lastMigrationUsername;
    private LocalDateTime lastMigrationDate;
}