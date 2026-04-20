package com.example.react_flow_be.service;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import static org.mockito.ArgumentMatchers.any;
import org.mockito.Mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import org.mockito.junit.jupiter.MockitoExtension;

import com.example.react_flow_be.config.DiagramSessionManager;
import com.example.react_flow_be.config.WebSocketSessionTracker;
import com.example.react_flow_be.dto.collaboration.CollaborationDTO;
import com.example.react_flow_be.entity.Collaboration;
import com.example.react_flow_be.entity.Diagram;
import com.example.react_flow_be.repository.CollaborationRepository;
import com.example.react_flow_be.repository.DiagramRepository;

import jakarta.persistence.EntityNotFoundException;

/**
 * TestCollaborationService: Unit Test cho CollaborationService (Diagram Collaboration)
 * 
 * Mục tiêu: Đảm bảo collaboration được quản lý chính xác (thêm, sửa quyền, xóa, kiểm tra quyền)
 * 
 * Coverage Level 2 (Branch Coverage):
 * - getCollaborations: lấy danh sách collaboration
 * - addCollaborator: thêm collaborator thành công, user đã tồn tại, diagram không tồn tại
 * - updatePermission: cập nhật quyền thành công, collaboration không tồn tại
 * - removeCollaborator: xóa collaborator, collaboration không tồn tại
 * - hasAccess: user có quyền, user không có quyền
 * - getOwner: lấy owner, không có owner
 * - countParticipants: đếm participants
 */
@DisplayName("TestCollaborationService - Diagram Collaboration (UC02)")
@ExtendWith(MockitoExtension.class)
public class TestCollaborationService {

    private CollaborationService collaborationService;

    @Mock
    private CollaborationRepository collaborationRepository;

    @Mock
    private DiagramRepository diagramRepository;

    @Mock
    private DiagramSessionManager diagramSessionManager;

    @Mock
    private WebSocketSessionTracker webSocketSessionTracker;

    private Diagram validDiagram;
    private Collaboration ownerCollaboration;
    private Collaboration participantCollaboration;
    private final Long diagramId = 1L;
    private final Long collaborationId = 1L;
    private final String ownerUsername = "owner_user";
    private final String participantUsername = "participant_user";
    private final String newUsername = "new_user";

    @BeforeEach
    public void setUp() {
        collaborationService = new CollaborationService(
                collaborationRepository,
                diagramRepository,
                diagramSessionManager,
                webSocketSessionTracker
        );

        // Create valid diagram
        validDiagram = new Diagram();
        validDiagram.setId(diagramId);
        validDiagram.setName("Test Diagram");
        validDiagram.setCreatedAt(LocalDateTime.now());

        // Create owner collaboration
        ownerCollaboration = new Collaboration();
        ownerCollaboration.setId(1L);
        ownerCollaboration.setDiagram(validDiagram);
        ownerCollaboration.setUsername(ownerUsername);
        ownerCollaboration.setType(Collaboration.CollaborationType.OWNER);
        ownerCollaboration.setPermission(Collaboration.Permission.FULL_ACCESS);
        ownerCollaboration.setIsActive(true);
        ownerCollaboration.setCreatedAt(LocalDateTime.now());

        // Create participant collaboration
        participantCollaboration = new Collaboration();
        participantCollaboration.setId(2L);
        participantCollaboration.setDiagram(validDiagram);
        participantCollaboration.setUsername(participantUsername);
        participantCollaboration.setType(Collaboration.CollaborationType.PARTICIPANTS);
        participantCollaboration.setPermission(Collaboration.Permission.EDIT);
        participantCollaboration.setIsActive(true);
        participantCollaboration.setCreatedAt(LocalDateTime.now());
    }

    // ==================== Test getCollaborations ====================

