package com.example.react_flow_be.repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.test.context.ActiveProfiles;

import com.example.react_flow_be.entity.Collaboration;
import com.example.react_flow_be.entity.Diagram;

/**
 * TestDiagramRepository: Unit Test cho DiagramRepository (Diagram Data Access)
 * 
 * Mục tiêu: Đảm bảo các truy vấn database cho Diagram hoạt động chính xác
 * 
 * Coverage Level 2 (Branch Coverage):
 * - findByName: tìm diagram theo name thành công, không tồn tại
 * - findByIsDeletedFalse: lấy danh sách diagram chưa xóa
 * - findByIsDeletedTrue: lấy danh sách diagram đã xóa
 * - countByIsDeleted: đếm diagram theo trạng thái xóa
 * - findByIsDeletedTrueAndDeletedAtBefore: tìm diagram quá thời hạn
 * - countByIsDeletedTrueAndDeletedAtBefore: đếm diagram quá thời hạn
 * - findDeletedDiagramsByOwner: tìm diagram xóa của owner
 */
@DataJpaTest
@ActiveProfiles("test")
@DisplayName("TestDiagramRepository - Diagram Data Access (UC02)")
public class TestDiagramRepository {

    @Autowired
    private DiagramRepository diagramRepository;

    @Autowired
    private CollaborationRepository collaborationRepository;

    private Diagram activeDiagram;
    private Diagram deletedDiagram;
    private Diagram deletedExpiredDiagram;
    private Collaboration ownerCollaboration;

    private final String diagramName = "Test Diagram";
    private final String ownerUsername = "owner_user";
    private final int AUTO_DELETE_DAYS = 30;

    @BeforeEach
    public void setUp() {
        // Clear repositories
        collaborationRepository.deleteAll();
        diagramRepository.deleteAll();

        // Create active diagram
        activeDiagram = new Diagram();
        activeDiagram.setName("Active Diagram");
        activeDiagram.setDescription("Active test diagram");
        activeDiagram.setIsDeleted(false);
        activeDiagram.setDeletedAt(null);
        activeDiagram.setIsPublic(false);
        activeDiagram.setIsTemplate(false);
        activeDiagram = diagramRepository.save(activeDiagram);

        // Create soft-deleted diagram (recent)
        deletedDiagram = new Diagram();
        deletedDiagram.setName("Deleted Diagram Recent");
        deletedDiagram.setDescription("Recently deleted diagram");
        deletedDiagram.setIsDeleted(true);
        deletedDiagram.setDeletedAt(LocalDateTime.now().minusDays(5));
        deletedDiagram.setIsPublic(false);
        deletedDiagram.setIsTemplate(false);
        deletedDiagram = diagramRepository.save(deletedDiagram);

        // Create soft-deleted diagram (expired)
        deletedExpiredDiagram = new Diagram();
        deletedExpiredDiagram.setName("Deleted Diagram Expired");
        deletedExpiredDiagram.setDescription("Old deleted diagram");
        deletedExpiredDiagram.setIsDeleted(true);
        deletedExpiredDiagram.setDeletedAt(LocalDateTime.now().minusDays(40));
        deletedExpiredDiagram.setIsPublic(false);
        deletedExpiredDiagram.setIsTemplate(false);
        deletedExpiredDiagram = diagramRepository.save(deletedExpiredDiagram);

        // Create owner collaboration for activeDiagram
        ownerCollaboration = new Collaboration();
        ownerCollaboration.setDiagram(activeDiagram);
        ownerCollaboration.setUsername(ownerUsername);
        ownerCollaboration.setType(Collaboration.CollaborationType.OWNER);
        ownerCollaboration.setPermission(Collaboration.Permission.FULL_ACCESS);
        ownerCollaboration.setIsActive(true);
        collaborationRepository.save(ownerCollaboration);
    }

    // ==================== Test findByName ====================

    @Test
    @DisplayName("findByName_testChuan1 - Find diagram by name successfully")
    public void findByName_testChuan1() {
        // Standard case: find existing diagram by name
        Optional<Diagram> result = diagramRepository.findByName("Active Diagram");

        assertTrue(result.isPresent());
        assertEquals(activeDiagram.getId(), result.get().getId());
        assertEquals("Active Diagram", result.get().getName());
    }

    @Test
    @DisplayName("findByName_ngoaile1 - Find non-existent diagram returns empty")
    public void findByName_ngoaile1() {
        // Error case: diagram name not found
        Optional<Diagram> result = diagramRepository.findByName("Non-existent Diagram");

        assertTrue(result.isEmpty());
    }

    @Test
    @DisplayName("findByName_ngoaile2 - Find with null name returns empty")
    public void findByName_ngoaile2() {
        // Error case: null name
        Optional<Diagram> result = diagramRepository.findByName(null);

        assertTrue(result.isEmpty());
    }

    // ==================== Test findByIsDeletedFalse ====================

