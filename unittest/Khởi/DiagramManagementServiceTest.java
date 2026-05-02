package com.example.react_flow_be.service;

import com.example.react_flow_be.entity.Collaboration;
import com.example.react_flow_be.entity.Diagram;
import com.example.react_flow_be.repository.CollaborationRepository;
import com.example.react_flow_be.repository.DiagramRepository;
import jakarta.persistence.EntityNotFoundException;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@Slf4j
@ExtendWith(MockitoExtension.class)
public class DiagramManagementServiceTest {

    @Mock
    private DiagramRepository diagramRepository;

    @Mock
    private CollaborationRepository collaborationRepository;

    @InjectMocks
    private DiagramManagementService diagramManagementService;

    // Test data
    private Diagram sampleDiagram;
    private Diagram sampleDiagramInTrash;
    private Collaboration ownerCollaboration;

    @BeforeEach
    public void setUp() {
        log.info("========================================");
        log.info("[SETUP] Khởi tạo dữ liệu test...");

        // Create sample diagram
        sampleDiagram = new Diagram();
        sampleDiagram.setId(1L);
        sampleDiagram.setName("Test Diagram");
        sampleDiagram.setDescription("Test Description");
        sampleDiagram.setIsDeleted(false);
        sampleDiagram.setDeletedAt(null);
        sampleDiagram.setCreatedAt(LocalDateTime.now());
        sampleDiagram.setUpdatedAt(LocalDateTime.now());

        // Create sample diagram in trash
        sampleDiagramInTrash = new Diagram();
        sampleDiagramInTrash.setId(2L);
        sampleDiagramInTrash.setName("Deleted Diagram");
        sampleDiagramInTrash.setDescription("Already in trash");
        sampleDiagramInTrash.setIsDeleted(true);
        sampleDiagramInTrash.setDeletedAt(LocalDateTime.now().minusDays(3));
        sampleDiagramInTrash.setCreatedAt(LocalDateTime.now().minusDays(10));
        sampleDiagramInTrash.setUpdatedAt(LocalDateTime.now().minusDays(3));

        // Create owner collaboration
        ownerCollaboration = new Collaboration();
        ownerCollaboration.setId(1L);
        ownerCollaboration.setUsername("owner_user");
        ownerCollaboration.setType(Collaboration.CollaborationType.OWNER);
        ownerCollaboration.setPermission(Collaboration.Permission.FULL_ACCESS);
        ownerCollaboration.setIsActive(true);

        log.info("[SETUP] Hoàn tất khởi tạo dữ liệu test.");
    }

    @AfterEach
    public void tearDown() {
        log.info("[TEARDOWN] Dọn dẹp dữ liệu test, reset mock objects...");
        reset(diagramRepository, collaborationRepository);
        sampleDiagram = null;
        sampleDiagramInTrash = null;
        ownerCollaboration = null;
        log.info("[TEARDOWN] Hoàn tất dọn dẹp. Trạng thái đã được khôi phục.");
        log.info("========================================\n");
    }

    // ======================== softDeleteDiagram() - 4 tests ========================

    @Test
    public void UT_DMS_001_softDeleteDiagramSuccess() {
        // Arrange: Setup mocks
        when(diagramRepository.findById(1L)).thenReturn(Optional.of(sampleDiagram));
        when(collaborationRepository.findByDiagramIdAndType(1L, Collaboration.CollaborationType.OWNER))
                .thenReturn(Optional.of(ownerCollaboration));
        when(diagramRepository.save(any(Diagram.class))).thenAnswer(invocation -> {
            Diagram diagram = invocation.getArgument(0);
            assertTrue(diagram.getIsDeleted(), "Diagram should be marked as deleted");
            assertNotNull(diagram.getDeletedAt(), "DeletedAt should be set");
            return diagram;
        });

        // Act: Call service method
        diagramManagementService.softDeleteDiagram(1L, "owner_user");

        // Assert: Verify result and mock interactions
        verify(diagramRepository, times(1)).findById(1L);
        verify(collaborationRepository, times(1)).findByDiagramIdAndType(1L, Collaboration.CollaborationType.OWNER);
        verify(diagramRepository, times(1)).save(any(Diagram.class));

        log.info("[UT_DMS_001] response={}", sampleDiagram);
    }

