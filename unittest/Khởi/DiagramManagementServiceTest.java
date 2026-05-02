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
        log.info("========================================");
        log.info("[UT_DMS_001] BẮT ĐẦU: Soft delete diagram - thành công");
        log.info("[UT_DMS_001] Input: diagramId=1, username='owner_user'");

        // Arrange: Setup mocks
        when(diagramRepository.findById(1L)).thenReturn(Optional.of(sampleDiagram));
        when(collaborationRepository.findByDiagramIdAndType(1L, Collaboration.CollaborationType.OWNER))
                .thenReturn(Optional.of(ownerCollaboration));
        when(diagramRepository.save(any(Diagram.class))).thenAnswer(invocation -> {
            Diagram diagram = invocation.getArgument(0);
            assertTrue(diagram.getIsDeleted(), "Diagram should be marked as deleted");
            assertNotNull(diagram.getDeletedAt(), "DeletedAt should be set");
            log.info("[UT_DMS_001] Diagram được lưu: isDeleted=true, deletedAt={}", diagram.getDeletedAt());
            return diagram;
        });

        // Act: Call service method
        diagramManagementService.softDeleteDiagram(1L, "owner_user");

        // Assert: Verify result and mock interactions
        verify(diagramRepository, times(1)).findById(1L);
        verify(collaborationRepository, times(1)).findByDiagramIdAndType(1L, Collaboration.CollaborationType.OWNER);
        verify(diagramRepository, times(1)).save(any(Diagram.class));

        log.info("[UT_DMS_001] KẾT QUẢ: PASSED - Diagram được soft delete thành công");
        log.info("[TEARDOWN] Dọn dẹp dữ liệu test, reset mock objects...");
    }

    @Test
    public void UT_DMS_002_softDeleteDiagramNotFound() {
        log.info("========================================");
        log.info("[UT_DMS_002] BẮT ĐẦU: Soft delete diagram - diagram không tồn tại");
        log.info("[UT_DMS_002] Input: diagramId=999 (không tồn tại)");

        // Arrange: Setup mocks
        when(diagramRepository.findById(999L)).thenReturn(Optional.empty());

        // Act & Assert: Verify exception is thrown
        EntityNotFoundException exception = assertThrows(EntityNotFoundException.class, () -> {
            diagramManagementService.softDeleteDiagram(999L, "owner_user");
        });

        log.info("[UT_DMS_002] Exception caught: {}", exception.getMessage());
        log.info("[UT_DMS_002] KẾT QUẢ: PASSED - EntityNotFoundException được ném");
        log.info("[TEARDOWN] Dọn dẹp dữ liệu test, reset mock objects...");
    }

    @Test
    public void UT_DMS_003_softDeleteDiagramNotOwner() {
        log.info("========================================");
        log.info("[UT_DMS_003] BẮT ĐẦU: Soft delete diagram - user không phải owner");
        log.info("[UT_DMS_003] Input: diagramId=1, username='not_owner' (không phải owner)");

        // Arrange: Setup mocks
        when(diagramRepository.findById(1L)).thenReturn(Optional.of(sampleDiagram));
        when(collaborationRepository.findByDiagramIdAndType(1L, Collaboration.CollaborationType.OWNER))
                .thenReturn(Optional.of(ownerCollaboration));

        // Act & Assert: Verify exception is thrown
        IllegalStateException exception = assertThrows(IllegalStateException.class, () -> {
            diagramManagementService.softDeleteDiagram(1L, "not_owner");
        });

        log.info("[UT_DMS_003] Exception caught: {}", exception.getMessage());
        assertTrue(exception.getMessage().contains("Only owner can delete"), "Should mention owner requirement");
        log.info("[UT_DMS_003] KẾT QUẢ: PASSED - IllegalStateException được ném");
        log.info("[TEARDOWN] Dọn dẹp dữ liệu test, reset mock objects...");
    }

    @Test
    public void UT_DMS_004_softDeleteDiagramNoOwner() {
        log.info("========================================");
        log.info("[UT_DMS_004] BẮT ĐẦU: Soft delete diagram - diagram không có owner");
        log.info("[UT_DMS_004] Input: diagramId=1, không có owner");

        // Arrange: Setup mocks
        when(diagramRepository.findById(1L)).thenReturn(Optional.of(sampleDiagram));
        when(collaborationRepository.findByDiagramIdAndType(1L, Collaboration.CollaborationType.OWNER))
                .thenReturn(Optional.empty());

        // Act & Assert: Verify exception is thrown
        IllegalStateException exception = assertThrows(IllegalStateException.class, () -> {
            diagramManagementService.softDeleteDiagram(1L, "owner_user");
        });

        log.info("[UT_DMS_004] Exception caught: {}", exception.getMessage());
        log.info("[UT_DMS_004] KẾT QUẢ: PASSED - IllegalStateException được ném");
        log.info("[TEARDOWN] Dọn dẹp dữ liệu test, reset mock objects...");
    }

    // ======================== restoreDiagram() - 4 tests ========================

    @Test
    public void UT_DMS_005_restoreDiagramSuccess() {
        log.info("========================================");
        log.info("[UT_DMS_005] BẮT ĐẦU: Restore diagram từ trash - thành công");
        log.info("[UT_DMS_005] Input: diagramId=2, username='owner_user'");

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
            log.info("[UT_DMS_005] Diagram được lưu: isDeleted=false, deletedAt=null");
            return diagram;
        });

        // Act: Call service method
        diagramManagementService.restoreDiagram(2L, "owner_user");

        // Assert: Verify result and mock interactions
        verify(diagramRepository, times(1)).findById(2L);
        verify(collaborationRepository, times(1)).findByDiagramIdAndType(2L, Collaboration.CollaborationType.OWNER);
        verify(diagramRepository, times(1)).save(any(Diagram.class));

        log.info("[UT_DMS_005] KẾT QUẢ: PASSED - Diagram được restore thành công");
        log.info("[TEARDOWN] Dọn dẹp dữ liệu test, reset mock objects...");
    }

    @Test
    public void UT_DMS_006_restoreDiagramNotFound() {
        log.info("========================================");
        log.info("[UT_DMS_006] BẮT ĐẦU: Restore diagram - diagram không tồn tại");
        log.info("[UT_DMS_006] Input: diagramId=999 (không tồn tại)");

        // Arrange: Setup mocks
        when(diagramRepository.findById(999L)).thenReturn(Optional.empty());

        // Act & Assert: Verify exception is thrown
        EntityNotFoundException exception = assertThrows(EntityNotFoundException.class, () -> {
            diagramManagementService.restoreDiagram(999L, "owner_user");
        });

        log.info("[UT_DMS_006] Exception caught: {}", exception.getMessage());
        log.info("[UT_DMS_006] KẾT QUẢ: PASSED - EntityNotFoundException được ném");
        log.info("[TEARDOWN] Dọn dẹp dữ liệu test, reset mock objects...");
    }

    @Test
    public void UT_DMS_007_restoreDiagramNotOwner() {
        log.info("========================================");
        log.info("[UT_DMS_007] BẮT ĐẦU: Restore diagram - user không phải owner");
        log.info("[UT_DMS_007] Input: diagramId=2, username='not_owner'");

        // Arrange: Setup mocks
        when(diagramRepository.findById(2L)).thenReturn(Optional.of(sampleDiagramInTrash));
        when(collaborationRepository.findByDiagramIdAndType(2L, Collaboration.CollaborationType.OWNER))
                .thenReturn(Optional.of(ownerCollaboration));

        // Act & Assert: Verify exception is thrown
        IllegalStateException exception = assertThrows(IllegalStateException.class, () -> {
            diagramManagementService.restoreDiagram(2L, "not_owner");
        });

        log.info("[UT_DMS_007] Exception caught: {}", exception.getMessage());
        log.info("[UT_DMS_007] KẾT QUẢ: PASSED - IllegalStateException được ném");
        log.info("[TEARDOWN] Dọn dẹp dữ liệu test, reset mock objects...");
    }

    @Test
    public void UT_DMS_008_restoreDiagramNoOwner() {
        log.info("========================================");
        log.info("[UT_DMS_008] BẮT ĐẦU: Restore diagram - diagram không có owner");
        log.info("[UT_DMS_008] Input: diagramId=2, không có owner");

        // Arrange: Setup mocks
        when(diagramRepository.findById(2L)).thenReturn(Optional.of(sampleDiagramInTrash));
        when(collaborationRepository.findByDiagramIdAndType(2L, Collaboration.CollaborationType.OWNER))
                .thenReturn(Optional.empty());

        // Act & Assert: Verify exception is thrown
        IllegalStateException exception = assertThrows(IllegalStateException.class, () -> {
            diagramManagementService.restoreDiagram(2L, "owner_user");
        });

        log.info("[UT_DMS_008] Exception caught: {}", exception.getMessage());
        log.info("[UT_DMS_008] KẾT QUẢ: PASSED - IllegalStateException được ném");
        log.info("[TEARDOWN] Dọn dẹp dữ liệu test, reset mock objects...");
    }

    // ======================== permanentlyDeleteDiagram() - 5 tests ========================

    @Test
    public void UT_DMS_009_permanentlyDeleteDiagramSuccess() {
        log.info("========================================");
        log.info("[UT_DMS_009] BẮT ĐẦU: Permanently delete diagram - thành công");
        log.info("[UT_DMS_009] Input: diagramId=2, username='owner_user' (diagram in trash)");

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

        log.info("[UT_DMS_009] KẾT QUẢ: PASSED - Diagram được permanently delete thành công");
        log.info("[TEARDOWN] Dọn dẹp dữ liệu test, reset mock objects...");
    }

    @Test
    public void UT_DMS_010_permanentlyDeleteDiagramNotFound() {
        log.info("========================================");
        log.info("[UT_DMS_010] BẮT ĐẦU: Permanently delete diagram - diagram không tồn tại");
        log.info("[UT_DMS_010] Input: diagramId=999 (không tồn tại)");

        // Arrange: Setup mocks
        when(diagramRepository.findById(999L)).thenReturn(Optional.empty());

        // Act & Assert: Verify exception is thrown
        EntityNotFoundException exception = assertThrows(EntityNotFoundException.class, () -> {
            diagramManagementService.permanentlyDeleteDiagram(999L, "owner_user");
        });

        log.info("[UT_DMS_010] Exception caught: {}", exception.getMessage());
        log.info("[UT_DMS_010] KẾT QUẢ: PASSED - EntityNotFoundException được ném");
        log.info("[TEARDOWN] Dọn dẹp dữ liệu test, reset mock objects...");
    }

    @Test
    public void UT_DMS_011_permanentlyDeleteDiagramNotOwner() {
        log.info("========================================");
        log.info("[UT_DMS_011] BẮT ĐẦU: Permanently delete diagram - user không phải owner");
        log.info("[UT_DMS_011] Input: diagramId=2, username='not_owner'");

        // Arrange: Setup mocks
        when(diagramRepository.findById(2L)).thenReturn(Optional.of(sampleDiagramInTrash));
        when(collaborationRepository.findByDiagramIdAndType(2L, Collaboration.CollaborationType.OWNER))
                .thenReturn(Optional.of(ownerCollaboration));

        // Act & Assert: Verify exception is thrown
        IllegalStateException exception = assertThrows(IllegalStateException.class, () -> {
            diagramManagementService.permanentlyDeleteDiagram(2L, "not_owner");
        });

        log.info("[UT_DMS_011] Exception caught: {}", exception.getMessage());
        log.info("[UT_DMS_011] KẾT QUẢ: PASSED - IllegalStateException được ném");
        log.info("[TEARDOWN] Dọn dẹp dữ liệu test, reset mock objects...");
    }

    @Test
    public void UT_DMS_012_permanentlyDeleteDiagramNotInTrash() {
        log.info("========================================");
        log.info("[UT_DMS_012] BẮT ĐẦU: Permanently delete diagram - diagram không trong trash");
        log.info("[UT_DMS_012] Input: diagramId=1 (not deleted, isDeleted=false)");

        // Arrange: Setup mocks
        when(diagramRepository.findById(1L)).thenReturn(Optional.of(sampleDiagram));
        when(collaborationRepository.findByDiagramIdAndType(1L, Collaboration.CollaborationType.OWNER))
                .thenReturn(Optional.of(ownerCollaboration));

        // Act & Assert: Verify exception is thrown
        IllegalStateException exception = assertThrows(IllegalStateException.class, () -> {
            diagramManagementService.permanentlyDeleteDiagram(1L, "owner_user");
        });

        log.info("[UT_DMS_012] Exception caught: {}", exception.getMessage());
        assertTrue(exception.getMessage().contains("in trash"), "Should mention trash requirement");
        log.info("[UT_DMS_012] KẾT QUẢ: PASSED - IllegalStateException được ném");
        log.info("[TEARDOWN] Dọn dẹp dữ liệu test, reset mock objects...");
    }

    @Test
    public void UT_DMS_013_permanentlyDeleteDiagramNoOwner() {
        log.info("========================================");
        log.info("[UT_DMS_013] BẮT ĐẦU: Permanently delete diagram - diagram không có owner");
        log.info("[UT_DMS_013] Input: diagramId=2, không có owner");

        // Arrange: Setup mocks
        when(diagramRepository.findById(2L)).thenReturn(Optional.of(sampleDiagramInTrash));
        when(collaborationRepository.findByDiagramIdAndType(2L, Collaboration.CollaborationType.OWNER))
                .thenReturn(Optional.empty());

        // Act & Assert: Verify exception is thrown
        IllegalStateException exception = assertThrows(IllegalStateException.class, () -> {
            diagramManagementService.permanentlyDeleteDiagram(2L, "owner_user");
        });

        log.info("[UT_DMS_013] Exception caught: {}", exception.getMessage());
        log.info("[UT_DMS_013] KẾT QUẢ: PASSED - IllegalStateException được ném");
        log.info("[TEARDOWN] Dọn dẹp dữ liệu test, reset mock objects...");
    }

    // ======================== isOwner() - 2 tests ========================

    @Test
    public void UT_DMS_014_isOwnerTrue() {
        log.info("========================================");
        log.info("[UT_DMS_014] BẮT ĐẦU: Check is owner - user là owner");
        log.info("[UT_DMS_014] Input: diagramId=1, username='owner_user'");

        // Arrange: Setup mocks
        when(collaborationRepository.findByDiagramIdAndType(1L, Collaboration.CollaborationType.OWNER))
                .thenReturn(Optional.of(ownerCollaboration));

        // Act: Call service method
        boolean result = diagramManagementService.isOwner(1L, "owner_user");

        // Assert: Verify result
        assertTrue(result, "Should return true for owner");
        verify(collaborationRepository, times(1)).findByDiagramIdAndType(1L, Collaboration.CollaborationType.OWNER);

        log.info("[UT_DMS_014] KẾT QUẢ: PASSED - Trả về true");
        log.info("[TEARDOWN] Dọn dẹp dữ liệu test, reset mock objects...");
    }

    @Test
    public void UT_DMS_015_isOwnerFalse() {
        log.info("========================================");
        log.info("[UT_DMS_015] BẮT ĐẦU: Check is owner - user không phải owner");
        log.info("[UT_DMS_015] Input: diagramId=1, username='not_owner'");

        // Arrange: Setup mocks
        when(collaborationRepository.findByDiagramIdAndType(1L, Collaboration.CollaborationType.OWNER))
                .thenReturn(Optional.of(ownerCollaboration));

        // Act: Call service method
        boolean result = diagramManagementService.isOwner(1L, "not_owner");

        // Assert: Verify result
        assertFalse(result, "Should return false for non-owner");
        verify(collaborationRepository, times(1)).findByDiagramIdAndType(1L, Collaboration.CollaborationType.OWNER);

        log.info("[UT_DMS_015] KẾT QUẢ: PASSED - Trả về false");
        log.info("[TEARDOWN] Dọn dẹp dữ liệu test, reset mock objects...");
    }

    @Test
    public void UT_DMS_016_isOwnerNoOwner() {
        log.info("========================================");
        log.info("[UT_DMS_016] BẮT ĐẦU: Check is owner - diagram không có owner");
        log.info("[UT_DMS_016] Input: diagramId=1, không có owner");

        // Arrange: Setup mocks
        when(collaborationRepository.findByDiagramIdAndType(1L, Collaboration.CollaborationType.OWNER))
                .thenReturn(Optional.empty());

        // Act: Call service method
        boolean result = diagramManagementService.isOwner(1L, "owner_user");

        // Assert: Verify result
        assertFalse(result, "Should return false when no owner exists");
        verify(collaborationRepository, times(1)).findByDiagramIdAndType(1L, Collaboration.CollaborationType.OWNER);

        log.info("[UT_DMS_016] KẾT QUẢ: PASSED - Trả về false");
        log.info("[TEARDOWN] Dọn dẹp dữ liệu test, reset mock objects...");
    }

    // ======================== getTrashCount() - 2 tests ========================

    @Test
    public void UT_DMS_017_getTrashCountWithData() {
        log.info("========================================");
        log.info("[UT_DMS_017] BẮT ĐẦU: Get trash count - có dữ liệu");
        log.info("[UT_DMS_017] Input: username='owner_user'");

        // Arrange: Setup mocks
        when(diagramRepository.countByIsDeleted(true)).thenReturn(5L);

        // Act: Call service method
        Long result = diagramManagementService.getTrashCount("owner_user");

        // Assert: Verify result
        assertEquals(5L, result, "Should return count of deleted diagrams");
        verify(diagramRepository, times(1)).countByIsDeleted(true);

        log.info("[UT_DMS_017] KẾT QUẢ: PASSED - Trả về 5");
        log.info("[TEARDOWN] Dọn dẹp dữ liệu test, reset mock objects...");
    }

    @Test
    public void UT_DMS_018_getTrashCountEmpty() {
        log.info("========================================");
        log.info("[UT_DMS_018] BẮT ĐẦU: Get trash count - trash trống");
        log.info("[UT_DMS_018] Input: username='owner_user', trash empty");

        // Arrange: Setup mocks
        when(diagramRepository.countByIsDeleted(true)).thenReturn(0L);

        // Act: Call service method
        Long result = diagramManagementService.getTrashCount("owner_user");

        // Assert: Verify result
        assertEquals(0L, result, "Should return 0 when trash is empty");
        verify(diagramRepository, times(1)).countByIsDeleted(true);

        log.info("[UT_DMS_018] KẾT QUẢ: PASSED - Trả về 0");
        log.info("[TEARDOWN] Dọn dẹp dữ liệu test, reset mock objects...");
    }

    // ======================== getDaysUntilAutoDelete() - 4 tests ========================

    @Test
    public void UT_DMS_019_getDaysUntilAutoDeleteWithDaysRemaining() {
        log.info("========================================");
        log.info("[UT_DMS_019] BẮT ĐẦU: Get days until auto delete - còn ngày");
        log.info("[UT_DMS_019] Input: diagramId=2, deletedAt=3 days ago (4 days remaining)");

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

        log.info("[UT_DMS_019] KẾT QUẢ: PASSED - Trả về {} ngày", result);
        log.info("[TEARDOWN] Dọn dẹp dữ liệu test, reset mock objects...");
    }

    @Test
    public void UT_DMS_020_getDaysUntilAutoDeleteExpired() {
        log.info("========================================");
        log.info("[UT_DMS_020] BẮT ĐẦU: Get days until auto delete - đã hết hạn");
        log.info("[UT_DMS_020] Input: diagramId=2, deletedAt=8 days ago (expired)");

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

        log.info("[UT_DMS_020] KẾT QUẢ: PASSED - Trả về 0 (đã hết hạn)");
        log.info("[TEARDOWN] Dọn dẹp dữ liệu test, reset mock objects...");
    }

    @Test
    public void UT_DMS_021_getDaysUntilAutoDeleteNotDeleted() {
        log.info("========================================");
        log.info("[UT_DMS_021] BẮT ĐẦU: Get days until auto delete - diagram không bị xóa");
        log.info("[UT_DMS_021] Input: diagramId=1 (isDeleted=false)");

        // Arrange: Setup mocks
        when(diagramRepository.findById(1L)).thenReturn(Optional.of(sampleDiagram));

        // Act: Call service method
        Long result = diagramManagementService.getDaysUntilAutoDelete(1L);

        // Assert: Verify result
        assertNull(result, "Should return null for non-deleted diagram");
        verify(diagramRepository, times(1)).findById(1L);

        log.info("[UT_DMS_021] KẾT QUẢ: PASSED - Trả về null");
        log.info("[TEARDOWN] Dọn dẹp dữ liệu test, reset mock objects...");
    }

    @Test
    public void UT_DMS_022_getDaysUntilAutoDeleteNotFound() {
        log.info("========================================");
        log.info("[UT_DMS_022] BẮT ĐẦU: Get days until auto delete - diagram không tồn tại");
        log.info("[UT_DMS_022] Input: diagramId=999 (không tồn tại)");

        // Arrange: Setup mocks
        when(diagramRepository.findById(999L)).thenReturn(Optional.empty());

        // Act & Assert: Verify exception is thrown
        EntityNotFoundException exception = assertThrows(EntityNotFoundException.class, () -> {
            diagramManagementService.getDaysUntilAutoDelete(999L);
        });

        log.info("[UT_DMS_022] Exception caught: {}", exception.getMessage());
        log.info("[UT_DMS_022] KẾT QUẢ: PASSED - EntityNotFoundException được ném");
        log.info("[TEARDOWN] Dọn dẹp dữ liệu test, reset mock objects...");
    }
}