    @Test
    @DisplayName("findByIsDeletedFalse_testChuan1 - Find all active diagrams")
    public void findByIsDeletedFalse_testChuan1() {
        // Standard case: retrieve all non-deleted diagrams
        List<Diagram> result = diagramRepository.findByIsDeletedFalse();

        assertNotNull(result);
        assertEquals(1, result.size());
        assertTrue(result.stream().anyMatch(d -> d.getId().equals(activeDiagram.getId())));
        assertTrue(result.stream().noneMatch(d -> d.getIsDeleted()));
    }

    @Test
    @DisplayName("findByIsDeletedFalse_testChuan2 - All active diagrams not deleted")
    public void findByIsDeletedFalse_testChuan2() {
        // Standard case: verify no deleted diagrams in result
        List<Diagram> result = diagramRepository.findByIsDeletedFalse();

        for (Diagram diagram : result) {
            assertFalse(diagram.getIsDeleted());
            assertNull(diagram.getDeletedAt());
        }
    }

    // ==================== Test findByIsDeletedTrue ====================

    @Test
    @DisplayName("findByIsDeletedTrue_testChuan1 - Find all deleted diagrams")
    public void findByIsDeletedTrue_testChuan1() {
        // Standard case: retrieve all deleted diagrams
        List<Diagram> result = diagramRepository.findByIsDeletedTrue();

        assertNotNull(result);
        assertEquals(2, result.size());
        assertTrue(result.stream().allMatch(d -> d.getIsDeleted()));
    }

    @Test
    @DisplayName("findByIsDeletedTrue_testChuan2 - All deleted diagrams have deletedAt set")
    public void findByIsDeletedTrue_testChuan2() {
        // Standard case: verify deletedAt is set for all deleted diagrams
        List<Diagram> result = diagramRepository.findByIsDeletedTrue();

        for (Diagram diagram : result) {
            assertTrue(diagram.getIsDeleted());
            assertNotNull(diagram.getDeletedAt());
        }
    }

    // ==================== Test countByIsDeleted ====================

    @Test
    @DisplayName("countByIsDeleted_testChuan1 - Count active diagrams")
    public void countByIsDeleted_testChuan1() {
        // Standard case: count non-deleted diagrams
        Long result = diagramRepository.countByIsDeleted(false);

        assertEquals(1L, result);
    }

    @Test
    @DisplayName("countByIsDeleted_testChuan2 - Count deleted diagrams")
    public void countByIsDeleted_testChuan2() {
        // Standard case: count deleted diagrams
        Long result = diagramRepository.countByIsDeleted(true);

        assertEquals(2L, result);
    }

    // ==================== Test findByIsDeletedTrueAndDeletedAtBefore ====================

    @Test
    @DisplayName("findByIsDeletedTrueAndDeletedAtBefore_testChuan1 - Find expired deleted diagrams")
    public void findByIsDeletedTrueAndDeletedAtBefore_testChuan1() {
        // Standard case: find diagrams deleted more than 30 days ago
        LocalDateTime cutoffDate = LocalDateTime.now().minusDays(AUTO_DELETE_DAYS);
        List<Diagram> result = diagramRepository.findByIsDeletedTrueAndDeletedAtBefore(cutoffDate);

        assertNotNull(result);
        assertEquals(1, result.size());
        assertTrue(result.stream().anyMatch(d -> d.getId().equals(deletedExpiredDiagram.getId())));
    }

    @Test
    @DisplayName("findByIsDeletedTrueAndDeletedAtBefore_testChuan2 - No expired diagrams returns empty")
    public void findByIsDeletedTrueAndDeletedAtBefore_testChuan2() {
        // Standard case: when cutoff is recent
        LocalDateTime cutoffDate = LocalDateTime.now().minusDays(1);
        List<Diagram> result = diagramRepository.findByIsDeletedTrueAndDeletedAtBefore(cutoffDate);

        assertEquals(0, result.size());
    }

    // ==================== Test countByIsDeletedTrueAndDeletedAtBefore ====================

    @Test
    @DisplayName("countByIsDeletedTrueAndDeletedAtBefore_testChuan1 - Count expired deleted diagrams")
    public void countByIsDeletedTrueAndDeletedAtBefore_testChuan1() {
        // Standard case: count diagrams ready for permanent deletion
        LocalDateTime cutoffDate = LocalDateTime.now().minusDays(AUTO_DELETE_DAYS);
        Long result = diagramRepository.countByIsDeletedTrueAndDeletedAtBefore(cutoffDate);

        assertEquals(1L, result);
    }

    @Test
    @DisplayName("countByIsDeletedTrueAndDeletedAtBefore_testChuan2 - Count when no expired diagrams")
    public void countByIsDeletedTrueAndDeletedAtBefore_testChuan2() {
        // Standard case: recent cutoff returns zero
        LocalDateTime cutoffDate = LocalDateTime.now().minusDays(1);
        Long result = diagramRepository.countByIsDeletedTrueAndDeletedAtBefore(cutoffDate);

        assertEquals(0L, result);
    }

