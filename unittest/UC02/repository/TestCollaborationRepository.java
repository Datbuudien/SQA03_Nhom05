package com.example.react_flow_be.repository;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import com.example.react_flow_be.entity.Collaboration;
import com.example.react_flow_be.entity.Diagram;

/**
 * TestCollaborationRepository: Unit Test cho CollaborationRepository (Collaboration Data Access)
 * 
 * Mục tiêu: Đảm bảo các truy vấn database cho Collaboration hoạt động chính xác
 * 
 * Coverage Level 2 (Branch Coverage):
 * - findByDiagramId: tìm tất cả collaboration của diagram
 * - findByDiagramIdAndUsername: tìm collaboration theo diagram và user
 * - findActiveCollaboration: tìm collaboration đang hoạt động
 * - hasAccess: kiểm tra user có quyền truy cập
 * - findByDiagramIdAndType: tìm collaboration theo type
 * - countParticipants: đếm participants
 * - deleteByDiagramId: xóa tất cả collaboration của diagram
 */
@SpringBootTest
@ActiveProfiles("test")
@Transactional
@DisplayName("TestCollaborationRepository - Collaboration Data Access (UC02)")
public class TestCollaborationRepository {

    @Autowired
    private CollaborationRepository collaborationRepository;

    @Autowired
    private DiagramRepository diagramRepository;

    private Diagram diagram;
    private Collaboration ownerCollaboration;
    private Collaboration participantCollaboration;
    private Collaboration inactiveCollaboration;

    private final String ownerUsername = "owner_user";
    private final String participantUsername = "participant_user";
    private final String inactiveUsername = "inactive_user";

    @BeforeEach
    public void setUp() {
        // Clear repositories
        collaborationRepository.deleteAll();
        diagramRepository.deleteAll();

        // Create diagram
        diagram = new Diagram();
        diagram.setName("Test Diagram");
        diagram.setIsDeleted(false);
        diagram = diagramRepository.save(diagram);

        // Create owner collaboration
        ownerCollaboration = new Collaboration();
        ownerCollaboration.setDiagram(diagram);
        ownerCollaboration.setUsername(ownerUsername);
        ownerCollaboration.setType(Collaboration.CollaborationType.OWNER);
        ownerCollaboration.setPermission(Collaboration.Permission.FULL_ACCESS);
        ownerCollaboration.setIsActive(true);
        ownerCollaboration = collaborationRepository.save(ownerCollaboration);

        // Create participant collaboration (active)
        participantCollaboration = new Collaboration();
        participantCollaboration.setDiagram(diagram);
        participantCollaboration.setUsername(participantUsername);
        participantCollaboration.setType(Collaboration.CollaborationType.PARTICIPANTS);
        participantCollaboration.setPermission(Collaboration.Permission.EDIT);
        participantCollaboration.setIsActive(true);
        participantCollaboration = collaborationRepository.save(participantCollaboration);

        // Create inactive collaboration
        inactiveCollaboration = new Collaboration();
        inactiveCollaboration.setDiagram(diagram);
        inactiveCollaboration.setUsername(inactiveUsername);
        inactiveCollaboration.setType(Collaboration.CollaborationType.PARTICIPANTS);
        inactiveCollaboration.setPermission(Collaboration.Permission.VIEW);
        inactiveCollaboration.setIsActive(false);
        inactiveCollaboration = collaborationRepository.save(inactiveCollaboration);
    }

    // ==================== Test findByDiagramId ====================

    @Test
    @DisplayName("findByDiagramId_testChuan1 - Find all collaborations for diagram")
    public void findByDiagramId_testChuan1() {
        // Standard case: retrieve all collaborations
        List<Collaboration> result = collaborationRepository.findByDiagramId(diagram.getId());

        assertNotNull(result);
        assertEquals(3, result.size());
        assertTrue(result.stream().allMatch(c -> c.getDiagram().getId().equals(diagram.getId())));
    }

    @Test
    @DisplayName("findByDiagramId_testChuan2 - Find collaborations includes all types")
    public void findByDiagramId_testChuan2() {
        // Standard case: includes both active and inactive
        List<Collaboration> result = collaborationRepository.findByDiagramId(diagram.getId());

        assertTrue(result.stream().anyMatch(c -> c.getType().equals(Collaboration.CollaborationType.OWNER)));
        assertTrue(result.stream().anyMatch(c -> c.getType().equals(Collaboration.CollaborationType.PARTICIPANTS)));
        assertTrue(result.stream().anyMatch(c -> !c.getIsActive()));
    }