    @Test
    @DisplayName("getCollaborations_testChuan1 - Get all collaborations for diagram")
    public void getCollaborations_testChuan1() {
        // Standard case: diagram has multiple collaborations
        List<Collaboration> collaborations = new ArrayList<>();
        collaborations.add(ownerCollaboration);
        collaborations.add(participantCollaboration);

        when(diagramRepository.existsById(diagramId)).thenReturn(true);
        when(collaborationRepository.findByDiagramId(diagramId)).thenReturn(collaborations);

        List<CollaborationDTO> result = collaborationService.getCollaborations(diagramId);

        assertNotNull(result);
        assertEquals(2, result.size());
        verify(collaborationRepository).findByDiagramId(diagramId);
    }

    @Test
    @DisplayName("getCollaborations_testChuan2 - Diagram with only owner")
    public void getCollaborations_testChuan2() {
        // Standard case: only owner collaboration
        List<Collaboration> collaborations = new ArrayList<>();
        collaborations.add(ownerCollaboration);

        when(diagramRepository.existsById(diagramId)).thenReturn(true);
        when(collaborationRepository.findByDiagramId(diagramId)).thenReturn(collaborations);

        List<CollaborationDTO> result = collaborationService.getCollaborations(diagramId);

        assertNotNull(result);
        assertEquals(1, result.size());
    }

    @Test
    @DisplayName("getCollaborations_ngoaile1 - No collaborations returns empty list")
    public void getCollaborations_ngoaile1() {
        // Error case: no collaborations
        when(diagramRepository.existsById(diagramId)).thenReturn(true);
        when(collaborationRepository.findByDiagramId(diagramId)).thenReturn(new ArrayList<>());

        List<CollaborationDTO> result = collaborationService.getCollaborations(diagramId);

        assertNotNull(result);
        assertTrue(result.isEmpty());
    }

    // ==================== Test addCollaborator ====================

    @Test
    @DisplayName("addCollaborator_testChuan1 - Add new collaborator successfully")
    public void addCollaborator_testChuan1() {
        // Standard case: add new user as collaborator
        Collaboration newCollaboration = new Collaboration();
        newCollaboration.setId(3L);
        newCollaboration.setUsername(newUsername);
        newCollaboration.setDiagram(validDiagram);
        newCollaboration.setPermission(Collaboration.Permission.VIEW);
        newCollaboration.setType(Collaboration.CollaborationType.PARTICIPANTS);
        newCollaboration.setIsActive(true);

        when(diagramRepository.findById(diagramId)).thenReturn(Optional.of(validDiagram));
        when(collaborationRepository.findByDiagramIdAndUsername(diagramId, newUsername))
                .thenReturn(Optional.empty());
        when(collaborationRepository.save(any(Collaboration.class))).thenReturn(newCollaboration);

        CollaborationDTO result = collaborationService.addCollaborator(
                diagramId, newUsername, Collaboration.Permission.VIEW);

        assertNotNull(result);
        assertEquals(newUsername, result.getUsername());
        verify(collaborationRepository).save(any(Collaboration.class));
    }

    @Test
    @DisplayName("addCollaborator_ngoaile1 - Diagram not found throws exception")
    public void addCollaborator_ngoaile1() {
        // Error case: diagram not found
        when(diagramRepository.findById(diagramId)).thenReturn(Optional.empty());

        assertThrows(EntityNotFoundException.class, 
                () -> collaborationService.addCollaborator(diagramId, newUsername, Collaboration.Permission.VIEW));

        verify(collaborationRepository, never()).save(any());
    }

    @Test
    @DisplayName("addCollaborator_ngoaile2 - User already collaborator throws exception")
    public void addCollaborator_ngoaile2() {
        // Error case: user is already a collaborator
        when(diagramRepository.findById(diagramId)).thenReturn(Optional.of(validDiagram));
        when(collaborationRepository.findByDiagramIdAndUsername(diagramId, participantUsername))
                .thenReturn(Optional.of(participantCollaboration));

        assertThrows(IllegalArgumentException.class, 
                () -> collaborationService.addCollaborator(diagramId, participantUsername, Collaboration.Permission.EDIT));

        verify(collaborationRepository, never()).save(any());
    }

    // ==================== Test updatePermission ====================

