package com.example.react_flow_be.service;

import com.example.react_flow_be.dto.DiagramListItemDto;
import com.example.react_flow_be.dto.DiagramListRequestDto;
import com.example.react_flow_be.dto.DiagramListResponseDto;
import com.example.react_flow_be.entity.Collaboration;
import com.example.react_flow_be.entity.Diagram;
import com.example.react_flow_be.entity.Migration;
import com.example.react_flow_be.repository.CollaborationRepository;
import com.example.react_flow_be.repository.DiagramRepository;
import com.example.react_flow_be.repository.MigrationRepository;
import com.example.react_flow_be.specification.DiagramSpecification;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class DiagramListService {

    private final DiagramRepository diagramRepository;
    private final CollaborationRepository collaborationRepository;
    private final MigrationRepository migrationRepository;

    @Transactional(readOnly = true)
    public DiagramListResponseDto getDiagramList(DiagramListRequestDto request, String currentUsername) {
        log.info("Getting diagram list for user: {} with filters: {}", currentUsername, request);

        // Build specification
        Specification<Diagram> spec = buildSpecification(request, currentUsername);

        // Build sort
        Sort sort = buildSort(request);

        // Build pageable with cursor
        int pageSize = request.getPageSize() != null ? request.getPageSize() : 20;
        Pageable pageable = PageRequest.of(0, pageSize + 1, sort); // +1 to check if has more

        // Execute query
        List<Diagram> diagrams = diagramRepository.findAll(spec, pageable).getContent();

        // Check if has more
        boolean hasMore = diagrams.size() > pageSize;
        if (hasMore) {
            diagrams = diagrams.subList(0, pageSize);
        }

        // Convert to DTO
        List<DiagramListItemDto> items = diagrams.stream()
                .map(diagram -> convertToDto(diagram, currentUsername))
                .collect(Collectors.toList());

        // Get last id for cursor
        Long lastDiagramId = items.isEmpty() ? null : items.get(items.size() - 1).getId();

        // Get total count (optional, có thể tốn performance)
        Integer totalCount = (int) diagramRepository.count(spec);

        return new DiagramListResponseDto(items, lastDiagramId, hasMore, totalCount);
    }

    private Specification<Diagram> buildSpecification(DiagramListRequestDto request, String currentUsername) {
        Specification<Diagram> spec = Specification.where(null);

        // User must have access to diagram
        spec = spec.and(DiagramSpecification.userHasAccess(currentUsername));

        // Filter by deleted status
        spec = spec.and(DiagramSpecification.isDeleted(request.getIsDeleted()));

        // Filter by shared with me
        if (request.getSharedWithMe() != null && request.getSharedWithMe()) {
            spec = spec.and(DiagramSpecification.sharedWithMe(true, currentUsername));
        }

        // Filter by name starting with letter
        if (request.getNameStartsWith() != null && !request.getNameStartsWith().isEmpty()) {
            spec = spec.and(DiagramSpecification.nameStartsWith(request.getNameStartsWith()));
        }

        // Search query
        if (request.getSearchQuery() != null && !request.getSearchQuery().trim().isEmpty()) {
            spec = spec.and(DiagramSpecification.searchQuery(request.getSearchQuery()));
        }

        // Owner filter (me or team)
        if (request.getOwnerFilter() != null && !request.getOwnerFilter().isEmpty()) {
            spec = spec.and(DiagramSpecification.ownerFilter(request.getOwnerFilter(), currentUsername));
        }

        // Date range filter
        if (request.getDateRange() != null && !request.getDateRange().isEmpty()) {
            spec = spec.and(DiagramSpecification.dateRange(request.getDateRange()));
        }

        // Cursor pagination
        if (request.getLastDiagramId() != null) {
            spec = spec.and(DiagramSpecification.afterId(request.getLastDiagramId()));
        }

        return spec;
    }

    private Sort buildSort(DiagramListRequestDto request) {
        String sortBy = request.getSortBy() != null ? request.getSortBy() : "updatedAt";
        String direction = request.getSortDirection() != null ? request.getSortDirection() : "DESC";

        Sort.Direction sortDirection = "ASC".equalsIgnoreCase(direction)
                ? Sort.Direction.ASC
                : Sort.Direction.DESC;

        // Always add id as secondary sort for stable pagination
        return Sort.by(sortDirection, sortBy).and(Sort.by(Sort.Direction.DESC, "id"));
    }

    private DiagramListItemDto convertToDto(Diagram diagram, String currentUsername) {
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