    @Test
    public void UT_DMS_002_softDeleteDiagramNotFound() {
        // Arrange: Setup mocks
        when(diagramRepository.findById(999L)).thenReturn(Optional.empty());

        // Act & Assert: Verify exception is thrown
        EntityNotFoundException exception = assertThrows(EntityNotFoundException.class, () -> {
            diagramManagementService.softDeleteDiagram(999L, "owner_user");
        });

        log.info("[UT_DMS_002] exception={}", exception.getMessage());
    }

    @Test
    public void UT_DMS_003_softDeleteDiagramNotOwner() {
        // Arrange: Setup mocks
        when(diagramRepository.findById(1L)).thenReturn(Optional.of(sampleDiagram));
        when(collaborationRepository.findByDiagramIdAndType(1L, Collaboration.CollaborationType.OWNER))
                .thenReturn(Optional.of(ownerCollaboration));

        // Act & Assert: Verify exception is thrown
        IllegalStateException exception = assertThrows(IllegalStateException.class, () -> {
            diagramManagementService.softDeleteDiagram(1L, "not_owner");
        });

        assertTrue(exception.getMessage().contains("Only owner can delete"), "Should mention owner requirement");
        log.info("[UT_DMS_003] exception={}", exception.getMessage());
    }

    @Test
    public void UT_DMS_004_softDeleteDiagramNoOwner() {
        // Arrange: Setup mocks
        when(diagramRepository.findById(1L)).thenReturn(Optional.of(sampleDiagram));
        when(collaborationRepository.findByDiagramIdAndType(1L, Collaboration.CollaborationType.OWNER))
                .thenReturn(Optional.empty());

        // Act & Assert: Verify exception is thrown
        IllegalStateException exception = assertThrows(IllegalStateException.class, () -> {
            diagramManagementService.softDeleteDiagram(1L, "owner_user");
        });

        log.info("[UT_DMS_004] exception={}", exception.getMessage());
    }

    // ======================== restoreDiagram() - 4 tests ========================

    @Test
    public void UT_DMS_005_restoreDiagramSuccess() {
        // Arrange: Setup mocks
        Collaboration trash_owner = new Collaboration();
        trash_owner.setUsername("owner_user");
        trash_owner.setType(Collaboration.CollaborationType.OWNER);

        when(diagramRepository.findById(2L)).thenReturn(Optional.of(sampleDiagramInTrash));
        when(collaborationRepository.findByDiagramIdAndType(2L, Collaboration.CollaborationType.OWNER))
                .thenReturn(Optional.of(trash_owner));
        when(diagramRepository.save(any(Diagram.class))).thenAnswer(invocation -> {
            Diagram diagram = invocation.getArgument(0);
            assertFalse(diagram.getIsDeleted(), "Diagram should not be marked as deleted");
            assertNull(diagram.getDeletedAt(), "DeletedAt should be null");
            return diagram;
        });

        // Act: Call service method
        diagramManagementService.restoreDiagram(2L, "owner_user");

        // Assert: Verify result and mock interactions
        verify(diagramRepository, times(1)).findById(2L);
        verify(collaborationRepository, times(1)).findByDiagramIdAndType(2L, Collaboration.CollaborationType.OWNER);
        verify(diagramRepository, times(1)).save(any(Diagram.class));

        log.info("[UT_DMS_005] response={}", sampleDiagramInTrash);
    }

