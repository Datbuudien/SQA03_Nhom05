package com.example.react_flow_be.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ConnectionDto {
    private Long id;
    private String targetModelId;
    private String targetAttributeId;
    private String foreignKeyName;
    private Boolean isEnforced;
    
}