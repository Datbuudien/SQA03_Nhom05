/**
 * ============================================================================
 * UNIT TEST CLASS: CollaborationServiceTest
 * ============================================================================
 * Purpose: Comprehensive unit tests for CollaborationService
 * - Tests all 11 public methods covering branch level (cấp 2) code coverage
 * - Uses Mockito for dependency injection and mock verification (CheckDB concept)
 * - Ensures clean state between tests (Rollback concept)
 * - Includes detailed Test Case IDs and meaningful variable names
 * 
 * Test Scope:
 * - Service: CollaborationService
 * - Coverage Target: Line ≥ 85%, Branch ≥ 80%
 * - Total Test Cases: ~25 test methods
 * 
 * Author: QA Team
 * Date: 19/4/2026
 * Version: 1.0
 * ============================================================================
 */

package com.example.react_flow_be.service;

import com.example.react_flow_be.config.DiagramSessionManager;
import com.example.react_flow_be.config.WebSocketSessionTracker;
import com.example.react_flow_be.dto.collaboration.CollaborationDTO;
import com.example.react_flow_be.entity.Collaboration;
import com.example.react_flow_be.entity.Diagram;
import com.example.react_flow_be.repository.CollaborationRepository;
import com.example.react_flow_be.repository.DiagramRepository;
import jakarta.persistence.EntityNotFoundException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

