package com.example.react_flow_be.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class MigrationHistoryDto {
    private Long id;
    private String username;
    private LocalDateTime createdAt;
    private String snapshotHash;
    private Long diagramId;
    private String diagramName;
}