    @Test
    public void UT_DMS_006_restoreDiagramNotFound() {
        // Arrange: Setup mocks
        when(diagramRepository.findById(999L)).thenReturn(Optional.empty());

        // Act & Assert: Verify exception is thrown
        EntityNotFoundException exception = assertThrows(EntityNotFoundException.class, () -> {
            diagramManagementService.restoreDiagram(999L, "owner_user");
        });

        log.info("[UT_DMS_006] exception={}", exception.getMessage());
    }

    @Test
    public void UT_DMS_007_restoreDiagramNotOwner() {
        // Arrange: Setup mocks
        when(diagramRepository.findById(2L)).thenReturn(Optional.of(sampleDiagramInTrash));
        when(collaborationRepository.findByDiagramIdAndType(2L, Collaboration.CollaborationType.OWNER))
                .thenReturn(Optional.of(ownerCollaboration));

        // Act & Assert: Verify exception is thrown
        IllegalStateException exception = assertThrows(IllegalStateException.class, () -> {
            diagramManagementService.restoreDiagram(2L, "not_owner");
        });

        log.info("[UT_DMS_007] exception={}", exception.getMessage());
    }

    @Test
    public void UT_DMS_008_restoreDiagramNoOwner() {
        // Arrange: Setup mocks
        when(diagramRepository.findById(2L)).thenReturn(Optional.of(sampleDiagramInTrash));
        when(collaborationRepository.findByDiagramIdAndType(2L, Collaboration.CollaborationType.OWNER))
                .thenReturn(Optional.empty());

        // Act & Assert: Verify exception is thrown
        IllegalStateException exception = assertThrows(IllegalStateException.class, () -> {
            diagramManagementService.restoreDiagram(2L, "owner_user");
        });

        log.info("[UT_DMS_008] exception={}", exception.getMessage());
    }

    // ======================== permanentlyDeleteDiagram() - 5 tests ========================

    @Test
    public void UT_DMS_009_permanentlyDeleteDiagramSuccess() {
        // Arrange: Setup mocks
        Collaboration trash_owner = new Collaboration();
        trash_owner.setUsername("owner_user");
        trash_owner.setType(Collaboration.CollaborationType.OWNER);

        when(diagramRepository.findById(2L)).thenReturn(Optional.of(sampleDiagramInTrash));
        when(collaborationRepository.findByDiagramIdAndType(2L, Collaboration.CollaborationType.OWNER))
                .thenReturn(Optional.of(trash_owner));
        doNothing().when(diagramRepository).delete(sampleDiagramInTrash);

        // Act: Call service method
        diagramManagementService.permanentlyDeleteDiagram(2L, "owner_user");

        // Assert: Verify result and mock interactions
        verify(diagramRepository, times(1)).findById(2L);
        verify(collaborationRepository, times(1)).findByDiagramIdAndType(2L, Collaboration.CollaborationType.OWNER);
        verify(diagramRepository, times(1)).delete(sampleDiagramInTrash);

        log.info("[UT_DMS_009] response=deleted");
    }

    @Test
    public void UT_DMS_010_permanentlyDeleteDiagramNotFound() {
        // Arrange: Setup mocks
        when(diagramRepository.findById(999L)).thenReturn(Optional.empty());

        // Act & Assert: Verify exception is thrown
        EntityNotFoundException exception = assertThrows(EntityNotFoundException.class, () -> {
            diagramManagementService.permanentlyDeleteDiagram(999L, "owner_user");
        });

        log.info("[UT_DMS_010] exception={}", exception.getMessage());
    }

    @Test
    public void UT_DMS_011_permanentlyDeleteDiagramNotOwner() {
        // Arrange: Setup mocks
        when(diagramRepository.findById(2L)).thenReturn(Optional.of(sampleDiagramInTrash));
        when(collaborationRepository.findByDiagramIdAndType(2L, Collaboration.CollaborationType.OWNER))
                .thenReturn(Optional.of(ownerCollaboration));

        // Act & Assert: Verify exception is thrown
        IllegalStateException exception = assertThrows(IllegalStateException.class, () -> {
            diagramManagementService.permanentlyDeleteDiagram(2L, "not_owner");
        });

        log.info("[UT_DMS_011] exception={}", exception.getMessage());
    }

