package com.example.react_flow_be.service;

import com.example.react_flow_be.dto.DiagramListItemDto;
import com.example.react_flow_be.dto.DiagramListRequestDto;
import com.example.react_flow_be.dto.DiagramListResponseDto;
import com.example.react_flow_be.entity.Collaboration;
import com.example.react_flow_be.entity.DatabaseDiagram;
import com.example.react_flow_be.entity.Migration;
import com.example.react_flow_be.repository.CollaborationRepository;
import com.example.react_flow_be.repository.DatabaseDiagramRepository;
import com.example.react_flow_be.repository.MigrationRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class DatabaseDiagramListService {

    private final DatabaseDiagramRepository databaseDiagramRepository;
    private final CollaborationRepository collaborationRepository;
    private final MigrationRepository migrationRepository;

    @Transactional(readOnly = true)
    public DiagramListResponseDto getDatabaseDiagramList(DiagramListRequestDto request, String currentUsername) {
        log.info("Getting database diagram list for user: {} with filters: {}", currentUsername, request);

        // Get all database diagrams
        List<DatabaseDiagram> allDiagrams = databaseDiagramRepository.findAll();

        // Filter by user access and apply filters
        List<DatabaseDiagram> filteredDiagrams = allDiagrams.stream()
                .filter(diagram -> hasUserAccess(diagram, currentUsername))
                .filter(diagram -> applyFilters(diagram, request, currentUsername))
                .sorted((d1, d2) -> compareBySort(d1, d2, request))
                .collect(Collectors.toList());

        // Apply pagination
        int pageSize = request.getPageSize() != null ? request.getPageSize() : 20;
        int startIndex = 0;

        // Find start index based on lastDiagramId
        if (request.getLastDiagramId() != null) {
            for (int i = 0; i < filteredDiagrams.size(); i++) {
                if (filteredDiagrams.get(i).getId().equals(request.getLastDiagramId())) {
                    startIndex = i + 1;
                    break;
                }
            }
        }

        // Get page + 1 to check if has more
        int endIndex = Math.min(startIndex + pageSize + 1, filteredDiagrams.size());
        List<DatabaseDiagram> pageDiagrams = filteredDiagrams.subList(startIndex, endIndex);

        // Check if has more
        boolean hasMore = pageDiagrams.size() > pageSize;
        if (hasMore) {
            pageDiagrams = pageDiagrams.subList(0, pageSize);
        }

        // Convert to DTO
        List<DiagramListItemDto> items = pageDiagrams.stream()
                .map(diagram -> convertToDto(diagram, currentUsername))
                .collect(Collectors.toList());

        // Get last id for cursor
        Long lastDiagramId = items.isEmpty() ? null : items.get(items.size() - 1).getId();

        // Total count
        Integer totalCount = filteredDiagrams.size();

        return new DiagramListResponseDto(items, lastDiagramId, hasMore, totalCount);
    }

    @Transactional(readOnly = true)
    public DiagramListResponseDto getDatabaseDiagramAll(DiagramListRequestDto request, String currentUsername) {
        log.info("Getting database diagram list for user: {} with filters: {}", currentUsername, request);

        // Get all database diagrams
        List<DatabaseDiagram> allDiagrams = databaseDiagramRepository.findAll();

        // Filter by user access and apply filters
        List<DatabaseDiagram> filteredDiagrams = allDiagrams;

        // Apply pagination
        int pageSize = request.getPageSize() != null ? request.getPageSize() : 20;
        int startIndex = 0;

        // Find start index based on lastDiagramId
        if (request.getLastDiagramId() != null) {
            for (int i = 0; i < filteredDiagrams.size(); i++) {
                if (filteredDiagrams.get(i).getId().equals(request.getLastDiagramId())) {
                    startIndex = i + 1;
                    break;
                }
            }
        }

        // Get page + 1 to check if has more
        int endIndex = Math.min(startIndex + pageSize + 1, filteredDiagrams.size());
        List<DatabaseDiagram> pageDiagrams = filteredDiagrams.subList(startIndex, endIndex);

        // Check if has more
        boolean hasMore = pageDiagrams.size() > pageSize;
        if (hasMore) {
            pageDiagrams = pageDiagrams.subList(0, pageSize);
        }

        // Convert to DTO
        List<DiagramListItemDto> items = pageDiagrams.stream()
                .map(diagram -> convertToDto(diagram, currentUsername))
                .collect(Collectors.toList());

        // Get last id for cursor
        Long lastDiagramId = items.isEmpty() ? null : items.get(items.size() - 1).getId();

        // Total count
        Integer totalCount = filteredDiagrams.size();

        return new DiagramListResponseDto(items, lastDiagramId, hasMore, totalCount);
    }

    private boolean hasUserAccess(DatabaseDiagram diagram, String username) {
        return collaborationRepository.hasAccess(diagram.getId(), username);
    }

    private boolean applyFilters(DatabaseDiagram diagram, DiagramListRequestDto request, String currentUsername) {
        // Filter by deleted status - handle null safely
        Boolean diagramDeleted = diagram.getIsDeleted() != null ? diagram.getIsDeleted() : false;
        Boolean requestDeleted = request.getIsDeleted() != null ? request.getIsDeleted() : false;

        if (!diagramDeleted.equals(requestDeleted)) {
            return false;
        }

        // Filter by shared with me
        if (request.getSharedWithMe() != null && request.getSharedWithMe()) {
            Collaboration userCollab = collaborationRepository
                    .findByDiagramIdAndUsername(diagram.getId(), currentUsername)
                    .orElse(null);
            if (userCollab == null || userCollab.getType() != Collaboration.CollaborationType.PARTICIPANTS) {
                return false;
            }
        }

        // Filter by name starts with
        if (request.getNameStartsWith() != null && !request.getNameStartsWith().isEmpty()
                && !request.getNameStartsWith().equalsIgnoreCase("all")) {
            if (diagram.getName() == null ||
                    !diagram.getName().toLowerCase().startsWith(request.getNameStartsWith().toLowerCase())) {
                return false;
            }
        }

        // Filter by search query
        if (request.getSearchQuery() != null && !request.getSearchQuery().trim().isEmpty()) {
            String query = request.getSearchQuery().toLowerCase();
            boolean matchName = diagram.getName() != null && diagram.getName().toLowerCase().contains(query);
            boolean matchOwner = false;

            Collaboration owner = collaborationRepository
                    .findByDiagramIdAndType(diagram.getId(), Collaboration.CollaborationType.OWNER)
                    .orElse(null);
            if (owner != null && owner.getUsername() != null) {
                matchOwner = owner.getUsername().toLowerCase().contains(query);
            }

            if (!matchName && !matchOwner) {
                return false;
            }
        }

        // Filter by owner
        if (request.getOwnerFilter() != null && !request.getOwnerFilter().isEmpty()) {
            Collaboration owner = collaborationRepository
                    .findByDiagramIdAndType(diagram.getId(), Collaboration.CollaborationType.OWNER)
                    .orElse(null);

            if ("me".equalsIgnoreCase(request.getOwnerFilter())) {
                if (owner == null || !owner.getUsername().equals(currentUsername)) {
                    return false;
                }
            } else if ("team".equalsIgnoreCase(request.getOwnerFilter())) {
                Integer participantCount = collaborationRepository.countParticipants(diagram.getId());
                if (participantCount == null || participantCount == 0) {
                    return false;
                }
            }
        }

        // Filter by date range
        if (request.getDateRange() != null && !request.getDateRange().equalsIgnoreCase("alltime")) {
            LocalDateTime now = LocalDateTime.now();
            LocalDateTime startDate = null;

            switch (request.getDateRange().toLowerCase()) {
                case "today":
                    startDate = now.toLocalDate().atStartOfDay();
                    break;
                case "last7days":
                    startDate = now.minusDays(7);
                    break;
                case "last30days":
                    startDate = now.minusDays(30);
                    break;
            }

            if (startDate != null && diagram.getUpdatedAt() != null) {
                if (diagram.getUpdatedAt().isBefore(startDate)) {
                    return false;
                }
            }
        }

        return true;
    }

    private int compareBySort(DatabaseDiagram d1, DatabaseDiagram d2, DiagramListRequestDto request) {
        String sortBy = request.getSortBy() != null ? request.getSortBy() : "updatedAt";
        boolean isAsc = "ASC".equalsIgnoreCase(request.getSortDirection());

        int result = 0;
        switch (sortBy) {
            case "name":
                result = compareStrings(d1.getName(), d2.getName());
                break;
            case "createdAt":
                result = compareDates(d1.getCreatedAt(), d2.getCreatedAt());
                break;
            case "updatedAt":
            default:
                result = compareDates(d1.getUpdatedAt(), d2.getUpdatedAt());
                break;
        }

        return isAsc ? result : -result;
    }

    private int compareStrings(String s1, String s2) {
        if (s1 == null && s2 == null)
            return 0;
        if (s1 == null)
            return 1;
        if (s2 == null)
            return -1;
        return s1.compareToIgnoreCase(s2);
    }

    private int compareDates(LocalDateTime d1, LocalDateTime d2) {
        if (d1 == null && d2 == null)
            return 0;
        if (d1 == null)
            return 1;
        if (d2 == null)
            return -1;
        return d1.compareTo(d2);
    }

    private DiagramListItemDto convertToDto(DatabaseDiagram diagram, String currentUsername) {
        DiagramListItemDto dto = new DiagramListItemDto();
        dto.setId(diagram.getId());
        dto.setName(diagram.getName());
        dto.setCreatedAt(diagram.getCreatedAt());
        dto.setUpdatedAt(diagram.getUpdatedAt());

        // Get owner info
        Collaboration owner = collaborationRepository
                .findByDiagramIdAndType(diagram.getId(), Collaboration.CollaborationType.OWNER)
                .orElse(null);

        if (owner != null) {
            dto.setOwnerUsername(owner.getUsername());
            dto.setOwnerFullName(owner.getUsername()); // TODO: Get from User table
            dto.setOwnerAvatar(null); // TODO: Get from User table
            dto.setCreatedByUsername(owner.getUsername());
            dto.setCreatedByFullName(owner.getUsername());
        }

        // Get last migration info
        List<Migration> migrations = migrationRepository
                .findTopByDatabaseDiagramIdOrderByCreatedAtDesc(diagram.getId());

        if (!migrations.isEmpty()) {
            Migration lastMigration = migrations.get(0);
            dto.setLastMigrationUsername(lastMigration.getUsername());
            dto.setLastMigrationDate(lastMigration.getCreatedAt());
            dto.setUpdatedByUsername(lastMigration.getUsername());
            dto.setUpdatedByFullName(lastMigration.getUsername());
        } else {
            // No migration yet, use owner as updatedBy
            if (owner != null) {
                dto.setUpdatedByUsername(owner.getUsername());
                dto.setUpdatedByFullName(owner.getUsername());
            }
        }

        // Check if has collaborators (more than just owner)
        Integer participantCount = collaborationRepository.countParticipants(diagram.getId());
        dto.setHasCollaborators(participantCount != null && participantCount > 0);

        // TODO: Implement starred feature
        dto.setIsStarred(false);

        return dto;
    }
}