package com.example.react_flow_be.service;

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
import java.util.Optional;

@Service
@RequiredArgsConstructor
@Slf4j
public class DiagramManagementService {

    private final DiagramRepository diagramRepository;
    private final CollaborationRepository collaborationRepository;

    /**
     * Soft delete diagram (move to trash)
     * Only owner can delete
     */
    @Transactional
    public void softDeleteDiagram(Long diagramId, String username) {
        log.info("Soft deleting diagram {} by user {}", diagramId, username);

        Diagram diagram = diagramRepository.findById(diagramId)
                .orElseThrow(() -> new EntityNotFoundException("Diagram not found with id: " + diagramId));

        // Check if user is owner
        Optional<Collaboration> ownerCollaboration = collaborationRepository
                .findByDiagramIdAndType(diagramId, Collaboration.CollaborationType.OWNER);

        if (ownerCollaboration.isEmpty() || !ownerCollaboration.get().getUsername().equals(username)) {
            throw new IllegalStateException("Only owner can delete diagram");
        }

        // ⭐ Set isDeleted to true và ghi lại thời điểm xóa
        diagram.setIsDeleted(true);
        diagram.setDeletedAt(LocalDateTime.now());
        diagramRepository.save(diagram);

        log.info("Diagram {} moved to trash by user {} at {}",
                diagramId, username, diagram.getDeletedAt());
    }

    /**
     * Restore diagram from trash
     * Only owner can restore
     */
    @Transactional
    public void restoreDiagram(Long diagramId, String username) {
        log.info("Restoring diagram {} by user {}", diagramId, username);

        Diagram diagram = diagramRepository.findById(diagramId)
                .orElseThrow(() -> new EntityNotFoundException("Diagram not found with id: " + diagramId));

        // Check if user is owner
        Optional<Collaboration> ownerCollaboration = collaborationRepository
                .findByDiagramIdAndType(diagramId, Collaboration.CollaborationType.OWNER);

        if (ownerCollaboration.isEmpty() || !ownerCollaboration.get().getUsername().equals(username)) {
            throw new IllegalStateException("Only owner can restore diagram");
        }

        // ⭐ Set isDeleted to false và xóa deletedAt
        diagram.setIsDeleted(false);
        diagram.setDeletedAt(null);
        diagramRepository.save(diagram);

        log.info("Diagram {} restored from trash by user {}", diagramId, username);
    }

    /**
     * Permanently delete diagram
     * Only owner can permanently delete
     * This will delete all related data (collaborations, models, attributes,
     * connections, migrations)
     */
    @Transactional
    public void permanentlyDeleteDiagram(Long diagramId, String username) {
        log.info("Permanently deleting diagram {} by user {}", diagramId, username);

        Diagram diagram = diagramRepository.findById(diagramId)
                .orElseThrow(() -> new EntityNotFoundException("Diagram not found with id: " + diagramId));

        // Check if user is owner
        Optional<Collaboration> ownerCollaboration = collaborationRepository
                .findByDiagramIdAndType(diagramId, Collaboration.CollaborationType.OWNER);

        if (ownerCollaboration.isEmpty() || !ownerCollaboration.get().getUsername().equals(username)) {
            throw new IllegalStateException("Only owner can permanently delete diagram");
        }

        // Check if diagram is in trash
        if (!diagram.getIsDeleted()) {
            throw new IllegalStateException("Diagram must be in trash before permanent deletion");
        }

        // Delete diagram (cascade will handle related entities)
        diagramRepository.delete(diagram);

        log.info("Diagram {} permanently deleted by user {}", diagramId, username);
    }

    /**
     * Check if user is owner of diagram
     */
    @Transactional(readOnly = true)
    public boolean isOwner(Long diagramId, String username) {
        Optional<Collaboration> ownerCollaboration = collaborationRepository
                .findByDiagramIdAndType(diagramId, Collaboration.CollaborationType.OWNER);

        return ownerCollaboration.isPresent()
                && ownerCollaboration.get().getUsername().equals(username);
    }

    /**
     * Get trash count for user
     */
    @Transactional(readOnly = true)
    public Long getTrashCount(String username) {
        // This would need a custom query to count deleted diagrams where user is owner
        return diagramRepository.countByIsDeleted(true);
    }

    /**
     * ⭐ NEW: Get days remaining before auto-delete
     */
    @Transactional(readOnly = true)
    public Long getDaysUntilAutoDelete(Long diagramId) {
        Diagram diagram = diagramRepository.findById(diagramId)
                .orElseThrow(() -> new EntityNotFoundException("Diagram not found"));

        if (!diagram.getIsDeleted() || diagram.getDeletedAt() == null) {
            return null; // Not in trash
        }

        LocalDateTime autoDeleteDate = diagram.getDeletedAt().plusDays(7);
        LocalDateTime now = LocalDateTime.now();

        if (now.isAfter(autoDeleteDate)) {
            return 0L; // Already expired
        }

        return java.time.Duration.between(now, autoDeleteDate).toDays();
    }
}