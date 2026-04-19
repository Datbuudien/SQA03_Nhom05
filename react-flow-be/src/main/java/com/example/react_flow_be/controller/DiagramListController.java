package com.example.react_flow_be.controller;

import com.example.react_flow_be.annotation.RequireRole;
import com.example.react_flow_be.dto.DiagramListRequestDto;
import com.example.react_flow_be.dto.DiagramListResponseDto;
import com.example.react_flow_be.enums.Role;
import com.example.react_flow_be.service.DatabaseDiagramListService;
import com.example.react_flow_be.service.DiagramManagementService;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/diagrams")
@RequiredArgsConstructor
@Slf4j
public class DiagramListController {

    private final DatabaseDiagramListService databaseDiagramListService;
    private final DiagramManagementService diagramManagementService;

    /**
     * Get paginated list of diagrams with filters
     * GET /api/diagrams/list
     * 
     * Query params:
     * - lastDiagramId: For cursor pagination
     * - pageSize: Number of items per page (default 20)
     * - nameStartsWith: Filter by first letter (a, b, c, ...)
     * - searchQuery: Search in name or owner
     * - ownerFilter: "me" or "team"
     * - dateRange: "today", "last7days", "last30days", "alltime"
     * - isDeleted: true for trash, false for active
     * - sharedWithMe: true for shared diagrams
     * - sortBy: "name", "createdAt", "updatedAt"
     * - sortDirection: "ASC" or "DESC"
     */
    @GetMapping("/list")
    public ResponseEntity<DiagramListResponseDto> getDiagramList(
            @RequestParam(required = false) Long lastDiagramId,
            @RequestParam(required = false, defaultValue = "20") Integer pageSize,
            @RequestParam(required = false) String nameStartsWith,
            @RequestParam(required = false) String searchQuery,
            @RequestParam(required = false) String ownerFilter,
            @RequestParam(required = false, defaultValue = "alltime") String dateRange,
            @RequestParam(required = false, defaultValue = "false") Boolean isDeleted,
            @RequestParam(required = false, defaultValue = "false") Boolean sharedWithMe,
            @RequestParam(required = false, defaultValue = "updatedAt") String sortBy,
            @RequestParam(required = false, defaultValue = "DESC") String sortDirection,
            HttpServletRequest request) {

        String username = request.getHeader("X-Username");

        if (username == null || username.isEmpty()) {
            log.warn("No username provided in request header");
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }

        DiagramListRequestDto requestDto = new DiagramListRequestDto();
        requestDto.setLastDiagramId(lastDiagramId);
        requestDto.setPageSize(pageSize);
        requestDto.setNameStartsWith(nameStartsWith);
        requestDto.setSearchQuery(searchQuery);
        requestDto.setOwnerFilter(ownerFilter);
        requestDto.setDateRange(dateRange);
        requestDto.setIsDeleted(isDeleted);
        requestDto.setSharedWithMe(sharedWithMe);
        requestDto.setSortBy(sortBy);
        requestDto.setSortDirection(sortDirection);

        log.info("GET /api/diagrams/list - user: {}, filters: {}", username, requestDto);

        try {
            DiagramListResponseDto response = databaseDiagramListService.getDatabaseDiagramList(requestDto, username);
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            log.error("Error getting diagram list: {}", e.getMessage(), e);
            return ResponseEntity.internalServerError().build();
        }
    }

    @GetMapping("/all")
    @RequireRole({ Role.ADMIN })
    public ResponseEntity<DiagramListResponseDto> getDiagramAll(
            @RequestParam(required = false) Long lastDiagramId,
            @RequestParam(required = false, defaultValue = "20") Integer pageSize,
            @RequestParam(required = false) String nameStartsWith,
            @RequestParam(required = false) String searchQuery,
            @RequestParam(required = false) String ownerFilter,
            @RequestParam(required = false, defaultValue = "alltime") String dateRange,
            @RequestParam(required = false, defaultValue = "false") Boolean isDeleted,
            @RequestParam(required = false, defaultValue = "false") Boolean sharedWithMe,
            @RequestParam(required = false, defaultValue = "updatedAt") String sortBy,
            @RequestParam(required = false, defaultValue = "DESC") String sortDirection,
            HttpServletRequest request) {

        String username = request.getHeader("X-Username");

        if (username == null || username.isEmpty()) {
            log.warn("No username provided in request header");
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }

        DiagramListRequestDto requestDto = new DiagramListRequestDto();
        requestDto.setLastDiagramId(lastDiagramId);
        requestDto.setPageSize(pageSize);
        requestDto.setNameStartsWith(nameStartsWith);
        requestDto.setSearchQuery(searchQuery);
        requestDto.setOwnerFilter(ownerFilter);
        requestDto.setDateRange(dateRange);
        requestDto.setIsDeleted(isDeleted);
        requestDto.setSharedWithMe(sharedWithMe);
        requestDto.setSortBy(sortBy);
        requestDto.setSortDirection(sortDirection);

        log.info("GET /api/diagrams/list - user: {}, filters: {}", username, requestDto);

        try {
            DiagramListResponseDto response = databaseDiagramListService.getDatabaseDiagramAll(requestDto, username);
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            log.error("Error getting diagram list: {}", e.getMessage(), e);
            return ResponseEntity.internalServerError().build();
        }
    }