    @Test
    public void UT_DMS_012_permanentlyDeleteDiagramNotInTrash() {
        // Arrange: Setup mocks
        when(diagramRepository.findById(1L)).thenReturn(Optional.of(sampleDiagram));
        when(collaborationRepository.findByDiagramIdAndType(1L, Collaboration.CollaborationType.OWNER))
                .thenReturn(Optional.of(ownerCollaboration));

        // Act & Assert: Verify exception is thrown
        IllegalStateException exception = assertThrows(IllegalStateException.class, () -> {
            diagramManagementService.permanentlyDeleteDiagram(1L, "owner_user");
        });

        assertTrue(exception.getMessage().contains("in trash"), "Should mention trash requirement");
        log.info("[UT_DMS_012] exception={}", exception.getMessage());
    }

    @Test
    public void UT_DMS_013_permanentlyDeleteDiagramNoOwner() {
        // Arrange: Setup mocks
        when(diagramRepository.findById(2L)).thenReturn(Optional.of(sampleDiagramInTrash));
        when(collaborationRepository.findByDiagramIdAndType(2L, Collaboration.CollaborationType.OWNER))
                .thenReturn(Optional.empty());

        // Act & Assert: Verify exception is thrown
        IllegalStateException exception = assertThrows(IllegalStateException.class, () -> {
            diagramManagementService.permanentlyDeleteDiagram(2L, "owner_user");
        });

        log.info("[UT_DMS_013] exception={}", exception.getMessage());
    }

    // ======================== isOwner() - 2 tests ========================

    @Test
    public void UT_DMS_014_isOwnerTrue() {
        // Arrange: Setup mocks
        when(collaborationRepository.findByDiagramIdAndType(1L, Collaboration.CollaborationType.OWNER))
                .thenReturn(Optional.of(ownerCollaboration));

        // Act: Call service method
        boolean result = diagramManagementService.isOwner(1L, "owner_user");

        // Assert: Verify result
        assertTrue(result, "Should return true for owner");
        verify(collaborationRepository, times(1)).findByDiagramIdAndType(1L, Collaboration.CollaborationType.OWNER);

        log.info("[UT_DMS_014] response={}", result);
    }

    @Test
    public void UT_DMS_015_isOwnerFalse() {
        // Arrange: Setup mocks
        when(collaborationRepository.findByDiagramIdAndType(1L, Collaboration.CollaborationType.OWNER))
                .thenReturn(Optional.of(ownerCollaboration));

        // Act: Call service method
        boolean result = diagramManagementService.isOwner(1L, "not_owner");

        // Assert: Verify result
        assertFalse(result, "Should return false for non-owner");
        verify(collaborationRepository, times(1)).findByDiagramIdAndType(1L, Collaboration.CollaborationType.OWNER);

        log.info("[UT_DMS_015] response={}", result);
    }

    @Test
    public void UT_DMS_016_isOwnerNoOwner() {
        // Arrange: Setup mocks
        when(collaborationRepository.findByDiagramIdAndType(1L, Collaboration.CollaborationType.OWNER))
                .thenReturn(Optional.empty());

        // Act: Call service method
        boolean result = diagramManagementService.isOwner(1L, "owner_user");

        // Assert: Verify result
        assertFalse(result, "Should return false when no owner exists");
        verify(collaborationRepository, times(1)).findByDiagramIdAndType(1L, Collaboration.CollaborationType.OWNER);

        log.info("[UT_DMS_016] response={}", result);
    }

    // ======================== getTrashCount() - 2 tests ========================