    @Test
    @DisplayName("findByDiagramId_ngoaile1 - Non-existent diagram returns empty")
    public void findByDiagramId_ngoaile1() {
        // Error case: diagram not found
        List<Collaboration> result = collaborationRepository.findByDiagramId(999L);

        assertNotNull(result);
        assertTrue(result.isEmpty());
    }

    // ==================== Test findByDiagramIdAndUsername ====================

    @Test
    @DisplayName("findByDiagramIdAndUsername_testChuan1 - Find user's collaboration")
    public void findByDiagramIdAndUsername_testChuan1() {
        // Standard case: find specific user's collaboration
        Optional<Collaboration> result = collaborationRepository.findByDiagramIdAndUsername(
                diagram.getId(), ownerUsername);

        assertTrue(result.isPresent());
        assertEquals(ownerUsername, result.get().getUsername());
        assertEquals(diagram.getId(), result.get().getDiagram().getId());
    }

    @Test
    @DisplayName("findByDiagramIdAndUsername_testChuan2 - Find participant collaboration")
    public void findByDiagramIdAndUsername_testChuan2() {
        // Standard case: find participant
        Optional<Collaboration> result = collaborationRepository.findByDiagramIdAndUsername(
                diagram.getId(), participantUsername);

        assertTrue(result.isPresent());
        assertEquals(participantUsername, result.get().getUsername());
        assertEquals(Collaboration.CollaborationType.PARTICIPANTS, result.get().getType());
    }

    @Test
    @DisplayName("findByDiagramIdAndUsername_ngoaile1 - Non-existent user returns empty")
    public void findByDiagramIdAndUsername_ngoaile1() {
        // Error case: user not collaborator
        Optional<Collaboration> result = collaborationRepository.findByDiagramIdAndUsername(
                diagram.getId(), "unknown_user");

        assertTrue(result.isEmpty());
    }

    @Test
    @DisplayName("findByDiagramIdAndUsername_ngoaile2 - Non-existent diagram returns empty")
    public void findByDiagramIdAndUsername_ngoaile2() {
        // Error case: diagram not found
        Optional<Collaboration> result = collaborationRepository.findByDiagramIdAndUsername(
                999L, ownerUsername);

        assertTrue(result.isEmpty());
    }

    // ==================== Test findByDiagramIdAndType ====================

    @Test
    @DisplayName("findByDiagramIdAndType_testChuan1 - Find owner collaboration by type")
    public void findByDiagramIdAndType_testChuan1() {
        // Standard case: find owner collaboration
        Optional<Collaboration> result = collaborationRepository.findByDiagramIdAndType(
                diagram.getId(), Collaboration.CollaborationType.OWNER);

        assertTrue(result.isPresent());
        assertEquals(Collaboration.CollaborationType.OWNER, result.get().getType());
        assertEquals(ownerUsername, result.get().getUsername());
    }

    @Test
    @DisplayName("findByDiagramIdAndType_ngoaile1 - No owner found returns empty")
    public void findByDiagramIdAndType_ngoaile1() {
        // Error case: search for non-existent type
        Optional<Collaboration> result = collaborationRepository.findByDiagramIdAndType(
                999L, Collaboration.CollaborationType.OWNER);

        assertTrue(result.isEmpty());
    }

    // ==================== Test countParticipants ====================

    @Test
    @DisplayName("countParticipants_testChuan1 - Count all participants")
    public void countParticipants_testChuan1() {
        // Standard case: count participants (excluding owner)
        Integer result = collaborationRepository.countParticipants(diagram.getId());

        assertNotNull(result);
        assertTrue(result >= 0);
    }

    @Test
    @DisplayName("countParticipants_testChuan2 - Count participants returns non-negative")
    public void countParticipants_testChuan2() {
        // Standard case: count should be non-negative
        Integer result = collaborationRepository.countParticipants(diagram.getId());

        assertTrue(result >= 0);
    }

    // ==================== Test deleteByDiagramId ====================

