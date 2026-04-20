package com.example.react_flow_be.service;

import java.time.LocalDateTime;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import static org.mockito.ArgumentMatchers.any;
import org.mockito.Mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import org.mockito.junit.jupiter.MockitoExtension;

import com.example.react_flow_be.entity.Collaboration;
import com.example.react_flow_be.entity.Diagram;
import com.example.react_flow_be.repository.CollaborationRepository;
import com.example.react_flow_be.repository.DiagramRepository;

import jakarta.persistence.EntityNotFoundException;

/**
 * TestDiagramManagementService: Unit Test cho JwtTokenProvider (API Gateway)
 * 
 * Mục tiêu: Đảm bảo diagram được quản lý (xóa, khôi phục, xóa vĩnh viễn) chính xác
 * 
 * Coverage Level 2 (Branch Coverage):
 * - softDeleteDiagram: test xóa mềm thành công, diagram không tồn tại, user không phải owner
 * - restoreDiagram: test khôi phục thành công, diagram không tồn tại, user không phải owner
 * - permanentlyDeleteDiagram: test xóa vĩnh viễn, diagram không tồn tại, user không phải owner
 * - isOwner: test user là owner, user không phải owner, diagram không tồn tại
 * - getTrashCount: test đếm diagram đã xóa của user
 * - getDaysUntilAutoDelete: test tính số ngày còn lại để xóa tự động
 */
@DisplayName("TestDiagramManagementService - Diagram Management (UC02)")
@ExtendWith(MockitoExtension.class)
public class TestDiagramManagementService {

    private DiagramManagementService diagramManagementService;

    @Mock
    private DiagramRepository diagramRepository;

    @Mock
    private CollaborationRepository collaborationRepository;

    private Diagram validDiagram;
    private Collaboration ownerCollaboration;
    private final Long diagramId = 1L;
    private final String ownerUsername = "owner_user";
    private final String otherUsername = "other_user";
    private final int AUTO_DELETE_DAYS = 30;

    @BeforeEach
    public void setUp() {
        diagramManagementService = new DiagramManagementService(diagramRepository, collaborationRepository);

        // Create valid diagram
        validDiagram = new Diagram();
        validDiagram.setId(diagramId);
        validDiagram.setName("Test Diagram");
        validDiagram.setDescription("Test Description");
        validDiagram.setIsDeleted(false);
        validDiagram.setDeletedAt(null);

        // Create owner collaboration
        ownerCollaboration = new Collaboration();
        ownerCollaboration.setId(1L);
        ownerCollaboration.setDiagram(validDiagram);
        ownerCollaboration.setUsername(ownerUsername);
        ownerCollaboration.setType(Collaboration.CollaborationType.OWNER);
        ownerCollaboration.setPermission(Collaboration.Permission.FULL_ACCESS);
        ownerCollaboration.setIsActive(true);
    }

    // ==================== Test softDeleteDiagram ====================

    @Test
    @DisplayName("softDeleteDiagram_testChuan1 - Soft delete diagram successfully by owner")
    public void softDeleteDiagram_testChuan1() {
        // Standard case: owner deletes diagram
        when(diagramRepository.findById(diagramId)).thenReturn(Optional.of(validDiagram));
        when(collaborationRepository.findByDiagramIdAndType(diagramId, Collaboration.CollaborationType.OWNER))
                .thenReturn(Optional.of(ownerCollaboration));
        when(diagramRepository.save(any(Diagram.class))).thenReturn(validDiagram);

        assertDoesNotThrow(() -> diagramManagementService.softDeleteDiagram(diagramId, ownerUsername));

        assertTrue(validDiagram.getIsDeleted());
        assertNotNull(validDiagram.getDeletedAt());
        verify(diagramRepository).save(validDiagram);
    }

    @Test
    @DisplayName("softDeleteDiagram_ngoaile1 - Soft delete non-existent diagram throws exception")
    public void softDeleteDiagram_ngoaile1() {
        // Error case: diagram not found
        when(diagramRepository.findById(diagramId)).thenReturn(Optional.empty());

        assertThrows(EntityNotFoundException.class, 
                () -> diagramManagementService.softDeleteDiagram(diagramId, ownerUsername));

        verify(diagramRepository, never()).save(any());
    }

    @Test
    @DisplayName("softDeleteDiagram_ngoaile2 - Non-owner cannot delete diagram")
    public void softDeleteDiagram_ngoaile2() {
        // Error case: user is not owner
        when(diagramRepository.findById(diagramId)).thenReturn(Optional.of(validDiagram));
        when(collaborationRepository.findByDiagramIdAndType(diagramId, Collaboration.CollaborationType.OWNER))
                .thenReturn(Optional.of(ownerCollaboration));

        assertThrows(IllegalStateException.class, 
                () -> diagramManagementService.softDeleteDiagram(diagramId, otherUsername));

        verify(diagramRepository, never()).save(any());
    }

