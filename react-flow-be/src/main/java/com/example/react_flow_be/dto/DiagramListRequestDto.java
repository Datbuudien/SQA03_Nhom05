package com.example.react_flow_be.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class DiagramListRequestDto {
    // Pagination
    private Long lastDiagramId; // For cursor-based pagination
    private Integer pageSize = 20; // Default page size

    // Filters
    private String nameStartsWith; // Filter by first letter (a, b, c, ...)
    private String searchQuery; // Search in name or owner
    private String ownerFilter; // "me" or "team" or null for all
    private String dateRange; // "today", "last7days", "last30days", "alltime"
    private Boolean isDeleted = false; // For trash page
    private Boolean sharedWithMe = false; // For shared page

    // Sort
    private String sortBy = "updatedAt"; // "name", "createdAt", "updatedAt"
    private String sortDirection = "DESC"; // "ASC" or "DESC"
}