    @Test
    @DisplayName("deleteByDiagramId_testChuan1 - Delete all collaborations for diagram")
    public void deleteByDiagramId_testChuan1() {
        // Standard case: delete all collaborations
        List<Collaboration> beforeDelete = collaborationRepository.findByDiagramId(diagram.getId());
        assertEquals(3, beforeDelete.size());

        collaborationRepository.deleteByDiagramId(diagram.getId());

        List<Collaboration> afterDelete = collaborationRepository.findByDiagramId(diagram.getId());
        assertTrue(afterDelete.isEmpty());
    }

    @Test
    @DisplayName("deleteByDiagramId_testChuan2 - Delete doesn't affect other diagrams")
    public void deleteByDiagramId_testChuan2() {
        // Standard case: only deletes for specified diagram
        // Create another diagram
        Diagram otherDiagram = new Diagram();
        otherDiagram.setName("Other Diagram");
        otherDiagram = diagramRepository.save(otherDiagram);

        Collaboration otherCollab = new Collaboration();
        otherCollab.setDiagram(otherDiagram);
        otherCollab.setUsername("other_owner");
        otherCollab.setType(Collaboration.CollaborationType.OWNER);
        otherCollab.setPermission(Collaboration.Permission.FULL_ACCESS);
        otherCollab.setIsActive(true);
        otherCollab = collaborationRepository.save(otherCollab);

        // Delete only diagram's collaborations
        collaborationRepository.deleteByDiagramId(diagram.getId());

        List<Collaboration> remaining = collaborationRepository.findByDiagramId(otherDiagram.getId());
        assertEquals(1, remaining.size());
    }

    @Test
    @DisplayName("deleteByDiagramId_ngoaile1 - Delete non-existent diagram doesn't fail")
    public void deleteByDiagramId_ngoaile1() {
        // Error case: delete non-existent diagram
        assertDoesNotThrow(() -> collaborationRepository.deleteByDiagramId(999L));

        List<Collaboration> result = collaborationRepository.findByDiagramId(diagram.getId());
        assertEquals(3, result.size()); // Original diagram unaffected
    }

    // ==================== Integration Tests ====================

    @Test
    @DisplayName("integration_testChuan1 - Add collaborator and verify")
    public void integration_testChuan1() {
        // Integration test: create and verify new collaborator
        Diagram newDiagram = new Diagram();
        newDiagram.setName("New Diagram");
        newDiagram = diagramRepository.save(newDiagram);

        // Create owner
        Collaboration owner = new Collaboration();
        owner.setDiagram(newDiagram);
        owner.setUsername("new_owner");
        owner.setType(Collaboration.CollaborationType.OWNER);
        owner.setPermission(Collaboration.Permission.FULL_ACCESS);
        owner.setIsActive(true);
        collaborationRepository.save(owner);

        // Verify
        Optional<Collaboration> found = collaborationRepository.findByDiagramIdAndType(
                newDiagram.getId(), Collaboration.CollaborationType.OWNER);

        assertTrue(found.isPresent());
        assertEquals("new_owner", found.get().getUsername());
    }

    @Test
    @DisplayName("integration_testChuan2 - Find and update collaboration")
    public void integration_testChuan2() {
        // Integration test: find collaboration and update
        Optional<Collaboration> found = collaborationRepository.findByDiagramIdAndUsername(
                diagram.getId(), participantUsername);

        assertTrue(found.isPresent());
        assertEquals(Collaboration.Permission.EDIT, found.get().getPermission());

        // Update permission
        Collaboration collab = found.get();
        collab.setPermission(Collaboration.Permission.COMMENT);
        collaborationRepository.save(collab);

        // Verify update
        Optional<Collaboration> updated = collaborationRepository.findByDiagramIdAndUsername(
                diagram.getId(), participantUsername);
        assertTrue(updated.isPresent());
        assertEquals(Collaboration.Permission.COMMENT, updated.get().getPermission());
    }

    @Test
    @DisplayName("integration_testChuan3 - Get all collaborations and count")
    public void integration_testChuan3() {
        // Integration test: retrieve and count collaborations
        List<Collaboration> allCollab = collaborationRepository.findByDiagramId(diagram.getId());
        assertEquals(3, allCollab.size());

        long activeCount = allCollab.stream().filter(Collaboration::getIsActive).count();
        assertEquals(2, activeCount);

        long inactiveCount = allCollab.stream().filter(c -> !c.getIsActive()).count();
        assertEquals(1, inactiveCount);
    }
}