    @Test
    @DisplayName("updatePermission_testChuan1 - Update permission successfully")
    public void updatePermission_testChuan1() {
        // Standard case: update collaborator permission
        when(collaborationRepository.findById(2L)).thenReturn(Optional.of(participantCollaboration));
        when(collaborationRepository.save(any(Collaboration.class))).thenReturn(participantCollaboration);

        assertDoesNotThrow(() -> 
                collaborationService.updatePermission(2L, Collaboration.Permission.COMMENT));

        verify(collaborationRepository).save(participantCollaboration);
    }

    @Test
    @DisplayName("updatePermission_ngoaile1 - Collaboration not found throws exception")
    public void updatePermission_ngoaile1() {
        // Error case: collaboration not found
        when(collaborationRepository.findById(999L)).thenReturn(Optional.empty());

        assertThrows(EntityNotFoundException.class, 
                () -> collaborationService.updatePermission(999L, Collaboration.Permission.VIEW));

        verify(collaborationRepository, never()).save(any());
    }

    // ==================== Test removeCollaborator ====================

    @Test
    @DisplayName("removeCollaborator_testChuan1 - Remove collaborator successfully")
    public void removeCollaborator_testChuan1() {
        // Standard case: remove participant collaborator
        when(collaborationRepository.findById(2L)).thenReturn(Optional.of(participantCollaboration));

        assertDoesNotThrow(() -> collaborationService.removeCollaborator(2L));

        verify(collaborationRepository).deleteById(2L);
    }

    @Test
    @DisplayName("removeCollaborator_ngoaile1 - Collaboration not found throws exception")
    public void removeCollaborator_ngoaile1() {
        // Error case: collaboration not found
        when(collaborationRepository.findById(999L)).thenReturn(Optional.empty());

        assertThrows(EntityNotFoundException.class, 
                () -> collaborationService.removeCollaborator(999L));

        verify(collaborationRepository, never()).deleteById(any());
    }

    // ==================== Test hasAccess ====================

    @Test
    @DisplayName("hasAccess_testChuan1 - User with active collaboration has access")
    public void hasAccess_testChuan1() {
        // Standard case: active user has access
        when(collaborationRepository.findByDiagramIdAndUsername(diagramId, ownerUsername))
                .thenReturn(Optional.of(ownerCollaboration));

        boolean result = collaborationService.hasAccess(diagramId, ownerUsername);

        assertTrue(result);
    }

    @Test
    @DisplayName("hasAccess_testChuan2 - User without collaboration has no access")
    public void hasAccess_testChuan2() {
        // Standard case: user not in collaboration
        when(collaborationRepository.findByDiagramIdAndUsername(diagramId, "unknown_user"))
                .thenReturn(Optional.empty());

        boolean result = collaborationService.hasAccess(diagramId, "unknown_user");

        assertFalse(result);
    }

    @Test
    @DisplayName("hasAccess_ngoaile1 - Inactive collaboration has no access")
    public void hasAccess_ngoaile1() {
        // Error case: collaboration is inactive
        participantCollaboration.setIsActive(false);
        when(collaborationRepository.findByDiagramIdAndUsername(diagramId, participantUsername))
                .thenReturn(Optional.of(participantCollaboration));

        boolean result = collaborationService.hasAccess(diagramId, participantUsername);

        assertFalse(result);
    }

    // ==================== Test getUserCollaboration ====================

    @Test
    @DisplayName("getUserCollaboration_testChuan1 - Get user collaboration successfully")
    public void getUserCollaboration_testChuan1() {
        // Standard case: retrieve user's collaboration
        when(collaborationRepository.findActiveCollaboration(diagramId, ownerUsername))
                .thenReturn(Optional.of(ownerCollaboration));

        CollaborationDTO result = collaborationService.getUserCollaboration(diagramId, ownerUsername);

        assertNotNull(result);
        assertEquals(ownerUsername, result.getUsername());
    }

