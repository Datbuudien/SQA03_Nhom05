package com.example.react_flow_be.controller;

import com.example.react_flow_be.dto.MigrationDetailDto;
import com.example.react_flow_be.dto.MigrationHistoryDto;
import com.example.react_flow_be.service.CollaborationService;
import com.example.react_flow_be.service.MigrationService;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/migration")
@RequiredArgsConstructor
@Slf4j
public class MigrationController {

    private final MigrationService migrationService;
    private final CollaborationService collaborationService;

    /**
     * Lấy danh sách lịch sử migration của một diagram
     * GET /api/migration/diagram/{diagramId}/history
     */
    @GetMapping("/diagram/{diagramId}/history")
    public ResponseEntity<List<MigrationHistoryDto>> getMigrationHistory(
            @PathVariable Long diagramId,
            HttpServletRequest request) {

        String username = request.getHeader("X-Username");

        // Kiểm tra quyền truy cập
        boolean hasAccess = collaborationService.hasAccess(diagramId, username);
        if (!hasAccess) {
            log.warn("User {} denied access to migration history for diagram {}", username, diagramId);
            return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
        }

        try {
            List<MigrationHistoryDto> history = migrationService.getMigrationHistory(diagramId);
            log.info("Retrieved {} migration records for diagram {}", history.size(), diagramId);
            return ResponseEntity.ok(history);
        } catch (Exception e) {
            log.error("Error retrieving migration history for diagram {}: {}", diagramId, e.getMessage(), e);
            return ResponseEntity.internalServerError().build();
        }
    }

    /**
     * Lấy chi tiết một migration snapshot
     * GET /api/migration/{migrationId}
     */
    @GetMapping("/{migrationId}")
    public ResponseEntity<MigrationDetailDto> getMigrationDetail(
            @PathVariable Long migrationId,
            HttpServletRequest request) {

        String username = request.getHeader("X-Username");

        try {
            MigrationDetailDto detail = migrationService.getMigrationDetail(migrationId);

            // Kiểm tra quyền truy cập diagram
            boolean hasAccess = collaborationService.hasAccess(detail.getDiagramId(), username);
            if (!hasAccess) {
                log.warn("User {} denied access to migration {} for diagram {}",
                        username, migrationId, detail.getDiagramId());
                return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
            }

            log.info("Retrieved migration detail {} for user {}", migrationId, username);
            return ResponseEntity.ok(detail);
        } catch (Exception e) {
            log.error("Error retrieving migration detail {}: {}", migrationId, e.getMessage(), e);

            if (e.getMessage().contains("not found")) {
                return ResponseEntity.notFound().build();
            }

            return ResponseEntity.internalServerError().build();
        }
    }

    /**
     * Tạo snapshot thủ công (optional - để test)
     * POST /api/migration/diagram/{diagramId}/snapshot
     */
    @PostMapping("/diagram/{diagramId}/snapshot")
    public ResponseEntity<String> createManualSnapshot(
            @PathVariable Long diagramId,
            HttpServletRequest request) {

        String username = request.getHeader("X-Username");

        // Kiểm tra quyền truy cập
        boolean hasAccess = collaborationService.hasAccess(diagramId, username);
        if (!hasAccess) {
            log.warn("User {} denied access to create snapshot for diagram {}", username, diagramId);
            return ResponseEntity.status(HttpStatus.FORBIDDEN)
                    .body("Access denied");
        }

        try {
            boolean created = migrationService.createSnapshotOnDisconnect(diagramId, username);

            if (created) {
                log.info("Manual snapshot created for diagram {} by user {}", diagramId, username);
                return ResponseEntity.ok("Snapshot created successfully");
            } else {
                log.info("No changes detected for diagram {}, snapshot not created", diagramId);
                return ResponseEntity.ok("No changes detected, snapshot not created");
            }
        } catch (Exception e) {
            log.error("Error creating manual snapshot for diagram {}: {}", diagramId, e.getMessage(), e);
            return ResponseEntity.internalServerError()
                    .body("Error creating snapshot: " + e.getMessage());
        }
    }
}