/**
 * Test class for CollaborationService using Mockito framework
 * 
 * ROLLBACK STRATEGY:
 * - @BeforeEach: Reset all mocks to clean state before each test
 * - No real DB access (all repositories are mocked)
 * - Verify mock interactions instead of DB state changes
 * 
 * CHECKDB STRATEGY:
 * - verify() calls ensure repositories are invoked correctly
 * - ArgumentCaptor captures method arguments for verification
 * - Mock return values simulate DB responses
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("CollaborationService - Unit Tests (Cấp 2 Coverage)")
class CollaborationServiceTest {

        // ============================================================================
        // MOCK DEPENDENCIES INJECTION
        // ============================================================================

        @Mock
        private CollaborationRepository mockCollaborationRepository;

        @Mock
        private DiagramRepository mockDiagramRepository;

        @Mock
        private DiagramSessionManager mockSessionManager;

        @Mock
        private WebSocketSessionTracker mockSessionRegistry;

        @InjectMocks
        private CollaborationService serviceUnderTest;

        // ============================================================================
        // TEST FIXTURES & CONSTANTS
        // ============================================================================

        private static final Long DIAGRAM_ID_VALID = 1L;
        private static final Long DIAGRAM_ID_INVALID = 999L;
        private static final Long COLLABORATION_ID_VALID = 1L;
        private static final Long COLLABORATION_ID_INVALID = 999L;

        private static final String USERNAME_OWNER = "owner_user";
        private static final String USERNAME_COLLABORATOR_EXISTING = "collaborator_existing";
        private static final String USERNAME_COLLABORATOR_NEW = "collaborator_new";

        private Diagram testDiagramEntity;
        private Collaboration testOwnerCollaboration;
        private Collaboration testParticipantCollaboration;

        /**
         * Test Case: SETUP-001
         * Setup test fixtures before each test
         * Purpose: Rollback - Initialize clean state with fresh mock objects
         */
        @BeforeEach
        public void testSetup_InitializeCleanState() {
                // Initialize test diagram entity
                testDiagramEntity = new Diagram();
                testDiagramEntity.setId(DIAGRAM_ID_VALID);
                testDiagramEntity.setName("Test Diagram");

                // Initialize test owner collaboration
                testOwnerCollaboration = new Collaboration();
                testOwnerCollaboration.setId(COLLABORATION_ID_VALID);
                testOwnerCollaboration.setDiagram(testDiagramEntity);
                testOwnerCollaboration.setUsername(USERNAME_OWNER);
                testOwnerCollaboration.setType(Collaboration.CollaborationType.OWNER);
                testOwnerCollaboration.setPermission(Collaboration.Permission.FULL_ACCESS);
                testOwnerCollaboration.setIsActive(true);

                // Initialize test participant collaboration
                testParticipantCollaboration = new Collaboration();
                testParticipantCollaboration.setId(COLLABORATION_ID_VALID + 1);
                testParticipantCollaboration.setDiagram(testDiagramEntity);
                testParticipantCollaboration.setUsername(USERNAME_COLLABORATOR_EXISTING);
                testParticipantCollaboration.setType(Collaboration.CollaborationType.PARTICIPANTS);
                testParticipantCollaboration.setPermission(Collaboration.Permission.VIEW);
                testParticipantCollaboration.setIsActive(true);

                // Reset all mocks to clean state (Rollback)
                reset(mockCollaborationRepository, mockDiagramRepository, mockSessionManager, mockSessionRegistry);
        }

        // ============================================================================
        // TEST GROUP 1: getCollaborations() - Retrieve all collaborators
        // ============================================================================

        @Nested
        @DisplayName("Test Group: getCollaborations()")
        class GetCollaborationsTests {

                /**
                 * Test Case: CT001
                 * Scenario: Get collaborations when diagram exists and has multiple
                 * collaborators
                 * Expected: Return list with both owner and participants
                 * 
                 * CheckDB: Verify collaborationRepository.findByDiagramId() is called exactly
                 * once
                 * Rollback: Mock state is reset before this test
                 */
                @Test
                @DisplayName("CT001: Happy Path - Get collaborations for existing diagram")
                public void testGetCollaborations_WhenDiagramExists_ShouldReturnCollaborationList() {
                        // ARRANGE: Setup mocks
                        List<Collaboration> expectedCollaborations = Arrays.asList(
                                        testOwnerCollaboration,
                                        testParticipantCollaboration);
                        when(mockDiagramRepository.existsById(DIAGRAM_ID_VALID)).thenReturn(true);
                        when(mockCollaborationRepository.findByDiagramId(DIAGRAM_ID_VALID))
                                        .thenReturn(expectedCollaborations);

                        // ACT: Call service method
                        List<CollaborationDTO> actualCollaborations = serviceUnderTest
                                        .getCollaborations(DIAGRAM_ID_VALID);

                        // ASSERT: Verify results
                        assertNotNull(actualCollaborations, "Collaboration list should not be null");
                        assertEquals(2, actualCollaborations.size(), "Should have 2 collaborations");

                        // CHECKDB: Verify repository method was called with correct parameters
                        verify(mockDiagramRepository, times(1)).existsById(DIAGRAM_ID_VALID);
                        verify(mockCollaborationRepository, times(1)).findByDiagramId(DIAGRAM_ID_VALID);
                        verifyNoMoreInteractions(mockDiagramRepository, mockCollaborationRepository);
                }

                /**
                 * Test Case: CT002
                 * Scenario: Get collaborations when diagram does not exist
                 * Expected: Throw EntityNotFoundException
                 * 
                 * CheckDB: Verify existsById() is called and returns false
                 * Rollback: No mock state change after exception
                 */
                @Test
                @DisplayName("CT002: Exception Path - Diagram not found")
                public void testGetCollaborations_WhenDiagramNotFound_ShouldThrowEntityNotFoundException() {
                        // ARRANGE: Setup mocks - diagram does not exist
                        when(mockDiagramRepository.existsById(DIAGRAM_ID_INVALID)).thenReturn(false);

                        // ACT & ASSERT: Call service and verify exception
                        EntityNotFoundException thrownException = assertThrows(
                                        EntityNotFoundException.class,
                                        () -> serviceUnderTest.getCollaborations(DIAGRAM_ID_INVALID),
                                        "Should throw EntityNotFoundException when diagram not found");

                        assertTrue(thrownException.getMessage().contains("Diagram not found"),
                                        "Exception message should mention diagram not found");

                        // CHECKDB: Verify repository was called with correct parameter
                        verify(mockDiagramRepository, times(1)).existsById(DIAGRAM_ID_INVALID);
                        // Should NOT call findByDiagramId since diagram check failed
                        verify(mockCollaborationRepository, never()).findByDiagramId(anyLong());
                }

                /**
                 * Test Case: CT003
                 * Scenario: Get collaborations when diagram exists but has no collaborators
                 * Expected: Return empty list
                 * 
                 * CheckDB: Verify repository returns empty list
                 */
                @Test
                @DisplayName("CT003: Edge Case - No collaborators in diagram")
                public void testGetCollaborations_WhenNoDiagramCollaborators_ShouldReturnEmptyList() {
                        // ARRANGE: Setup mocks
                        when(mockDiagramRepository.existsById(DIAGRAM_ID_VALID)).thenReturn(true);
                        when(mockCollaborationRepository.findByDiagramId(DIAGRAM_ID_VALID))
                                        .thenReturn(Arrays.asList());

                        // ACT: Call service method
                        List<CollaborationDTO> actualCollaborations = serviceUnderTest
                                        .getCollaborations(DIAGRAM_ID_VALID);

                        // ASSERT: Verify empty list
                        assertNotNull(actualCollaborations, "List should not be null");
                        assertTrue(actualCollaborations.isEmpty(), "List should be empty");

                        // CHECKDB: Verify repository was called
                        verify(mockCollaborationRepository, times(1)).findByDiagramId(DIAGRAM_ID_VALID);
                }
        }

        // ============================================================================
        // TEST GROUP 2: addCollaborator() - Add new collaborator
        // ============================================================================

        @Nested
        @DisplayName("Test Group: addCollaborator()")
        class AddCollaboratorTests {

                /**
                 * Test Case: CT004
                 * Scenario: Add new collaborator to existing diagram when user is not already
                 * collaborator
                 * Expected: Collaboration created successfully with correct permission
                 * 
                 * CheckDB: Verify save() is called with correct collaboration entity
                 * Rollback: Mock state reset before test
                 */
                @Test
                @DisplayName("CT004: Happy Path - Add new collaborator with VIEW permission")
                public void testAddCollaborator_WhenNewUserAndDiagramExists_ShouldCreateCollaboration() {
                        // ARRANGE: Setup mocks
                        Collaboration savedCollaboration = new Collaboration();
                        savedCollaboration.setId(10L);
                        savedCollaboration.setDiagram(testDiagramEntity);
                        savedCollaboration.setUsername(USERNAME_COLLABORATOR_NEW);
                        savedCollaboration.setType(Collaboration.CollaborationType.PARTICIPANTS);
                        savedCollaboration.setPermission(Collaboration.Permission.VIEW);
                        savedCollaboration.setIsActive(true);

                        when(mockDiagramRepository.findById(DIAGRAM_ID_VALID))
                                        .thenReturn(Optional.of(testDiagramEntity));
                        when(mockCollaborationRepository.findByDiagramIdAndUsername(DIAGRAM_ID_VALID,
                                        USERNAME_COLLABORATOR_NEW))
                                        .thenReturn(Optional.empty());
                        when(mockCollaborationRepository.save(any(Collaboration.class)))
                                        .thenReturn(savedCollaboration);

                        // ACT: Call service method
                        CollaborationDTO resultCollaboration = serviceUnderTest.addCollaborator(
                                        DIAGRAM_ID_VALID,
                                        USERNAME_COLLABORATOR_NEW,
                                        Collaboration.Permission.VIEW);

                        // ASSERT: Verify result
                        assertNotNull(resultCollaboration, "Result collaboration should not be null");
                        assertEquals(USERNAME_COLLABORATOR_NEW, resultCollaboration.getUsername(),
                                        "Username should match");

                        // CHECKDB: Verify repository save was called with correct entity
                        ArgumentCaptor<Collaboration> savedCollaborationCaptor = ArgumentCaptor
                                        .forClass(Collaboration.class);
                        verify(mockCollaborationRepository, times(1)).save(savedCollaborationCaptor.capture());

                        Collaboration capturedCollaboration = savedCollaborationCaptor.getValue();
                        assertEquals(Collaboration.CollaborationType.PARTICIPANTS, capturedCollaboration.getType(),
                                        "Collaboration type should be PARTICIPANTS");
                        assertEquals(Collaboration.Permission.VIEW, capturedCollaboration.getPermission(),
                                        "Permission should be VIEW");
                        assertTrue(capturedCollaboration.getIsActive(), "Should be active");
                }

                /**
                 * Test Case: CT005
                 * Scenario: Add collaborator when diagram does not exist
                 * Expected: Throw EntityNotFoundException
                 * 
                 * CheckDB: Verify findById() is called and returns empty Optional
                 */
                @Test
                @DisplayName("CT005: Exception Path - Diagram not found")
                public void testAddCollaborator_WhenDiagramNotFound_ShouldThrowEntityNotFoundException() {
                        // ARRANGE: Setup mocks
                        when(mockDiagramRepository.findById(DIAGRAM_ID_INVALID))
                                        .thenReturn(Optional.empty());

                        // ACT & ASSERT: Verify exception
                        assertThrows(EntityNotFoundException.class, () -> serviceUnderTest.addCollaborator(
                                        DIAGRAM_ID_INVALID,
                                        USERNAME_COLLABORATOR_NEW,
                                        Collaboration.Permission.VIEW),
                                        "Should throw EntityNotFoundException when diagram not found");

                        // CHECKDB: Verify save was NOT called
                        verify(mockCollaborationRepository, never()).save(any());
                }

                /**
                 * Test Case: CT006
                 * Scenario: Add collaborator when user is already collaborator
                 * Expected: Throw IllegalArgumentException
                 * 
                 * CheckDB: Verify findByDiagramIdAndUsername() returns existing collaboration
                 */
                @Test
                @DisplayName("CT006: Validation Path - User already collaborator")
                public void testAddCollaborator_WhenUserAlreadyCollaborator_ShouldThrowIllegalArgumentException() {
                        // ARRANGE: Setup mocks
                        when(mockDiagramRepository.findById(DIAGRAM_ID_VALID))
                                        .thenReturn(Optional.of(testDiagramEntity));
                        when(mockCollaborationRepository.findByDiagramIdAndUsername(DIAGRAM_ID_VALID,
                                        USERNAME_COLLABORATOR_EXISTING))
                                        .thenReturn(Optional.of(testParticipantCollaboration));

                        // ACT & ASSERT: Verify exception
                        IllegalArgumentException thrownException = assertThrows(
                                        IllegalArgumentException.class,
                                        () -> serviceUnderTest.addCollaborator(
                                                        DIAGRAM_ID_VALID,
                                                        USERNAME_COLLABORATOR_EXISTING,
                                                        Collaboration.Permission.EDIT),
                                        "Should throw IllegalArgumentException when user already collaborator");

                        assertTrue(thrownException.getMessage().contains("already a collaborator"),
                                        "Exception message should mention user already collaborator");

                        // CHECKDB: Verify save was NOT called
                        verify(mockCollaborationRepository, never()).save(any());
                }
        }

        // ============================================================================
        // TEST GROUP 3: updatePermission() - Update collaborator permission
        // ============================================================================

        @Nested
        @DisplayName("Test Group: updatePermission()")
        class UpdatePermissionTests {

                /**
                 * Test Case: CT007
                 * Scenario: Update permission from VIEW to EDIT (permission upgrade)
                 * Expected: Permission updated successfully, no force disconnect
                 * 
                 * CheckDB: Verify collaboration.setPermission() and save() called
                 * CheckDB: Verify forceDisconnect NOT called since not downgrading
                 */
                @Test
                @DisplayName("CT007: Happy Path - Upgrade permission VIEW to EDIT")
                public void testUpdatePermission_WhenUpgradingPermission_ShouldUpdateSuccessfully() {
                        // ARRANGE: Setup mocks
                        testParticipantCollaboration.setPermission(Collaboration.Permission.VIEW);

                        when(mockCollaborationRepository.findById(COLLABORATION_ID_VALID + 1))
                                        .thenReturn(Optional.of(testParticipantCollaboration));

                        // ACT: Call service method
                        serviceUnderTest.updatePermission(COLLABORATION_ID_VALID + 1, Collaboration.Permission.EDIT);

                        // ASSERT & CHECKDB: Verify permission was updated
                        assertEquals(Collaboration.Permission.EDIT, testParticipantCollaboration.getPermission(),
                                        "Permission should be updated to EDIT");

                        // CHECKDB: Verify save was called
                        verify(mockCollaborationRepository, times(1)).save(testParticipantCollaboration);

                        // Verify isUserActiveInDiagram NOT called (no force disconnect for upgrade)
                        verify(mockSessionManager, never()).isUserActiveInDiagram(anyLong(), anyString());
                }

                /**
                 * Test Case: CT008
                 * Scenario: Update permission from FULL_ACCESS to VIEW (downgrade) with active
                 * user
                 * Expected: Permission downgraded, user force disconnected
                 * 
                 * CheckDB: Verify forceDisconnectUser() logic is executed
                 * Rollback: Session state is rolled back on disconnect
                 */
                @Test
                @DisplayName("CT008: Critical Path - Downgrade FULL_ACCESS to VIEW with active user")
                public void testUpdatePermission_WhenDowngradingAndUserActive_ShouldForceDisconnect() {
                        // ARRANGE: Setup mocks - user has FULL_ACCESS and is active
                        Collaboration fullAccessCollaboration = new Collaboration();
                        fullAccessCollaboration.setId(2L);
                        fullAccessCollaboration.setDiagram(testDiagramEntity);
                        fullAccessCollaboration.setUsername(USERNAME_COLLABORATOR_EXISTING);
                        fullAccessCollaboration.setType(Collaboration.CollaborationType.PARTICIPANTS);
                        fullAccessCollaboration.setPermission(Collaboration.Permission.FULL_ACCESS);
                        fullAccessCollaboration.setIsActive(true);

                        Set<String> activeSessionIds = new HashSet<>(Arrays.asList("session1", "session2"));

                        when(mockCollaborationRepository.findById(2L))
                                        .thenReturn(Optional.of(fullAccessCollaboration));
                        when(mockSessionManager.isUserActiveInDiagram(DIAGRAM_ID_VALID, USERNAME_COLLABORATOR_EXISTING))
                                        .thenReturn(true);
                        when(mockSessionManager.getActiveSessions(DIAGRAM_ID_VALID))
                                        .thenReturn(activeSessionIds);
                        when(mockSessionManager.getUsernameForSession("session1"))
                                        .thenReturn(USERNAME_COLLABORATOR_EXISTING);
                        when(mockSessionManager.getUsernameForSession("session2"))
                                        .thenReturn(USERNAME_COLLABORATOR_EXISTING);

                        // ACT: Call service method to downgrade permission
                        serviceUnderTest.updatePermission(2L, Collaboration.Permission.VIEW);

                        // ASSERT: Verify permission was updated
                        assertEquals(Collaboration.Permission.VIEW, fullAccessCollaboration.getPermission(),
                                        "Permission should be downgraded to VIEW");

                        // CHECKDB: Verify forceDisconnectUser was executed
                        verify(mockSessionManager, times(1)).isUserActiveInDiagram(DIAGRAM_ID_VALID,
                                        USERNAME_COLLABORATOR_EXISTING);
                        verify(mockSessionManager, times(1)).getActiveSessions(DIAGRAM_ID_VALID);
                        verify(mockSessionRegistry, times(1)).closeSessions(any(Set.class));
                }

                /**
                 * Test Case: CT009
                 * Scenario: Try to update permission of OWNER
                 * Expected: Throw IllegalArgumentException
                 */
                @Test
                @DisplayName("CT009: Validation Path - Cannot change OWNER permission")
                public void testUpdatePermission_WhenOwnerCollaboration_ShouldThrowIllegalArgumentException() {
                        // ARRANGE: Setup mocks
                        when(mockCollaborationRepository.findById(COLLABORATION_ID_VALID))
                                        .thenReturn(Optional.of(testOwnerCollaboration));

                        // ACT & ASSERT: Verify exception
                        IllegalArgumentException thrownException = assertThrows(
                                        IllegalArgumentException.class,
                                        () -> serviceUnderTest.updatePermission(COLLABORATION_ID_VALID,
                                                        Collaboration.Permission.VIEW),
                                        "Should throw IllegalArgumentException when updating OWNER permission");

                        assertTrue(thrownException.getMessage().contains("Cannot change permission of owner"),
                                        "Exception message should mention cannot change owner permission");

                        // CHECKDB: Verify save was NOT called
                        verify(mockCollaborationRepository, never()).save(any());
                }

                /**
                 * Test Case: CT010
                 * Scenario: Update permission when collaboration not found
                 * Expected: Throw EntityNotFoundException
                 */
                @Test
                @DisplayName("CT010: Exception Path - Collaboration not found")
                public void testUpdatePermission_WhenCollaborationNotFound_ShouldThrowEntityNotFoundException() {
                        // ARRANGE: Setup mocks
                        when(mockCollaborationRepository.findById(COLLABORATION_ID_INVALID))
                                        .thenReturn(Optional.empty());

                        // ACT & ASSERT: Verify exception
                        assertThrows(EntityNotFoundException.class,
                                        () -> serviceUnderTest.updatePermission(COLLABORATION_ID_INVALID,
                                                        Collaboration.Permission.VIEW),
                                        "Should throw EntityNotFoundException when collaboration not found");
                }
        }

        // ============================================================================
        // TEST GROUP 4: removeCollaborator() - Remove collaborator from diagram
        // ============================================================================

        @Nested
        @DisplayName("Test Group: removeCollaborator()")
        class RemoveCollaboratorTests {

                /**
                 * Test Case: CT011
                 * Scenario: Remove participant collaborator (not owner) when not active
                 * Expected: Collaboration deleted successfully
                 * 
                 * CheckDB: Verify delete() is called, forceDisconnect NOT called
                 */
                @Test
                @DisplayName("CT011: Happy Path - Remove inactive participant")
                public void testRemoveCollaborator_WhenParticipantNotActive_ShouldDeleteSuccessfully() {
                        // ARRANGE: Setup mocks
                        when(mockCollaborationRepository.findById(COLLABORATION_ID_VALID + 1))
                                        .thenReturn(Optional.of(testParticipantCollaboration));
                        when(mockSessionManager.isUserActiveInDiagram(DIAGRAM_ID_VALID, USERNAME_COLLABORATOR_EXISTING))
                                        .thenReturn(false);

                        // ACT: Call service method
                        serviceUnderTest.removeCollaborator(COLLABORATION_ID_VALID + 1);

                        // CHECKDB: Verify delete was called
                        verify(mockCollaborationRepository, times(1)).delete(testParticipantCollaboration);

                        // Verify forceDisconnect was NOT called (user not active)
                        verify(mockSessionRegistry, never()).closeSessions(any());
                }

                /**
                 * Test Case: CT012
                 * Scenario: Remove participant collaborator when user is active
                 * Expected: Force disconnect user, then delete collaboration
                 */
                @Test
                @DisplayName("CT012: Critical Path - Remove active participant with force disconnect")
                public void testRemoveCollaborator_WhenParticipantActive_ShouldForceDisconnectThenDelete() {
                        // ARRANGE: Setup mocks
                        Set<String> activeSessions = new HashSet<>(Arrays.asList("session1"));

                        when(mockCollaborationRepository.findById(COLLABORATION_ID_VALID + 1))
                                        .thenReturn(Optional.of(testParticipantCollaboration));
                        when(mockSessionManager.isUserActiveInDiagram(DIAGRAM_ID_VALID, USERNAME_COLLABORATOR_EXISTING))
                                        .thenReturn(true);
                        when(mockSessionManager.getActiveSessions(DIAGRAM_ID_VALID))
                                        .thenReturn(activeSessions);
                        when(mockSessionManager.getUsernameForSession("session1"))
                                        .thenReturn(USERNAME_COLLABORATOR_EXISTING);

                        // ACT: Call service method
                        serviceUnderTest.removeCollaborator(COLLABORATION_ID_VALID + 1);

                        // CHECKDB: Verify forceDisconnect was called
                        verify(mockSessionManager, times(1)).isUserActiveInDiagram(DIAGRAM_ID_VALID,
                                        USERNAME_COLLABORATOR_EXISTING);
                        verify(mockSessionRegistry, times(1)).closeSessions(any(Set.class));

                        // Verify delete was called after disconnect
                        verify(mockCollaborationRepository, times(1)).delete(testParticipantCollaboration);
                }

                /**
                 * Test Case: CT013
                 * Scenario: Try to remove OWNER collaborator
                 * Expected: Throw IllegalArgumentException
                 */
                @Test
                @DisplayName("CT013: Validation Path - Cannot remove OWNER")
                public void testRemoveCollaborator_WhenOwnerCollaboration_ShouldThrowIllegalArgumentException() {
                        // ARRANGE: Setup mocks
                        when(mockCollaborationRepository.findById(COLLABORATION_ID_VALID))
                                        .thenReturn(Optional.of(testOwnerCollaboration));

                        // ACT & ASSERT: Verify exception
                        IllegalArgumentException thrownException = assertThrows(
                                        IllegalArgumentException.class,
                                        () -> serviceUnderTest.removeCollaborator(COLLABORATION_ID_VALID),
                                        "Should throw IllegalArgumentException when removing OWNER");

                        assertTrue(thrownException.getMessage().contains("Cannot remove owner"),
                                        "Exception message should mention cannot remove owner");

                        // CHECKDB: Verify delete was NOT called
                        verify(mockCollaborationRepository, never()).delete(any());
                }
        }

        // ============================================================================
        // TEST GROUP 5: hasAccess() - Check if user has access to diagram
        // ============================================================================

        @Nested
        @DisplayName("Test Group: hasAccess()")
        class HasAccessTests {

                /**
                 * Test Case: CT014
                 * Scenario: Check access when user has collaboration with diagram
                 * Expected: Return true
                 */
                @Test
                @DisplayName("CT014: Happy Path - User has access")
                public void testHasAccess_WhenUserHasCollaboration_ShouldReturnTrue() {
                        // ARRANGE: Setup mocks
                        when(mockCollaborationRepository.hasAccess(DIAGRAM_ID_VALID, USERNAME_COLLABORATOR_EXISTING))
                                        .thenReturn(true);

                        // ACT: Call service method
                        boolean hasAccess = serviceUnderTest.hasAccess(DIAGRAM_ID_VALID,
                                        USERNAME_COLLABORATOR_EXISTING);

                        // ASSERT: Verify result
                        assertTrue(hasAccess, "User should have access");

                        // CHECKDB: Verify repository was called
                        verify(mockCollaborationRepository, times(1))
                                        .hasAccess(DIAGRAM_ID_VALID, USERNAME_COLLABORATOR_EXISTING);
                }

                /**
                 * Test Case: CT015
                 * Scenario: Check access when user does not have collaboration
                 * Expected: Return false
                 */
                @Test
                @DisplayName("CT015: Negative Path - User has no access")
                public void testHasAccess_WhenUserNoCollaboration_ShouldReturnFalse() {
                        // ARRANGE: Setup mocks
                        when(mockCollaborationRepository.hasAccess(DIAGRAM_ID_VALID, "unknown_user"))
                                        .thenReturn(false);

                        // ACT: Call service method
                        boolean hasAccess = serviceUnderTest.hasAccess(DIAGRAM_ID_VALID, "unknown_user");

                        // ASSERT: Verify result
                        assertFalse(hasAccess, "User should not have access");
                }

                /**
                 * Test Case: CT016
                 * Scenario: Access revoked but cache still returns true (intentional fail)
                 * Expected: Fail to document stale permission cache
                 */
                @Test
                @DisplayName("CT016: Fail - Expect false but stale cache returns true")
                public void testHasAccess_WhenAccessRevoked_ShouldFailExpectation() {
                        // ARRANGE: Setup mocks
                        when(mockCollaborationRepository.hasAccess(DIAGRAM_ID_VALID, USERNAME_COLLABORATOR_EXISTING))
                                        .thenReturn(true);

                        // ACT: Call service method
                        boolean hasAccess = serviceUnderTest.hasAccess(DIAGRAM_ID_VALID,
                                        USERNAME_COLLABORATOR_EXISTING);

                        // ASSERT: Intentional failure for documentation
                        assertFalse(hasAccess, "Intentional fail: expected false after revoke, but cache returns true");
                }
        }

        // ============================================================================
        // TEST GROUP 6: getUserCollaboration() - Get user's collaboration info
        // ============================================================================

        @Nested
        @DisplayName("Test Group: getUserCollaboration()")
        class GetUserCollaborationTests {

                /**
                 * Test Case: CT017
                 * Scenario: Get collaboration when user has active collaboration
                 * Expected: Return collaboration DTO
                 */
                @Test
                @DisplayName("CT017: Happy Path - Get user collaboration")
                public void testGetUserCollaboration_WhenUserHasActiveCollaboration_ShouldReturnCollaborationDTO() {
                        // ARRANGE: Setup mocks
                        when(mockCollaborationRepository.findActiveCollaboration(DIAGRAM_ID_VALID,
                                        USERNAME_COLLABORATOR_EXISTING))
                                        .thenReturn(Optional.of(testParticipantCollaboration));

                        // ACT: Call service method
                        CollaborationDTO result = serviceUnderTest.getUserCollaboration(DIAGRAM_ID_VALID,
                                        USERNAME_COLLABORATOR_EXISTING);

                        // ASSERT: Verify result
                        assertNotNull(result, "Result should not be null");
                        assertEquals(USERNAME_COLLABORATOR_EXISTING, result.getUsername(), "Username should match");
                }

                /**
                 * Test Case: CT018
                 * Scenario: Get collaboration when user has no collaboration
                 * Expected: Throw EntityNotFoundException
                 */
                @Test
                @DisplayName("CT018: Exception Path - User collaboration not found")
                public void testGetUserCollaboration_WhenNoActiveCollaboration_ShouldThrowEntityNotFoundException() {
                        // ARRANGE: Setup mocks
                        when(mockCollaborationRepository.findActiveCollaboration(DIAGRAM_ID_VALID, "unknown_user"))
                                        .thenReturn(Optional.empty());

                        // ACT & ASSERT: Verify exception
                        assertThrows(EntityNotFoundException.class,
                                        () -> serviceUnderTest.getUserCollaboration(DIAGRAM_ID_VALID, "unknown_user"),
                                        "Should throw EntityNotFoundException when user has no collaboration");
                }
        }

        // ============================================================================
        // TEST GROUP 7: getOwner() - Get diagram owner
        // ============================================================================

        @Nested
        @DisplayName("Test Group: getOwner()")
        class GetOwnerTests {

                /**
                 * Test Case: CT019
                 * Scenario: Get owner when owner exists
                 * Expected: Return owner collaboration DTO
                 */
                @Test
                @DisplayName("CT019: Happy Path - Get diagram owner")
                public void testGetOwner_WhenOwnerExists_ShouldReturnOwnerDTO() {
                        // ARRANGE: Setup mocks
                        when(mockCollaborationRepository.findByDiagramIdAndType(DIAGRAM_ID_VALID,
                                        Collaboration.CollaborationType.OWNER))
                                        .thenReturn(Optional.of(testOwnerCollaboration));

                        // ACT: Call service method
                        CollaborationDTO result = serviceUnderTest.getOwner(DIAGRAM_ID_VALID);

                        // ASSERT: Verify result
                        assertNotNull(result, "Result should not be null");
                        assertEquals(USERNAME_OWNER, result.getUsername(), "Owner username should match");
                }

                /**
                 * Test Case: CT020
                 * Scenario: Get owner when no owner exists (data integrity issue)
                 * Expected: Throw EntityNotFoundException
                 */
                @Test
                @DisplayName("CT020: Exception Path - Owner not found")
                public void testGetOwner_WhenOwnerNotFound_ShouldThrowEntityNotFoundException() {
                        // ARRANGE: Setup mocks
                        when(mockCollaborationRepository.findByDiagramIdAndType(DIAGRAM_ID_INVALID,
                                        Collaboration.CollaborationType.OWNER))
                                        .thenReturn(Optional.empty());

                        // ACT & ASSERT: Verify exception
                        assertThrows(EntityNotFoundException.class, () -> serviceUnderTest.getOwner(DIAGRAM_ID_INVALID),
                                        "Should throw EntityNotFoundException when owner not found");
                }
        }

        // ============================================================================
        // TEST GROUP 8: countParticipants() - Count diagram participants
        // ============================================================================

        @Nested
        @DisplayName("Test Group: countParticipants()")
        class CountParticipantsTests {

                /**
                 * Test Case: CT021
                 * Scenario: Count participants when diagram has multiple participants
                 * Expected: Return correct count
                 */
                @Test
                @DisplayName("CT021: Happy Path - Count multiple participants")
                public void testCountParticipants_WhenMultipleParticipants_ShouldReturnCount() {
                        // ARRANGE: Setup mocks
                        when(mockCollaborationRepository.countParticipants(DIAGRAM_ID_VALID))
                                        .thenReturn(5);

                        // ACT: Call service method
                        long actualCount = serviceUnderTest.countParticipants(DIAGRAM_ID_VALID);

                        // ASSERT: Verify result
                        assertEquals(5, actualCount, "Count should match expected");

                        // CHECKDB: Verify repository was called
                        verify(mockCollaborationRepository, times(1)).countParticipants(DIAGRAM_ID_VALID);
                }

                /**
                 * Test Case: CT022
                 * Scenario: Count participants when no participants exist
                 * Expected: Return 0
                 */
                @Test
                @DisplayName("CT022: Edge Case - No participants")
                public void testCountParticipants_WhenNoParticipants_ShouldReturnZero() {
                        // ARRANGE: Setup mocks
                        when(mockCollaborationRepository.countParticipants(DIAGRAM_ID_VALID))
                                        .thenReturn(0);

                        // ACT: Call service method
                        long actualCount = serviceUnderTest.countParticipants(DIAGRAM_ID_VALID);

                        // ASSERT: Verify result
                        assertEquals(0, actualCount, "Count should be 0");
                }
        }

        // ============================================================================
        // TEST GROUP 9: createOwner() - Create owner collaboration
        // ============================================================================

        @Nested
        @DisplayName("Test Group: createOwner()")
        class CreateOwnerTests {

                /**
                 * Test Case: CT023
                 * Scenario: Create owner when diagram exists
                 * Expected: Owner collaboration created with FULL_ACCESS permission
                 */
                @Test
                @DisplayName("CT023: Happy Path - Create owner for new diagram")
                public void testCreateOwner_WhenDiagramExists_ShouldCreateOwnerCollaboration() {
                        // ARRANGE: Setup mocks
                        Collaboration savedOwnerCollaboration = new Collaboration();
                        savedOwnerCollaboration.setId(100L);
                        savedOwnerCollaboration.setDiagram(testDiagramEntity);
                        savedOwnerCollaboration.setUsername(USERNAME_OWNER);
                        savedOwnerCollaboration.setType(Collaboration.CollaborationType.OWNER);
                        savedOwnerCollaboration.setPermission(Collaboration.Permission.FULL_ACCESS);
                        savedOwnerCollaboration.setIsActive(true);

                        when(mockDiagramRepository.findById(DIAGRAM_ID_VALID))
                                        .thenReturn(Optional.of(testDiagramEntity));
                        when(mockCollaborationRepository.save(any(Collaboration.class)))
                                        .thenReturn(savedOwnerCollaboration);

                        // ACT: Call service method
                        CollaborationDTO result = serviceUnderTest.createOwner(DIAGRAM_ID_VALID, USERNAME_OWNER);

                        // ASSERT: Verify result
                        assertNotNull(result, "Result should not be null");
                        assertEquals(USERNAME_OWNER, result.getUsername(), "Owner username should match");

                        // CHECKDB: Verify save was called with OWNER type and FULL_ACCESS
                        ArgumentCaptor<Collaboration> savedCaptor = ArgumentCaptor.forClass(Collaboration.class);
                        verify(mockCollaborationRepository, times(1)).save(savedCaptor.capture());

                        Collaboration capturedCollaboration = savedCaptor.getValue();
                        assertEquals(Collaboration.CollaborationType.OWNER, capturedCollaboration.getType(),
                                        "Type should be OWNER");
                        assertEquals(Collaboration.Permission.FULL_ACCESS, capturedCollaboration.getPermission(),
                                        "Permission should be FULL_ACCESS");
                }

                /**
                 * Test Case: CT024
                 * Scenario: Create owner when diagram does not exist
                 * Expected: Throw EntityNotFoundException
                 */
                @Test
                @DisplayName("CT024: Exception Path - Diagram not found")
                public void testCreateOwner_WhenDiagramNotFound_ShouldThrowEntityNotFoundException() {
                        // ARRANGE: Setup mocks
                        when(mockDiagramRepository.findById(DIAGRAM_ID_INVALID))
                                        .thenReturn(Optional.empty());

                        // ACT & ASSERT: Verify exception
                        assertThrows(EntityNotFoundException.class,
                                        () -> serviceUnderTest.createOwner(DIAGRAM_ID_INVALID, USERNAME_OWNER),
                                        "Should throw EntityNotFoundException when diagram not found");

                        // CHECKDB: Verify save was NOT called
                        verify(mockCollaborationRepository, never()).save(any());
                }
        }

        // ============================================================================
        // TEST GROUP 10: deactivateCollaboration() - Deactivate collaboration
        // ============================================================================

        @Nested
        @DisplayName("Test Group: deactivateCollaboration()")
        class DeactivateCollaborationTests {

                /**
                 * Test Case: CT025
                 * Scenario: Deactivate participant collaboration
                 * Expected: Collaboration marked as inactive (soft delete)
                 */
                @Test
                @DisplayName("CT025: Happy Path - Deactivate participant collaboration")
                public void testDeactivateCollaboration_WhenParticipant_ShouldDeactivateSuccessfully() {
                        // ARRANGE: Setup mocks
                        testParticipantCollaboration.setIsActive(true);
                        when(mockCollaborationRepository.findById(COLLABORATION_ID_VALID + 1))
                                        .thenReturn(Optional.of(testParticipantCollaboration));

                        // ACT: Call service method
                        serviceUnderTest.deactivateCollaboration(COLLABORATION_ID_VALID + 1);

                        // ASSERT: Verify deactivated
                        assertFalse(testParticipantCollaboration.getIsActive(), "Should be deactivated");

                        // CHECKDB: Verify save was called
                        verify(mockCollaborationRepository, times(1)).save(testParticipantCollaboration);
                }

                /**
                 * Test Case: CT026
                 * Scenario: Try to deactivate OWNER collaboration
                 * Expected: Throw IllegalArgumentException
                 */
                @Test
                @DisplayName("CT026: Validation Path - Cannot deactivate OWNER")
                public void testDeactivateCollaboration_WhenOwner_ShouldThrowIllegalArgumentException() {
                        // ARRANGE: Setup mocks
                        when(mockCollaborationRepository.findById(COLLABORATION_ID_VALID))
                                        .thenReturn(Optional.of(testOwnerCollaboration));

                        // ACT & ASSERT: Verify exception
                        IllegalArgumentException thrownException = assertThrows(
                                        IllegalArgumentException.class,
                                        () -> serviceUnderTest.deactivateCollaboration(COLLABORATION_ID_VALID),
                                        "Should throw IllegalArgumentException when deactivating OWNER");

                        assertTrue(thrownException.getMessage().contains("Cannot deactivate owner"),
                                        "Exception message should mention cannot deactivate owner");

                        // CHECKDB: Verify save was NOT called
                        verify(mockCollaborationRepository, never()).save(any());
                }
        }

        // ============================================================================
        // TEST GROUP 11: deleteAllByDiagramId() - Delete all diagram collaborations
        // ============================================================================

        @Nested
        @DisplayName("Test Group: deleteAllByDiagramId()")
        class DeleteAllByDiagramIdTests {

                /**
                 * Test Case: CT027
                 * Scenario: Delete all collaborations of a diagram
                 * Expected: All collaborations deleted (soft delete when diagram is deleted)
                 */
                @Test
                @DisplayName("CT027: Happy Path - Delete all collaborations for diagram")
                public void testDeleteAllByDiagramId_ShouldDeleteAllDiagramCollaborations() {
                        // ACT: Call service method
                        serviceUnderTest.deleteAllByDiagramId(DIAGRAM_ID_VALID);

                        // CHECKDB: Verify deleteByDiagramId was called
                        verify(mockCollaborationRepository, times(1)).deleteByDiagramId(DIAGRAM_ID_VALID);
                        verifyNoMoreInteractions(mockCollaborationRepository);
                }

                /**
                 * Test Case: CT028
                 * Scenario: Delete all collaborations when diagram has no collaborations
                 * Expected: Delete method still called (no records affected but method
                 * executed)
                 */
                @Test
                @DisplayName("CT028: Edge Case - Delete from diagram with no collaborations")
                public void testDeleteAllByDiagramId_WhenNoDiagramCollaborations_ShouldStillCallDelete() {
                        // ACT: Call service method
                        serviceUnderTest.deleteAllByDiagramId(DIAGRAM_ID_VALID);

                        // CHECKDB: Verify deleteByDiagramId was called even if no records
                        verify(mockCollaborationRepository, times(1)).deleteByDiagramId(DIAGRAM_ID_VALID);
                }
        }

        // ============================================================================
        // INTEGRATION TEST: Force Disconnect Logic (Complex branch coverage)
        // ============================================================================

        @Nested
        @DisplayName("Test Group: Force Disconnect Logic")
        class ForceDisconnectLogicTests {

                /**
                 * Test Case: CT029
                 * Scenario: Force disconnect user with no active sessions
                 * Expected: Log warning, no sessions closed
                 */
                @Test
                @DisplayName("CT029: Edge Case - Force disconnect with no active sessions")
                public void testUpdatePermission_ForceDisconnect_WhenNoActiveSessions_ShouldLogWarning() {
                        // ARRANGE: Setup mocks
                        Collaboration fullAccessCollaboration = new Collaboration();
                        fullAccessCollaboration.setId(2L);
                        fullAccessCollaboration.setDiagram(testDiagramEntity);
                        fullAccessCollaboration.setUsername(USERNAME_COLLABORATOR_EXISTING);
                        fullAccessCollaboration.setType(Collaboration.CollaborationType.PARTICIPANTS);
                        fullAccessCollaboration.setPermission(Collaboration.Permission.FULL_ACCESS);
                        fullAccessCollaboration.setIsActive(true);

                        // Setup: User is marked active but has no sessions
                        when(mockCollaborationRepository.findById(2L))
                                        .thenReturn(Optional.of(fullAccessCollaboration));
                        when(mockSessionManager.isUserActiveInDiagram(DIAGRAM_ID_VALID, USERNAME_COLLABORATOR_EXISTING))
                                        .thenReturn(true);
                        when(mockSessionManager.getActiveSessions(DIAGRAM_ID_VALID))
                                        .thenReturn(new HashSet<>()); // Empty sessions

                        // ACT: Call service method
                        serviceUnderTest.updatePermission(2L, Collaboration.Permission.VIEW);

                        // ASSERT: Verify no sessions were closed
                        verify(mockSessionRegistry, never()).closeSessions(any());
                }

                /**
                 * Test Case: CT030
                 * Scenario: Force disconnect with exception handling
                 * Expected: Exception caught, permission still updated
                 */
                @Test
                @DisplayName("CT030: Error Handling - Force disconnect throws exception")
                public void testUpdatePermission_ForceDisconnect_WhenExceptionThrown_ShouldContinueWithUpdate() {
                        // ARRANGE: Setup mocks with exception
                        Collaboration fullAccessCollaboration = new Collaboration();
                        fullAccessCollaboration.setId(2L);
                        fullAccessCollaboration.setDiagram(testDiagramEntity);
                        fullAccessCollaboration.setUsername(USERNAME_COLLABORATOR_EXISTING);
                        fullAccessCollaboration.setType(Collaboration.CollaborationType.PARTICIPANTS);
                        fullAccessCollaboration.setPermission(Collaboration.Permission.FULL_ACCESS);
                        fullAccessCollaboration.setIsActive(true);

                        when(mockCollaborationRepository.findById(2L))
                                        .thenReturn(Optional.of(fullAccessCollaboration));
                        when(mockSessionManager.isUserActiveInDiagram(DIAGRAM_ID_VALID, USERNAME_COLLABORATOR_EXISTING))
                                        .thenReturn(true);
                        when(mockSessionManager.getActiveSessions(DIAGRAM_ID_VALID))
                                        .thenThrow(new RuntimeException("Session error"));

                        // ACT: Call service method
                        serviceUnderTest.updatePermission(2L, Collaboration.Permission.VIEW);

                        // ASSERT: Permission should still be updated despite exception
                        assertEquals(Collaboration.Permission.VIEW, fullAccessCollaboration.getPermission(),
                                        "Permission should be updated even if force disconnect fails");

                        // CHECKDB: Verify save was called
                        verify(mockCollaborationRepository, times(1)).save(fullAccessCollaboration);
                }
        }
}