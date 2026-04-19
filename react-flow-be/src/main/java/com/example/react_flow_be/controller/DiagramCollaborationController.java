// DiagramCollaborationController.java
package com.example.react_flow_be.controller;

import com.example.react_flow_be.config.DiagramSessionManager;
import com.example.react_flow_be.dto.collaboration.AddCollaboratorRequest;
import com.example.react_flow_be.dto.collaboration.CollaborationDTO;
import com.example.react_flow_be.dto.collaboration.UpdatePermissionRequest;
import com.example.react_flow_be.service.CollaborationService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Set;

@RestController
@RequestMapping("/api/diagram")
@RequiredArgsConstructor
@Slf4j
public class DiagramCollaborationController {

    private final CollaborationService collaborationService;
    private final DiagramSessionManager sessionManager;

    /**
     * Lấy danh sách collaborations của diagram
     */
    @GetMapping("/{diagramId}/collaborations")
    public ResponseEntity<List<CollaborationDTO>> getCollaborations(
            @PathVariable Long diagramId) {
        log.info("GET /api/diagram/{}/collaborations", diagramId);
        List<CollaborationDTO> collaborations = collaborationService.getCollaborations(diagramId);
        return ResponseEntity.ok(collaborations);
    }

    /**
     * Thêm collaborator mới
     */
    @PostMapping("/{diagramId}/collaborations")
    public ResponseEntity<CollaborationDTO> addCollaborator(
            @PathVariable Long diagramId,
            @RequestBody AddCollaboratorRequest request) {
        log.info("POST /api/diagram/{}/collaborations - Adding user: {}",
                diagramId, request.getUsername());

        CollaborationDTO collaboration = collaborationService.addCollaborator(
                diagramId,
                request.getUsername(),
                request.getPermission());

        return ResponseEntity.ok(collaboration);
    }

    /**
     * Cập nhật quyền của collaborator
     */
    @PatchMapping("/{diagramId}/collaborations/{collaborationId}")
    public ResponseEntity<Void> updatePermission(
            @PathVariable Long diagramId,
            @PathVariable Long collaborationId,
            @RequestBody UpdatePermissionRequest request) {
        log.info("PATCH /api/diagram/{}/collaborations/{} - Permission: {}",
                diagramId, collaborationId, request.getPermission());

        collaborationService.updatePermission(collaborationId, request.getPermission());
        return ResponseEntity.ok().build();
    }

    /**
     * Xóa collaborator
     */
    @DeleteMapping("/{diagramId}/collaborations/{collaborationId}")
    public ResponseEntity<Void> removeCollaborator(
            @PathVariable Long diagramId,
            @PathVariable Long collaborationId) {
        log.info("DELETE /api/diagram/{}/collaborations/{}", diagramId, collaborationId);

        collaborationService.removeCollaborator(collaborationId);
        return ResponseEntity.ok().build();
    }

    /**
     * Kiểm tra quyền truy cập
     */
    @GetMapping("/{diagramId}/collaborations/check-access")
    public ResponseEntity<Boolean> checkAccess(
            @PathVariable Long diagramId,
            @RequestParam String username) {
        log.info("GET /api/diagram/{}/collaborations/check-access?username={}",
                diagramId, username);

        boolean hasAccess = collaborationService.hasAccess(diagramId, username);
        return ResponseEntity.ok(hasAccess);
    }

    /**
     * Lấy thông tin owner
     */
    @GetMapping("/{diagramId}/collaborations/owner")
    public ResponseEntity<CollaborationDTO> getOwner(@PathVariable Long diagramId) {
        log.info("GET /api/diagram/{}/collaborations/owner", diagramId);

        CollaborationDTO owner = collaborationService.getOwner(diagramId);
        return ResponseEntity.ok(owner);
    }

    @GetMapping("/{diagramId}/online-users")
    public ResponseEntity<Set<String>> getOnlineUsers(@PathVariable Long diagramId) {
        log.info("GET /api/diagram/{}/online-users", diagramId);
        Set<String> onlineUsers = sessionManager.getActiveUsernames(diagramId);
        return ResponseEntity.ok(onlineUsers);
    }
}