    @Test
    public void UT_DMS_017_getTrashCountWithData() {
        // Arrange: Setup mocks
        when(diagramRepository.countByIsDeleted(true)).thenReturn(5L);

        // Act: Call service method
        Long result = diagramManagementService.getTrashCount("owner_user");

        // Assert: Verify result
        assertEquals(5L, result, "Should return count of deleted diagrams");
        verify(diagramRepository, times(1)).countByIsDeleted(true);

        log.info("[UT_DMS_017] response={}", result);
    }

    @Test
    public void UT_DMS_018_getTrashCountEmpty() {
        // Arrange: Setup mocks
        when(diagramRepository.countByIsDeleted(true)).thenReturn(0L);

        // Act: Call service method
        Long result = diagramManagementService.getTrashCount("owner_user");

        // Assert: Verify result
        assertEquals(0L, result, "Should return 0 when trash is empty");
        verify(diagramRepository, times(1)).countByIsDeleted(true);

        log.info("[UT_DMS_018] response={}", result);
    }

    // ======================== getDaysUntilAutoDelete() - 4 tests ========================

    @Test
    public void UT_DMS_019_getDaysUntilAutoDeleteWithDaysRemaining() {
        // Arrange: Setup mocks - deleted 3 days ago, so 4 days remaining (7 days total)
        Diagram diagramWithDaysRemaining = new Diagram();
        diagramWithDaysRemaining.setId(2L);
        diagramWithDaysRemaining.setName("Deleted Diagram");
        diagramWithDaysRemaining.setIsDeleted(true);
        diagramWithDaysRemaining.setDeletedAt(LocalDateTime.now().minusDays(3));

        when(diagramRepository.findById(2L)).thenReturn(Optional.of(diagramWithDaysRemaining));

        // Act: Call service method
        Long result = diagramManagementService.getDaysUntilAutoDelete(2L);

        // Assert: Verify result (should be around 4 days, allowing 1 day variance for test execution time)
        assertNotNull(result, "Should return days remaining");
        assertTrue(result >= 3 && result <= 5, "Should return approximately 4 days (3-5 range to account for execution time)");
        verify(diagramRepository, times(1)).findById(2L);

        log.info("[UT_DMS_019] response={}", result);
    }

    @Test
    public void UT_DMS_020_getDaysUntilAutoDeleteExpired() {
        // Arrange: Setup mocks - deleted 8 days ago, so auto-delete date has passed
        Diagram expiredDiagram = new Diagram();
        expiredDiagram.setId(2L);
        expiredDiagram.setName("Deleted Diagram");
        expiredDiagram.setIsDeleted(true);
        expiredDiagram.setDeletedAt(LocalDateTime.now().minusDays(8));

        when(diagramRepository.findById(2L)).thenReturn(Optional.of(expiredDiagram));

        // Act: Call service method
        Long result = diagramManagementService.getDaysUntilAutoDelete(2L);

        // Assert: Verify result
        assertEquals(0L, result, "Should return 0 when already expired");
        verify(diagramRepository, times(1)).findById(2L);

        log.info("[UT_DMS_020] response={}", result);
    }

    @Test
    public void UT_DMS_021_getDaysUntilAutoDeleteNotDeleted() {
        // Arrange: Setup mocks
        when(diagramRepository.findById(1L)).thenReturn(Optional.of(sampleDiagram));

        // Act: Call service method
        Long result = diagramManagementService.getDaysUntilAutoDelete(1L);

        // Assert: Verify result
        assertNull(result, "Should return null for non-deleted diagram");
        verify(diagramRepository, times(1)).findById(1L);

        log.info("[UT_DMS_021] response={}", result);
    }

    @Test
    public void UT_DMS_022_getDaysUntilAutoDeleteNotFound() {
        // Arrange: Setup mocks
        when(diagramRepository.findById(999L)).thenReturn(Optional.empty());

        // Act & Assert: Verify exception is thrown
        EntityNotFoundException exception = assertThrows(EntityNotFoundException.class, () -> {
            diagramManagementService.getDaysUntilAutoDelete(999L);
        });

        log.info("[UT_DMS_022] exception={}", exception.getMessage());
    }
}