    // ==================== Test findDeletedDiagramsByOwner ====================

    @Test
    @DisplayName("findDeletedDiagramsByOwner_testChuan1 - Find owner's deleted diagrams")
    public void findDeletedDiagramsByOwner_testChuan1() {
        // Standard case: find deleted diagrams for specific owner
        // Create deleted diagram with owner
        Diagram ownerDeletedDiagram = new Diagram();
        ownerDeletedDiagram.setName("Owner Deleted Diagram");
        ownerDeletedDiagram.setIsDeleted(true);
        ownerDeletedDiagram.setDeletedAt(LocalDateTime.now().minusDays(10));
        ownerDeletedDiagram = diagramRepository.save(ownerDeletedDiagram);

        Collaboration ownerCollab = new Collaboration();
        ownerCollab.setDiagram(ownerDeletedDiagram);
        ownerCollab.setUsername(ownerUsername);
        ownerCollab.setType(Collaboration.CollaborationType.OWNER);
        ownerCollab.setPermission(Collaboration.Permission.FULL_ACCESS);
        ownerCollab.setIsActive(true);
        collaborationRepository.save(ownerCollab);

        // Query
        List<Diagram> result = diagramRepository.findDeletedDiagramsByOwner(ownerUsername);

        assertNotNull(result);
        assertEquals(1, result.size());
        assertTrue(result.stream().allMatch(d -> d.getIsDeleted()));
    }

    @Test
    @DisplayName("findDeletedDiagramsByOwner_testChuan2 - No deleted diagrams returns empty")
    public void findDeletedDiagramsByOwner_testChuan2() {
        // Standard case: owner with no deleted diagrams
        List<Diagram> result = diagramRepository.findDeletedDiagramsByOwner(ownerUsername);

        assertNotNull(result);
        assertTrue(result.isEmpty());
    }

    @Test
    @DisplayName("findDeletedDiagramsByOwner_ngoaile1 - Non-existent owner returns empty")
    public void findDeletedDiagramsByOwner_ngoaile1() {
        // Error case: unknown username
        List<Diagram> result = diagramRepository.findDeletedDiagramsByOwner("unknown_user");

        assertNotNull(result);
        assertTrue(result.isEmpty());
    }

    // ==================== Integration Tests ====================

    @Test
    @DisplayName("integration_testChuan1 - Create, delete, and query soft-deleted diagram")
    public void integration_testChuan1() {
        // Integration test: create diagram, soft delete it, then query
        Diagram newDiagram = new Diagram();
        newDiagram.setName("Integration Test Diagram");
        newDiagram.setIsDeleted(false);
        newDiagram = diagramRepository.save(newDiagram);
        final Long newDiagramId = newDiagram.getId();

        // Verify active diagram count
        List<Diagram> activeBefore = diagramRepository.findByIsDeletedFalse();
        int activeBefCount = activeBefore.size();

        // Soft delete
        newDiagram.setIsDeleted(true);
        newDiagram.setDeletedAt(LocalDateTime.now());
        diagramRepository.save(newDiagram);

        // Verify moved from active to deleted
        List<Diagram> activeAfter = diagramRepository.findByIsDeletedFalse();
        List<Diagram> deletedAfter = diagramRepository.findByIsDeletedTrue();

        assertEquals(activeBefCount - 1, activeAfter.size());
        assertTrue(deletedAfter.stream().anyMatch(d -> d.getId().equals(newDiagramId)));
    }

    @Test
    @DisplayName("integration_testChuan2 - Count active and deleted diagrams")
    public void integration_testChuan2() {
        // Integration test: verify counts match
        Long activeCount = diagramRepository.countByIsDeleted(false);
        Long deletedCount = diagramRepository.countByIsDeleted(true);

        assertEquals(1L, activeCount);
        assertEquals(2L, deletedCount);

        List<Diagram> allActive = diagramRepository.findByIsDeletedFalse();
        List<Diagram> allDeleted = diagramRepository.findByIsDeletedTrue();

        assertEquals(activeCount, (long) allActive.size());
        assertEquals(deletedCount, (long) allDeleted.size());
    }

    @Test
    @DisplayName("integration_testChuan3 - Find by name and verify not deleted")
    public void integration_testChuan3() {
        // Integration test: find active diagram by name
        Optional<Diagram> result = diagramRepository.findByName("Active Diagram");

        assertTrue(result.isPresent());
        assertFalse(result.get().getIsDeleted());
        assertNull(result.get().getDeletedAt());

        List<Diagram> activeList = diagramRepository.findByIsDeletedFalse();
        assertTrue(activeList.stream().anyMatch(d -> d.getId().equals(result.get().getId())));
    }
}
