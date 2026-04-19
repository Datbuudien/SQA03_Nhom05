// CollaborationService.java
package com.example.react_flow_be.service;

import com.example.react_flow_be.config.DiagramSessionManager;
import com.example.react_flow_be.config.WebSocketSessionTracker;
import com.example.react_flow_be.dto.collaboration.CollaborationDTO;
import com.example.react_flow_be.entity.Collaboration;
import com.example.react_flow_be.entity.Diagram;
import com.example.react_flow_be.repository.CollaborationRepository;
import com.example.react_flow_be.repository.DiagramRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import jakarta.persistence.EntityNotFoundException;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class CollaborationService {

    private final CollaborationRepository collaborationRepository;
    private final DiagramRepository diagramRepository;
    private final DiagramSessionManager sessionManager;
    private final WebSocketSessionTracker sessionRegistry;

    /**
     * Lấy danh sách tất cả collaborations của một diagram
     */
    @Transactional(readOnly = true)
    public List<CollaborationDTO> getCollaborations(Long diagramId) {
        log.info("Getting collaborations for diagram: {}", diagramId);

        // Kiểm tra diagram có tồn tại không
        if (!diagramRepository.existsById(diagramId)) {
            throw new EntityNotFoundException("Diagram not found with id: " + diagramId);
        }

        List<Collaboration> collaborations = collaborationRepository.findByDiagramId(diagramId);

        return collaborations.stream()
                .map(CollaborationDTO::fromEntity)
                .collect(Collectors.toList());
    }

    /**
     * Thêm collaborator mới vào diagram
     */
    @Transactional
    public CollaborationDTO addCollaborator(Long diagramId, String username, Collaboration.Permission permission) {
        log.info("Adding collaborator {} to diagram {} with permission {}", username, diagramId, permission);

        // Kiểm tra diagram có tồn tại không
        Diagram diagram = diagramRepository.findById(diagramId)
                .orElseThrow(() -> new EntityNotFoundException("Diagram not found with id: " + diagramId));

        // Kiểm tra user đã là collaborator chưa
        if (collaborationRepository.findByDiagramIdAndUsername(diagramId, username).isPresent()) {
            throw new IllegalArgumentException("User is already a collaborator of this diagram");
        }

        // Tạo collaboration mới
        Collaboration collaboration = new Collaboration();
        collaboration.setDiagram(diagram);
        collaboration.setUsername(username);
        collaboration.setType(Collaboration.CollaborationType.PARTICIPANTS);
        collaboration.setPermission(permission);
        collaboration.setIsActive(true);
        collaboration.setExpiresAt(null); // Không có thời hạn

        Collaboration saved = collaborationRepository.save(collaboration);
        log.info("Successfully added collaborator with id: {}", saved.getId());

        return CollaborationDTO.fromEntity(saved);
    }

    /**
     * Cập nhật quyền của collaborator
     */
    @Transactional
    public void updatePermission(Long collaborationId, Collaboration.Permission newPermission) {
        log.info("Updating permission for collaboration {} to {}", collaborationId, newPermission);

        Collaboration collaboration = collaborationRepository.findById(collaborationId)
                .orElseThrow(() -> new EntityNotFoundException("Collaboration not found with id: " + collaborationId));

        if (collaboration.getType() == Collaboration.CollaborationType.OWNER) {
            throw new IllegalArgumentException("Cannot change permission of owner");
        }

        Collaboration.Permission oldPermission = collaboration.getPermission();
        String username = collaboration.getUsername();
        Long diagramId = collaboration.getDiagram().getId();

        // KIỂM TRA NẾU GIẢM QUYỀN TỪ FULL_ACCESS VỀ VIEW
        if (oldPermission == Collaboration.Permission.FULL_ACCESS &&
                newPermission == Collaboration.Permission.VIEW) {

            log.warn("⚠️ Downgrading permission from FULL_ACCESS to VIEW for user: {}", username);

            if (sessionManager.isUserActiveInDiagram(diagramId, username)) {
                log.info("🔌 User {} is currently connected, force disconnecting...", username);
                forceDisconnectUser(diagramId, username);
            }
        }

        // Cập nhật quyền
        collaboration.setPermission(newPermission);
        collaborationRepository.save(collaboration);

        log.info("✅ Successfully updated permission for collaboration {}", collaborationId);
    }

    /**
     * Xóa collaborator khỏi diagram
     */
    @Transactional
    public void removeCollaborator(Long collaborationId) {
        log.info("Removing collaborator with id: {}", collaborationId);

        Collaboration collaboration = collaborationRepository.findById(collaborationId)
                .orElseThrow(() -> new EntityNotFoundException("Collaboration not found with id: " + collaborationId));

        if (collaboration.getType() == Collaboration.CollaborationType.OWNER) {
            throw new IllegalArgumentException("Cannot remove owner from diagram");
        }

        String username = collaboration.getUsername();
        Long diagramId = collaboration.getDiagram().getId();

        // KIỂM TRA USER CÓ ĐANG KẾT NỐI KHÔNG
        if (sessionManager.isUserActiveInDiagram(diagramId, username)) {
            log.info("🔌 User {} is currently connected, force disconnecting...", username);
            forceDisconnectUser(diagramId, username);
        }

        // Xóa collaboration
        collaborationRepository.delete(collaboration);
        log.info("✅ Successfully removed collaborator {}", collaborationId);
    }

    private void forceDisconnectUser(Long diagramId, String username) {
        try {
            // Lấy tất cả sessions của user trong diagram
            Set<String> allSessions = sessionManager.getActiveSessions(diagramId);

            Set<String> userSessions = allSessions.stream()
                    .filter(sessionId -> username.equals(sessionManager.getUsernameForSession(sessionId)))
                    .collect(Collectors.toSet());

            if (userSessions.isEmpty()) {
                log.warn("⚠️ No active sessions found for user {}", username);
                return;
            }

            log.info("Found {} session(s) for user {} in diagram {}",
                    userSessions.size(), username, diagramId);

            // ĐÓNG TẤT CẢ WEBSOCKET SESSIONS THẬT
            sessionRegistry.closeSessions(userSessions);

            log.info("✅ Force disconnected user {} from diagram {}", username, diagramId);

        } catch (Exception e) {
            log.error("❌ Error force disconnecting user {}: {}", username, e.getMessage(), e);
        }
    }

    /**
     * Kiểm tra user có quyền truy cập diagram không
     */
    @Transactional(readOnly = true)
    public boolean hasAccess(Long diagramId, String username) {
        return collaborationRepository.hasAccess(diagramId, username);
    }

    /**
     * Lấy thông tin collaboration của user với diagram
     */
    @Transactional(readOnly = true)
    public CollaborationDTO getUserCollaboration(Long diagramId, String username) {
        Collaboration collaboration = collaborationRepository
                .findActiveCollaboration(diagramId, username)
                .orElseThrow(() -> new EntityNotFoundException(
                        "User does not have access to this diagram"));

        return CollaborationDTO.fromEntity(collaboration);
    }

    /**
     * Lấy thông tin owner của diagram
     */
    @Transactional(readOnly = true)
    public CollaborationDTO getOwner(Long diagramId) {
        Collaboration owner = collaborationRepository
                .findByDiagramIdAndType(diagramId, Collaboration.CollaborationType.OWNER)
                .orElseThrow(() -> new EntityNotFoundException("Owner not found for diagram: " + diagramId));

        return CollaborationDTO.fromEntity(owner);
    }

    /**
     * Đếm số participants của diagram
     */
    @Transactional(readOnly = true)
    public long countParticipants(Long diagramId) {
        return collaborationRepository.countParticipants(diagramId);
    }

    /**
     * Tạo owner collaboration khi tạo diagram mới
     */
    @Transactional
    public CollaborationDTO createOwner(Long diagramId, String username) {
        log.info("Creating owner collaboration for diagram {} with user {}", diagramId, username);

        Diagram diagram = diagramRepository.findById(diagramId)
                .orElseThrow(() -> new EntityNotFoundException("Diagram not found with id: " + diagramId));

        Collaboration collaboration = new Collaboration();
        collaboration.setDiagram(diagram);
        collaboration.setUsername(username);
        collaboration.setType(Collaboration.CollaborationType.OWNER);
        collaboration.setPermission(Collaboration.Permission.FULL_ACCESS);
        collaboration.setIsActive(true);
        collaboration.setExpiresAt(null);

        Collaboration saved = collaborationRepository.save(collaboration);
        log.info("Successfully created owner collaboration with id: {}", saved.getId());

        return CollaborationDTO.fromEntity(saved);
    }

    /**
     * Vô hiệu hóa collaboration (soft delete)
     */
    @Transactional
    public void deactivateCollaboration(Long collaborationId) {
        log.info("Deactivating collaboration: {}", collaborationId);

        Collaboration collaboration = collaborationRepository.findById(collaborationId)
                .orElseThrow(() -> new EntityNotFoundException("Collaboration not found with id: " + collaborationId));

        if (collaboration.getType() == Collaboration.CollaborationType.OWNER) {
            throw new IllegalArgumentException("Cannot deactivate owner collaboration");
        }

        collaboration.setIsActive(false);
        collaborationRepository.save(collaboration);

        log.info("Successfully deactivated collaboration {}", collaborationId);
    }

    /**
     * Xóa tất cả collaborations của diagram (khi xóa diagram)
     */
    @Transactional
    public void deleteAllByDiagramId(Long diagramId) {
        log.info("Deleting all collaborations for diagram: {}", diagramId);
        collaborationRepository.deleteByDiagramId(diagramId);
        log.info("Successfully deleted all collaborations for diagram {}", diagramId);
    }
}