    @Test
    @DisplayName("getUserCollaboration_ngoaile1 - User collaboration not found throws exception")
    public void getUserCollaboration_ngoaile1() {
        // Error case: collaboration not found
        when(collaborationRepository.findByDiagramIdAndUsername(diagramId, "unknown_user"))
                .thenReturn(Optional.empty());

        assertThrows(EntityNotFoundException.class, 
                () -> collaborationService.getUserCollaboration(diagramId, "unknown_user"));
    }

    // ==================== Test getOwner ====================

    @Test
    @DisplayName("getOwner_testChuan1 - Get diagram owner successfully")
    public void getOwner_testChuan1() {
        // Standard case: retrieve owner
        when(collaborationRepository.findByDiagramIdAndType(diagramId, Collaboration.CollaborationType.OWNER))
                .thenReturn(Optional.of(ownerCollaboration));

        CollaborationDTO result = collaborationService.getOwner(diagramId);

        assertNotNull(result);
        assertEquals(ownerUsername, result.getUsername());
    }

    @Test
    @DisplayName("getOwner_ngoaile1 - Diagram not found throws exception")
    public void getOwner_ngoaile1() {
        // Error case: no owner collaboration exists
        when(collaborationRepository.findByDiagramIdAndType(diagramId, Collaboration.CollaborationType.OWNER))
                .thenReturn(Optional.empty());

        assertThrows(EntityNotFoundException.class, 
                () -> collaborationService.getOwner(diagramId));
    }

    @Test
    @DisplayName("getOwner_ngoaile2 - No owner found throws exception")
    public void getOwner_ngoaile2() {
        // Error case: no owner collaboration exists
        when(collaborationRepository.findByDiagramIdAndType(diagramId, Collaboration.CollaborationType.OWNER))
                .thenReturn(Optional.empty());

        assertThrows(EntityNotFoundException.class, 
                () -> collaborationService.getOwner(diagramId));
    }

    // ==================== Test countParticipants ====================

    @Test
    @DisplayName("countParticipants_testChuan1 - Count multiple participants")
    public void countParticipants_testChuan1() {
        // Standard case: diagram has multiple participants
        when(collaborationRepository.countParticipants(diagramId)).thenReturn(5);

        long result = collaborationService.countParticipants(diagramId);

        assertEquals(5, result);
    }

    @Test
    @DisplayName("countParticipants_testChuan2 - Count with only owner")
    public void countParticipants_testChuan2() {
        // Standard case: only owner, no participants
        when(collaborationRepository.countParticipants(diagramId)).thenReturn(0);

        long result = collaborationService.countParticipants(diagramId);

        assertEquals(0, result);
    }

    // ==================== Test createOwner ====================

    @Test
    @DisplayName("createOwner_testChuan1 - Create owner collaboration successfully")
    public void createOwner_testChuan1() {
        // Standard case: create new owner
        Collaboration newOwner = new Collaboration();
        newOwner.setId(10L);
        newOwner.setUsername(newUsername);
        newOwner.setDiagram(validDiagram);
        newOwner.setType(Collaboration.CollaborationType.OWNER);
        newOwner.setPermission(Collaboration.Permission.FULL_ACCESS);
        newOwner.setIsActive(true);

        when(diagramRepository.findById(diagramId)).thenReturn(Optional.of(validDiagram));
        when(collaborationRepository.save(any(Collaboration.class))).thenReturn(newOwner);

        CollaborationDTO result = collaborationService.createOwner(diagramId, newUsername);

        assertNotNull(result);
        assertEquals(newUsername, result.getUsername());
        assertEquals(Collaboration.CollaborationType.OWNER, result.getType());
        verify(collaborationRepository).save(any(Collaboration.class));
    }

    @Test
    @DisplayName("createOwner_ngoaile1 - Diagram not found throws exception")
    public void createOwner_ngoaile1() {
        // Error case: diagram not found
        when(diagramRepository.findById(diagramId)).thenReturn(Optional.empty());

        assertThrows(EntityNotFoundException.class, 
                () -> collaborationService.createOwner(diagramId, newUsername));

        verify(collaborationRepository, never()).save(any());
    }