    /**
     * Soft delete diagram (move to trash)
     * DELETE /api/diagrams/{diagramId}
     */
    @DeleteMapping("/{diagramId}")
    public ResponseEntity<?> deleteDiagram(
            @PathVariable Long diagramId,
            HttpServletRequest request) {

        String username = request.getHeader("X-Username");

        if (username == null || username.isEmpty()) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("No username provided");
        }

        log.info("DELETE /api/diagrams/{} - user: {}", diagramId, username);

        try {
            diagramManagementService.softDeleteDiagram(diagramId, username);
            return ResponseEntity.ok().body("Diagram moved to trash");
        } catch (IllegalStateException e) {
            log.warn("Permission denied: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body(e.getMessage());
        } catch (Exception e) {
            log.error("Error deleting diagram: {}", e.getMessage(), e);
            return ResponseEntity.internalServerError().body("Error deleting diagram");
        }
    }

    /**
     * Restore diagram from trash
     * POST /api/diagrams/{diagramId}/restore
     */
    @PostMapping("/{diagramId}/restore")
    public ResponseEntity<?> restoreDiagram(
            @PathVariable Long diagramId,
            HttpServletRequest request) {

        String username = request.getHeader("X-Username");

        if (username == null || username.isEmpty()) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("No username provided");
        }

        log.info("POST /api/diagrams/{}/restore - user: {}", diagramId, username);

        try {
            diagramManagementService.restoreDiagram(diagramId, username);
            return ResponseEntity.ok().body("Diagram restored from trash");
        } catch (IllegalStateException e) {
            log.warn("Permission denied: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body(e.getMessage());
        } catch (Exception e) {
            log.error("Error restoring diagram: {}", e.getMessage(), e);
            return ResponseEntity.internalServerError().body("Error restoring diagram");
        }
    }

    /**
     * Permanently delete diagram
     * DELETE /api/diagrams/{diagramId}/permanent
     */
    @DeleteMapping("/{diagramId}/permanent")
    public ResponseEntity<?> permanentlyDeleteDiagram(
            @PathVariable Long diagramId,
            HttpServletRequest request) {

        String username = request.getHeader("X-Username");

        if (username == null || username.isEmpty()) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("No username provided");
        }

        log.info("DELETE /api/diagrams/{}/permanent - user: {}", diagramId, username);

        try {
            diagramManagementService.permanentlyDeleteDiagram(diagramId, username);
            return ResponseEntity.ok().body("Diagram permanently deleted");
        } catch (IllegalStateException e) {
            log.warn("Permission denied or invalid state: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body(e.getMessage());
        } catch (Exception e) {
            log.error("Error permanently deleting diagram: {}", e.getMessage(), e);
            return ResponseEntity.internalServerError().body("Error permanently deleting diagram");
        }
    }

    /**
     * Check if user is owner of diagram
     * GET /api/diagrams/{diagramId}/is-owner
     */
    @GetMapping("/{diagramId}/is-owner")
    public ResponseEntity<Boolean> isOwner(
            @PathVariable Long diagramId,
            HttpServletRequest request) {

        String username = request.getHeader("X-Username");

        if (username == null || username.isEmpty()) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }

        try {
            boolean isOwner = diagramManagementService.isOwner(diagramId, username);
            return ResponseEntity.ok(isOwner);
        } catch (Exception e) {
            log.error("Error checking ownership: {}", e.getMessage(), e);
            return ResponseEntity.internalServerError().build();
        }
    }

    /**
     * Get trash count
     * GET /api/diagrams/trash/count
     */
    @GetMapping("/trash/count")
    public ResponseEntity<Long> getTrashCount(HttpServletRequest request) {
        String username = request.getHeader("X-Username");

        if (username == null || username.isEmpty()) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }

        try {
            Long count = diagramManagementService.getTrashCount(username);
            return ResponseEntity.ok(count);
        } catch (Exception e) {
            log.error("Error getting trash count: {}", e.getMessage(), e);
            return ResponseEntity.internalServerError().build();
        }
    }
}