    @Test
    @DisplayName("softDeleteDiagram_ngoaile3 - Owner collaboration not found throws exception")
    public void softDeleteDiagram_ngoaile3() {
        // Error case: no owner collaboration found
        when(diagramRepository.findById(diagramId)).thenReturn(Optional.of(validDiagram));
        when(collaborationRepository.findByDiagramIdAndType(diagramId, Collaboration.CollaborationType.OWNER))
                .thenReturn(Optional.empty());

        assertThrows(IllegalStateException.class, 
                () -> diagramManagementService.softDeleteDiagram(diagramId, ownerUsername));

        verify(diagramRepository, never()).save(any());
    }

    // ==================== Test restoreDiagram ====================

    @Test
    @DisplayName("restoreDiagram_testChuan1 - Restore diagram successfully by owner")
    public void restoreDiagram_testChuan1() {
        // Standard case: owner restores diagram
        validDiagram.setIsDeleted(true);
        validDiagram.setDeletedAt(LocalDateTime.now());

        when(diagramRepository.findById(diagramId)).thenReturn(Optional.of(validDiagram));
        when(collaborationRepository.findByDiagramIdAndType(diagramId, Collaboration.CollaborationType.OWNER))
                .thenReturn(Optional.of(ownerCollaboration));
        when(diagramRepository.save(any(Diagram.class))).thenReturn(validDiagram);

        assertDoesNotThrow(() -> diagramManagementService.restoreDiagram(diagramId, ownerUsername));

        assertFalse(validDiagram.getIsDeleted());
        assertNull(validDiagram.getDeletedAt());
        verify(diagramRepository).save(validDiagram);
    }

    @Test
    @DisplayName("restoreDiagram_ngoaile1 - Restore non-existent diagram throws exception")
    public void restoreDiagram_ngoaile1() {
        // Error case: diagram not found
        when(diagramRepository.findById(diagramId)).thenReturn(Optional.empty());

        assertThrows(EntityNotFoundException.class, 
                () -> diagramManagementService.restoreDiagram(diagramId, ownerUsername));

        verify(diagramRepository, never()).save(any());
    }

    @Test
    @DisplayName("restoreDiagram_ngoaile2 - Non-owner cannot restore diagram")
    public void restoreDiagram_ngoaile2() {
        // Error case: user is not owner
        validDiagram.setIsDeleted(true);

        when(diagramRepository.findById(diagramId)).thenReturn(Optional.of(validDiagram));
        when(collaborationRepository.findByDiagramIdAndType(diagramId, Collaboration.CollaborationType.OWNER))
                .thenReturn(Optional.of(ownerCollaboration));

        assertThrows(IllegalStateException.class, 
                () -> diagramManagementService.restoreDiagram(diagramId, otherUsername));

        verify(diagramRepository, never()).save(any());
    }

    @Test
    @DisplayName("restoreDiagram_ngoaile3 - Owner collaboration not found throws exception")
    public void restoreDiagram_ngoaile3() {
        // Error case: no owner collaboration found
        when(diagramRepository.findById(diagramId)).thenReturn(Optional.of(validDiagram));
        when(collaborationRepository.findByDiagramIdAndType(diagramId, Collaboration.CollaborationType.OWNER))
                .thenReturn(Optional.empty());

        assertThrows(IllegalStateException.class, 
                () -> diagramManagementService.restoreDiagram(diagramId, ownerUsername));

        verify(diagramRepository, never()).save(any());
    }

    // ==================== Test permanentlyDeleteDiagram ====================

    @Test
    @DisplayName("permanentlyDeleteDiagram_testChuan1 - Permanently delete diagram successfully")
    public void permanentlyDeleteDiagram_testChuan1() {
        // Standard case: owner permanently deletes soft-deleted diagram
        validDiagram.setIsDeleted(true);
        validDiagram.setDeletedAt(LocalDateTime.now());

        when(diagramRepository.findById(diagramId)).thenReturn(Optional.of(validDiagram));
        when(collaborationRepository.findByDiagramIdAndType(diagramId, Collaboration.CollaborationType.OWNER))
                .thenReturn(Optional.of(ownerCollaboration));

        assertDoesNotThrow(() -> diagramManagementService.permanentlyDeleteDiagram(diagramId, ownerUsername));

        verify(diagramRepository).deleteById(diagramId);
    }

    @Test
    @DisplayName("permanentlyDeleteDiagram_ngoaile1 - Permanently delete non-existent diagram throws exception")
    public void permanentlyDeleteDiagram_ngoaile1() {
        // Error case: diagram not found
        when(diagramRepository.findById(diagramId)).thenReturn(Optional.empty());

        assertThrows(EntityNotFoundException.class, 
                () -> diagramManagementService.permanentlyDeleteDiagram(diagramId, ownerUsername));

        verify(diagramRepository, never()).deleteById(any());
    }

