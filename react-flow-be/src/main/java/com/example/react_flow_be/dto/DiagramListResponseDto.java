package com.example.react_flow_be.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class DiagramListResponseDto {
    private List<DiagramListItemDto> diagrams;
    private Long lastDiagramId; // For next page cursor
    private Boolean hasMore;
    private Integer totalCount;
}