    // ==================== Test deactivateCollaboration ====================

    @Test
    @DisplayName("deactivateCollaboration_testChuan1 - Deactivate collaboration successfully")
    public void deactivateCollaboration_testChuan1() {
        // Standard case: deactivate active collaboration
        when(collaborationRepository.findById(2L)).thenReturn(Optional.of(participantCollaboration));
        when(collaborationRepository.save(any(Collaboration.class))).thenReturn(participantCollaboration);

        assertDoesNotThrow(() -> collaborationService.deactivateCollaboration(2L));

        assertFalse(participantCollaboration.getIsActive());
        verify(collaborationRepository).save(participantCollaboration);
    }

    @Test
    @DisplayName("deactivateCollaboration_ngoaile1 - Collaboration not found throws exception")
    public void deactivateCollaboration_ngoaile1() {
        // Error case: collaboration not found
        when(collaborationRepository.findById(999L)).thenReturn(Optional.empty());

        assertThrows(EntityNotFoundException.class, 
                () -> collaborationService.deactivateCollaboration(999L));

        verify(collaborationRepository, never()).save(any());
    }

    // ==================== Test deleteAllByDiagramId ====================

    @Test
    @DisplayName("deleteAllByDiagramId_testChuan1 - Delete all collaborations for diagram")
    public void deleteAllByDiagramId_testChuan1() {
        // Standard case: delete all collaborations
        assertDoesNotThrow(() -> collaborationService.deleteAllByDiagramId(diagramId));

        verify(collaborationRepository).deleteByDiagramId(diagramId);
    }

    // ==================== Integration Tests ====================

    @Test
    @DisplayName("integration_testChuan1 - Full collaboration flow: add, check access, remove")
    public void integration_testChuan1() {
        // Integration test: add collaborator, check access, remove
        Collaboration newCollaboration = new Collaboration();
        newCollaboration.setId(3L);
        newCollaboration.setUsername(newUsername);
        newCollaboration.setDiagram(validDiagram);
        newCollaboration.setPermission(Collaboration.Permission.EDIT);
        newCollaboration.setType(Collaboration.CollaborationType.PARTICIPANTS);
        newCollaboration.setIsActive(true);

        // Add collaborator
        when(diagramRepository.findById(diagramId)).thenReturn(Optional.of(validDiagram));
        when(collaborationRepository.findByDiagramIdAndUsername(diagramId, newUsername))
                .thenReturn(Optional.empty());
        when(collaborationRepository.save(any(Collaboration.class))).thenReturn(newCollaboration);

        CollaborationDTO addResult = collaborationService.addCollaborator(
                diagramId, newUsername, Collaboration.Permission.EDIT);
        assertNotNull(addResult);

        // Check access
        when(collaborationRepository.findByDiagramIdAndUsername(diagramId, newUsername))
                .thenReturn(Optional.of(newCollaboration));
        boolean hasAccess = collaborationService.hasAccess(diagramId, newUsername);
        assertTrue(hasAccess);

        // Remove collaborator
        when(collaborationRepository.findById(3L)).thenReturn(Optional.of(newCollaboration));
        assertDoesNotThrow(() -> collaborationService.removeCollaborator(3L));
        verify(collaborationRepository).deleteById(3L);
    }

    @Test
    @DisplayName("integration_testChuan2 - Get all collaborations and verify owner")
    public void integration_testChuan2() {
        // Integration test: get all collaborations and find owner
        List<Collaboration> collaborations = new ArrayList<>();
        collaborations.add(ownerCollaboration);
        collaborations.add(participantCollaboration);

        when(diagramRepository.findById(diagramId)).thenReturn(Optional.of(validDiagram));
        when(collaborationRepository.findByDiagramId(diagramId)).thenReturn(collaborations);

        List<CollaborationDTO> result = collaborationService.getCollaborations(diagramId);
        assertEquals(2, result.size());

        CollaborationDTO owner = collaborationService.getOwner(diagramId);
        assertEquals(ownerUsername, owner.getUsername());
    }
}