    @Test
    @DisplayName("permanentlyDeleteDiagram_ngoaile2 - Non-owner cannot permanently delete")
    public void permanentlyDeleteDiagram_ngoaile2() {
        // Error case: user is not owner
        validDiagram.setIsDeleted(true);

        when(diagramRepository.findById(diagramId)).thenReturn(Optional.of(validDiagram));
        when(collaborationRepository.findByDiagramIdAndType(diagramId, Collaboration.CollaborationType.OWNER))
                .thenReturn(Optional.of(ownerCollaboration));

        assertThrows(IllegalStateException.class, 
                () -> diagramManagementService.permanentlyDeleteDiagram(diagramId, otherUsername));

        verify(diagramRepository, never()).deleteById(any());
    }

    @Test
    @DisplayName("permanentlyDeleteDiagram_ngoaile3 - Owner collaboration not found throws exception")
    public void permanentlyDeleteDiagram_ngoaile3() {
        // Error case: no owner collaboration
        validDiagram.setIsDeleted(true);

        when(diagramRepository.findById(diagramId)).thenReturn(Optional.of(validDiagram));
        when(collaborationRepository.findByDiagramIdAndType(diagramId, Collaboration.CollaborationType.OWNER))
                .thenReturn(Optional.empty());

        assertThrows(IllegalStateException.class, 
                () -> diagramManagementService.permanentlyDeleteDiagram(diagramId, ownerUsername));

        verify(diagramRepository, never()).deleteById(any());
    }

    @Test
    @DisplayName("permanentlyDeleteDiagram_ngoaile4 - Cannot permanently delete non-deleted diagram")
    public void permanentlyDeleteDiagram_ngoaile4() {
        // Error case: diagram is not in trash
        validDiagram.setIsDeleted(false);

        when(diagramRepository.findById(diagramId)).thenReturn(Optional.of(validDiagram));
        when(collaborationRepository.findByDiagramIdAndType(diagramId, Collaboration.CollaborationType.OWNER))
                .thenReturn(Optional.of(ownerCollaboration));

        assertThrows(IllegalStateException.class, 
                () -> diagramManagementService.permanentlyDeleteDiagram(diagramId, ownerUsername));

        verify(diagramRepository, never()).deleteById(any());
    }

    // ==================== Test isOwner ====================

    @Test
    @DisplayName("isOwner_testChuan1 - User is owner returns true")
    public void isOwner_testChuan1() {
        // Standard case: user is owner
        when(diagramRepository.findById(diagramId)).thenReturn(Optional.of(validDiagram));
        when(collaborationRepository.findByDiagramIdAndType(diagramId, Collaboration.CollaborationType.OWNER))
                .thenReturn(Optional.of(ownerCollaboration));

        boolean result = diagramManagementService.isOwner(diagramId, ownerUsername);

        assertTrue(result);
    }

    @Test
    @DisplayName("isOwner_ngoaile1 - User is not owner returns false")
    public void isOwner_ngoaile1() {
        // Error case: user is not owner
        when(diagramRepository.findById(diagramId)).thenReturn(Optional.of(validDiagram));
        when(collaborationRepository.findByDiagramIdAndType(diagramId, Collaboration.CollaborationType.OWNER))
                .thenReturn(Optional.of(ownerCollaboration));

        boolean result = diagramManagementService.isOwner(diagramId, otherUsername);

        assertFalse(result);
    }

    @Test
    @DisplayName("isOwner_ngoaile2 - Diagram not found returns false")
    public void isOwner_ngoaile2() {
        // Error case: diagram not found
        when(diagramRepository.findById(diagramId)).thenReturn(Optional.empty());

        boolean result = diagramManagementService.isOwner(diagramId, ownerUsername);

        assertFalse(result);
    }

    @Test
    @DisplayName("isOwner_ngoaile3 - Owner collaboration not found returns false")
    public void isOwner_ngoaile3() {
        // Error case: no owner collaboration
        when(diagramRepository.findById(diagramId)).thenReturn(Optional.of(validDiagram));
        when(collaborationRepository.findByDiagramIdAndType(diagramId, Collaboration.CollaborationType.OWNER))
                .thenReturn(Optional.empty());

        boolean result = diagramManagementService.isOwner(diagramId, ownerUsername);

        assertFalse(result);
    }

    // ==================== Test getTrashCount ====================

    @Test
    @DisplayName("getTrashCount_testChuan1 - Get trash count for user")
    public void getTrashCount_testChuan1() {
        // Standard case: user has diagrams in trash
        when(diagramRepository.countByIsDeletedTrueAndDeletedAtBefore(any(LocalDateTime.class)))
                .thenReturn(3L);

        long result = diagramManagementService.getTrashCount(ownerUsername);

        assertEquals(3L, result);
    }

    @Test
    @DisplayName("getTrashCount_testChuan2 - No diagrams in trash returns zero")
    public void getTrashCount_testChuan2() {
        // Standard case: user has no diagrams in trash
        when(diagramRepository.countByIsDeletedTrueAndDeletedAtBefore(any(LocalDateTime.class)))
                .thenReturn(0L);

        long result = diagramManagementService.getTrashCount(ownerUsername);

        assertEquals(0L, result);
    }

    // ==================== Test getDaysUntilAutoDelete ====================

    @Test
    @DisplayName("getDaysUntilAutoDelete_testChuan1 - Calculate days until auto-delete")
    public void getDaysUntilAutoDelete_testChuan1() {
        // Standard case: diagram was deleted 6 days ago
        LocalDateTime deletedAt = LocalDateTime.now().minusDays(6);
        validDiagram.setIsDeleted(true);
        validDiagram.setDeletedAt(deletedAt);

        when(diagramRepository.findById(diagramId)).thenReturn(Optional.of(validDiagram));

        Long result = diagramManagementService.getDaysUntilAutoDelete(diagramId);

        assertNotNull(result);
        // Should have positive days remaining (less than 30)
        assertTrue(result > 0 && result < AUTO_DELETE_DAYS);
    }

    @Test
    @DisplayName("getDaysUntilAutoDelete_testChuan2 - Already expired returns zero")
    public void getDaysUntilAutoDelete_testChuan2() {
        // Standard case: diagram deleted more than AUTO_DELETE_DAYS ago
        LocalDateTime deletedAt = LocalDateTime.now().minusDays(40);
        validDiagram.setIsDeleted(true);
        validDiagram.setDeletedAt(deletedAt);

        when(diagramRepository.findById(diagramId)).thenReturn(Optional.of(validDiagram));

        Long result = diagramManagementService.getDaysUntilAutoDelete(diagramId);

        assertNotNull(result);
        assertEquals(0L, result.longValue());
    }

    @Test
    @DisplayName("getDaysUntilAutoDelete_ngoaile1 - Diagram not found throws exception")
    public void getDaysUntilAutoDelete_ngoaile1() {
        // Error case: diagram not found
        when(diagramRepository.findById(diagramId)).thenReturn(Optional.empty());

        assertThrows(EntityNotFoundException.class, 
                () -> diagramManagementService.getDaysUntilAutoDelete(diagramId));
    }

    @Test
    @DisplayName("getDaysUntilAutoDelete_ngoaile2 - Diagram not deleted returns null")
    public void getDaysUntilAutoDelete_ngoaile2() {
        // Error case: diagram is not deleted
        validDiagram.setIsDeleted(false);
        validDiagram.setDeletedAt(null);

        when(diagramRepository.findById(diagramId)).thenReturn(Optional.of(validDiagram));

        Long result = diagramManagementService.getDaysUntilAutoDelete(diagramId);

        assertNull(result);
    }

    // ==================== Integration Tests ====================

    @Test
    @DisplayName("integration_testChuan1 - Full soft delete and restore flow")
    public void integration_testChuan1() {
        // Integration test: soft delete then restore
        when(diagramRepository.findById(diagramId)).thenReturn(Optional.of(validDiagram));
        when(collaborationRepository.findByDiagramIdAndType(diagramId, Collaboration.CollaborationType.OWNER))
                .thenReturn(Optional.of(ownerCollaboration));
        when(diagramRepository.save(any(Diagram.class))).thenReturn(validDiagram);

        // Soft delete
        assertDoesNotThrow(() -> diagramManagementService.softDeleteDiagram(diagramId, ownerUsername));
        assertTrue(validDiagram.getIsDeleted());

        // Restore
        assertDoesNotThrow(() -> diagramManagementService.restoreDiagram(diagramId, ownerUsername));
        assertFalse(validDiagram.getIsDeleted());

        verify(diagramRepository, times(2)).save(validDiagram);
    }

    @Test
    @DisplayName("integration_testChuan2 - Check owner before deletion")
    public void integration_testChuan2() {
        // Integration test: verify owner before deleting
        when(diagramRepository.findById(diagramId)).thenReturn(Optional.of(validDiagram));
        when(collaborationRepository.findByDiagramIdAndType(diagramId, Collaboration.CollaborationType.OWNER))
                .thenReturn(Optional.of(ownerCollaboration));

        assertTrue(diagramManagementService.isOwner(diagramId, ownerUsername));
        assertFalse(diagramManagementService.isOwner(diagramId, otherUsername));
    }
}
