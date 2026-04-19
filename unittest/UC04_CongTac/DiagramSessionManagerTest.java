/**
 * ============================================================================
 * UNIT TEST CLASS: DiagramSessionManagerTest
 * ============================================================================
 * Purpose: Comprehensive unit tests for DiagramSessionManager
 * - Tests all 8 public methods covering branch level (cấp 2) code coverage
 * - Tests session tracking logic and state management
 * - Ensures statistics collection for active users/sessions
 * - Includes detailed Test Case IDs and meaningful variable names
 * 
 * Test Scope:
 * - Service: DiagramSessionManager (Statistics engine)
 * - Coverage Target: Line ≥ 85%, Branch ≥ 80%
 * - Total Test Cases: ~20 test methods
 * - Statistics Metrics: Active users, sessions, connection tracking
 * 
 * ROLLBACK STRATEGY:
 * - @BeforeEach: Creates fresh instance of DiagramSessionManager for clean state
 * - No previous test data affects subsequent tests
 * 
 * CHECKDB STRATEGY (Data Structure Verification):
 * - Verify internal map states using getter methods
 * - Assert collection sizes and contents
 * - Verify state transitions: joinDiagram → leaveDiagram → verify empty
 * 
 * Author: QA Team
 * Date: 19/4/2026
 * Version: 1.0
 * ============================================================================
 */

package com.example.react_flow_be.config;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Test class for DiagramSessionManager
 * 
 * No mocks needed - tests actual in-memory session management logic
 * Verifies concurrent session handling using ConcurrentHashMap
 */
@DisplayName("DiagramSessionManager - Unit Tests (Cấp 2 Coverage)")
class DiagramSessionManagerTest {

    // ============================================================================
    // TEST FIXTURES & CONSTANTS
    // ============================================================================

    private DiagramSessionManager sessionManagerUnderTest;

    private static final Long DIAGRAM_ID_VALID = 1L;
    private static final Long DIAGRAM_ID_ANOTHER = 2L;
    private static final Long DIAGRAM_ID_NONEXISTENT = 999L;

    private static final String SESSION_ID_1 = "session-001";
    private static final String SESSION_ID_2 = "session-002";
    private static final String SESSION_ID_3 = "session-003";

    private static final String USERNAME_USER1 = "user_alice";
    private static final String USERNAME_USER2 = "user_bob";
    private static final String USERNAME_USER3 = "user_charlie";

    /**
     * Test Case: SETUP-001
     * Setup: Initialize fresh DiagramSessionManager before each test
     * Purpose: Rollback - Each test starts with clean internal state
     */
    @BeforeEach
    public void testSetup_InitializeCleanSessionManager() {
        // Create new instance with empty internal maps - ensures clean state
        sessionManagerUnderTest = new DiagramSessionManager();
    }

    // ============================================================================
    // TEST GROUP 1: joinDiagram() - User joins diagram session
    // ============================================================================

    @Nested
    @DisplayName("Test Group: joinDiagram()")
    class JoinDiagramTests {

        /**
         * Test Case: CT031
         * Scenario: User joins diagram for first time
         * Expected: Session registered, user tracked, diagram has 1 active session
         * 
         * CheckDB: Verify internal maps contain correct entries:
         * - diagramSessions[diagramId] contains sessionId
         * - sessionToDiagram[sessionId] = diagramId
         * - sessionToUsername[sessionId] = username
         * - diagramUserSessions[diagramId][username] = sessionId
         */
        @Test
        @DisplayName("CT031: Happy Path - Join diagram first time")
        public void testJoinDiagram_WhenNewSession_ShouldRegisterUserInDiagram() {
            // ACT: User joins diagram
            sessionManagerUnderTest.joinDiagram(DIAGRAM_ID_VALID, SESSION_ID_1, USERNAME_USER1);

            // CHECKDB: Verify session is tracked for diagram
            Set<String> diagramSessions = sessionManagerUnderTest.getActiveSessions(DIAGRAM_ID_VALID);
            assertTrue(diagramSessions.contains(SESSION_ID_1),
                    "Session should be registered in diagram");

            // CHECKDB: Verify diagram is mapped to session
            Long mappedDiagramId = sessionManagerUnderTest.getDiagramForSession(SESSION_ID_1);
            assertEquals(DIAGRAM_ID_VALID, mappedDiagramId,
                    "Session should be mapped to correct diagram");

            // CHECKDB: Verify username is tracked
            String mappedUsername = sessionManagerUnderTest.getUsernameForSession(SESSION_ID_1);
            assertEquals(USERNAME_USER1, mappedUsername,
                    "Session should be mapped to correct username");

            // CHECKDB: Verify user appears in active usernames
            Set<String> activeUsernames = sessionManagerUnderTest.getActiveUsernames(DIAGRAM_ID_VALID);
            assertTrue(activeUsernames.contains(USERNAME_USER1),
                    "User should appear in active usernames");

            // CHECKDB: Verify user count
            int activeUserCount = sessionManagerUnderTest.getActiveUserCount(DIAGRAM_ID_VALID);
            assertEquals(1, activeUserCount, "Should have 1 active user");
        }

        /**
         * Test Case: CT032
         * Scenario: Multiple users join same diagram
         * Expected: All sessions tracked, multiple users active, correct counts
         * 
         * CheckDB: Verify all sessions and users are tracked correctly
         */
        @Test
        @DisplayName("CT032: Happy Path - Multiple users join same diagram")
        public void testJoinDiagram_WhenMultipleUsersJoin_ShouldTrackAllSessions() {
            // ACT: Multiple users join same diagram
            sessionManagerUnderTest.joinDiagram(DIAGRAM_ID_VALID, SESSION_ID_1, USERNAME_USER1);
            sessionManagerUnderTest.joinDiagram(DIAGRAM_ID_VALID, SESSION_ID_2, USERNAME_USER2);
            sessionManagerUnderTest.joinDiagram(DIAGRAM_ID_VALID, SESSION_ID_3, USERNAME_USER3);

            // CHECKDB: Verify all sessions are tracked
            Set<String> allSessions = sessionManagerUnderTest.getActiveSessions(DIAGRAM_ID_VALID);
            assertEquals(3, allSessions.size(), "Should have 3 sessions");
            assertTrue(allSessions.contains(SESSION_ID_1), "Should contain session 1");
            assertTrue(allSessions.contains(SESSION_ID_2), "Should contain session 2");
            assertTrue(allSessions.contains(SESSION_ID_3), "Should contain session 3");

            // CHECKDB: Verify all users are tracked
            Set<String> activeUsernames = sessionManagerUnderTest.getActiveUsernames(DIAGRAM_ID_VALID);
            assertEquals(3, activeUsernames.size(), "Should have 3 unique users");

            // CHECKDB: Verify user count
            int userCount = sessionManagerUnderTest.getActiveUserCount(DIAGRAM_ID_VALID);
            assertEquals(3, userCount, "Should have 3 active sessions/users");
        }

        /**
         * Test Case: CT033
         * Scenario: Same user joins multiple diagrams with different sessions
         * Expected: Both sessions remain tracked in their respective diagrams
         * 
         * CheckDB: Verify sessions stay in both diagrams
         */
        @Test
        @DisplayName("CT033: Happy Path - Same user joins different diagrams")
        public void testJoinDiagram_WhenSameUserJoinsDifferentDiagrams_ShouldSwitchDiagrams() {
            // ACT: Same user joins first diagram
            sessionManagerUnderTest.joinDiagram(DIAGRAM_ID_VALID, SESSION_ID_1, USERNAME_USER1);

            // CHECKDB: Verify user in first diagram
            Set<String> diagramOneSessions = sessionManagerUnderTest.getActiveSessions(DIAGRAM_ID_VALID);
            assertEquals(1, diagramOneSessions.size(), "Diagram 1 should have 1 session");

            // ACT: Same user switches to another diagram with new session
            sessionManagerUnderTest.joinDiagram(DIAGRAM_ID_ANOTHER, SESSION_ID_2, USERNAME_USER1);

            // CHECKDB: Verify old session remains in first diagram
            diagramOneSessions = sessionManagerUnderTest.getActiveSessions(DIAGRAM_ID_VALID);
            assertEquals(1, diagramOneSessions.size(), "Diagram 1 should still have 1 session");
            assertTrue(diagramOneSessions.contains(SESSION_ID_1), "Diagram 1 should still contain session 1");

            // CHECKDB: Verify new session in second diagram
            Set<String> diagramTwoSessions = sessionManagerUnderTest.getActiveSessions(DIAGRAM_ID_ANOTHER);
            assertEquals(1, diagramTwoSessions.size(), "Diagram 2 should have 1 session");
            assertTrue(diagramTwoSessions.contains(SESSION_ID_2), "Diagram 2 should contain session 2");
        }

        /**
         * Test Case: CT034
         * Scenario: Same user joins same diagram with multiple sessions (e.g., multiple
         * browser tabs)
         * Expected: Multiple sessions tracked separately for same user
         */
        @Test
        @DisplayName("CT034: Edge Case - Same user multiple sessions in same diagram")
        public void testJoinDiagram_WhenSameUserMultipleSessions_ShouldTrackBothSessions() {
            // ACT: Same user joins with two different sessions
            sessionManagerUnderTest.joinDiagram(DIAGRAM_ID_VALID, SESSION_ID_1, USERNAME_USER1);
            sessionManagerUnderTest.joinDiagram(DIAGRAM_ID_VALID, SESSION_ID_2, USERNAME_USER1);

            // CHECKDB: Verify both sessions exist in diagram
            Set<String> allSessions = sessionManagerUnderTest.getActiveSessions(DIAGRAM_ID_VALID);
            assertEquals(2, allSessions.size(), "Should have 2 sessions");

            // CHECKDB: Verify both map to same username
            String username1 = sessionManagerUnderTest.getUsernameForSession(SESSION_ID_1);
            String username2 = sessionManagerUnderTest.getUsernameForSession(SESSION_ID_2);
            assertEquals(USERNAME_USER1, username1, "Session 1 maps to correct user");
            assertEquals(USERNAME_USER1, username2, "Session 2 maps to correct user");

            // CHECKDB: Verify user count is 2 (2 sessions from same user)
            int sessionCount = sessionManagerUnderTest.getActiveUserCount(DIAGRAM_ID_VALID);
            assertEquals(2, sessionCount, "Should have 2 active session instances");

            // But unique users count should be 1
            Set<String> uniqueUsers = sessionManagerUnderTest.getActiveUsernames(DIAGRAM_ID_VALID);
            assertEquals(1, uniqueUsers.size(), "Should have only 1 unique user");
        }
    }

    // ============================================================================
    // TEST GROUP 2: leaveDiagram() - User leaves diagram session
    // ============================================================================

    @Nested
    @DisplayName("Test Group: leaveDiagram()")
    class LeaveDiagramTests {

        /**
         * Test Case: CT035
         * Scenario: User leaves diagram (session ends)
         * Expected: Session removed, user no longer tracked, diagram updates counts
         * 
         * CheckDB: Verify internal maps are cleaned up on leave
         * Rollback: State clean after leave
         */
        @Test
        @DisplayName("CT035: Happy Path - Leave diagram updates state")
        public void testLeaveDiagram_WhenSessionLeaves_ShouldRemoveFromDiagram() {
            // ARRANGE: User joins first
            sessionManagerUnderTest.joinDiagram(DIAGRAM_ID_VALID, SESSION_ID_1, USERNAME_USER1);

            // Verify user is in diagram
            int countBefore = sessionManagerUnderTest.getActiveUserCount(DIAGRAM_ID_VALID);
            assertEquals(1, countBefore, "Should have 1 user before leave");

            // ACT: User leaves diagram
            sessionManagerUnderTest.leaveDiagram(SESSION_ID_1);

            // CHECKDB: Verify session removed from diagram
            Set<String> remainingSessions = sessionManagerUnderTest.getActiveSessions(DIAGRAM_ID_VALID);
            assertTrue(remainingSessions.isEmpty(), "Diagram should have no sessions after leave");

            // CHECKDB: Verify user count updated
            int countAfter = sessionManagerUnderTest.getActiveUserCount(DIAGRAM_ID_VALID);
            assertEquals(0, countAfter, "Should have 0 users after leave");

            // CHECKDB: Verify diagram mapping cleared
            Long mappedDiagram = sessionManagerUnderTest.getDiagramForSession(SESSION_ID_1);
            assertNull(mappedDiagram, "Session should not map to any diagram after leave");

            // CHECKDB: Verify username mapping cleared
            String mappedUser = sessionManagerUnderTest.getUsernameForSession(SESSION_ID_1);
            assertNull(mappedUser, "Session should not map to any user after leave");
        }

        /**
         * Test Case: CT036
         * Scenario: One of multiple users leaves diagram
         * Expected: Other users remain, counts updated correctly
         * 
         * CheckDB: Verify selective user removal
         */
        @Test
        @DisplayName("CT036: Happy Path - Remove one user while others stay")
        public void testLeaveDiagram_WhenMultipleUsersLeaveOne_ShouldRemoveOnlyThatUser() {
            // ARRANGE: Multiple users in diagram
            sessionManagerUnderTest.joinDiagram(DIAGRAM_ID_VALID, SESSION_ID_1, USERNAME_USER1);
            sessionManagerUnderTest.joinDiagram(DIAGRAM_ID_VALID, SESSION_ID_2, USERNAME_USER2);
            sessionManagerUnderTest.joinDiagram(DIAGRAM_ID_VALID, SESSION_ID_3, USERNAME_USER3);

            // ACT: Only user1 leaves
            sessionManagerUnderTest.leaveDiagram(SESSION_ID_1);

            // CHECKDB: Verify 2 sessions remain
            Set<String> remainingSessions = sessionManagerUnderTest.getActiveSessions(DIAGRAM_ID_VALID);
            assertEquals(2, remainingSessions.size(), "Should have 2 remaining sessions");
            assertFalse(remainingSessions.contains(SESSION_ID_1), "User1 session should be removed");
            assertTrue(remainingSessions.contains(SESSION_ID_2), "User2 session should remain");
            assertTrue(remainingSessions.contains(SESSION_ID_3), "User3 session should remain");

            // CHECKDB: Verify 2 users remain
            Set<String> remainingUsers = sessionManagerUnderTest.getActiveUsernames(DIAGRAM_ID_VALID);
            assertEquals(2, remainingUsers.size(), "Should have 2 remaining users");
            assertFalse(remainingUsers.contains(USERNAME_USER1), "User1 should be removed");
            assertTrue(remainingUsers.contains(USERNAME_USER2), "User2 should remain");
            assertTrue(remainingUsers.contains(USERNAME_USER3), "User3 should remain");
        }

        /**
         * Test Case: CT037
         * Scenario: Last user leaves diagram
         * Expected: Diagram entry completely removed from tracking
         * 
         * CheckDB: Verify diagram cleanup when empty
         */
        @Test
        @DisplayName("CT037: Edge Case - Last user leaves diagram")
        public void testLeaveDiagram_WhenLastUserLeaves_ShouldCleanupDiagramEntry() {
            // ARRANGE: User joins
            sessionManagerUnderTest.joinDiagram(DIAGRAM_ID_VALID, SESSION_ID_1, USERNAME_USER1);

            // ACT: User leaves (last in diagram)
            sessionManagerUnderTest.leaveDiagram(SESSION_ID_1);

            // CHECKDB: Verify diagram is removed from active diagrams
            Set<Long> activeDiagrams = sessionManagerUnderTest.getActiveDiagrams();
            assertFalse(activeDiagrams.contains(DIAGRAM_ID_VALID),
                    "Empty diagram should be removed from active diagrams");

            // CHECKDB: Verify no sessions remain
            Set<String> sessions = sessionManagerUnderTest.getActiveSessions(DIAGRAM_ID_VALID);
            assertTrue(sessions.isEmpty(), "Should return empty set for cleaned diagram");
        }

        /**
         * Test Case: CT038
         * Scenario: Leave from non-existent session
         * Expected: No errors, graceful handling
         */
        @Test
        @DisplayName("CT038: Edge Case - Leave with invalid session ID")
        public void testLeaveDiagram_WhenSessionNotExists_ShouldNotThrowError() {
            // ACT: Leave with non-existent session - should not throw
            assertDoesNotThrow(() -> sessionManagerUnderTest.leaveDiagram(SESSION_ID_1),
                    "Should not throw exception for non-existent session");

            // CHECKDB: Verify system still healthy
            Set<Long> activeDiagrams = sessionManagerUnderTest.getActiveDiagrams();
            assertTrue(activeDiagrams.isEmpty(), "Should have no active diagrams");
        }

        /**
         * Test Case: CT039
         * Scenario: Same user with multiple sessions leaves one
         * Expected: Only that session removed, user inactive in username map
         */
        @Test
        @DisplayName("CT039: Edge Case - Leave one of multiple user sessions")
        public void testLeaveDiagram_WhenUserHasMultipleSessions_ShouldRemoveOnlyThatSession() {
            // ARRANGE: Same user joins with 2 sessions
            sessionManagerUnderTest.joinDiagram(DIAGRAM_ID_VALID, SESSION_ID_1, USERNAME_USER1);
            sessionManagerUnderTest.joinDiagram(DIAGRAM_ID_VALID, SESSION_ID_2, USERNAME_USER1);

            // ACT: Leave one session
            sessionManagerUnderTest.leaveDiagram(SESSION_ID_1);

            // CHECKDB: Verify other session remains
            Set<String> remainingSessions = sessionManagerUnderTest.getActiveSessions(DIAGRAM_ID_VALID);
            assertEquals(1, remainingSessions.size(), "Should have 1 session remaining");
            assertTrue(remainingSessions.contains(SESSION_ID_2), "Session 2 should remain");

            // CHECKDB: Verify username map no longer contains user
            Set<String> activeUsers = sessionManagerUnderTest.getActiveUsernames(DIAGRAM_ID_VALID);
            assertFalse(activeUsers.contains(USERNAME_USER1), "User should be inactive in username map");
        }
    }

    // ============================================================================
    // TEST GROUP 3: getActiveSessions() - Get all active sessions in diagram
    // ============================================================================

    @Nested
    @DisplayName("Test Group: getActiveSessions()")
    class GetActiveSessionsTests {

        /**
         * Test Case: CT040
         * Scenario: Get sessions when diagram has active sessions
         * Expected: Return set with all session IDs
         */
        @Test
        @DisplayName("CT040: Happy Path - Get active sessions")
        public void testGetActiveSessions_WhenSessionsExist_ShouldReturnAllSessions() {
            // ARRANGE: Multiple users join
            sessionManagerUnderTest.joinDiagram(DIAGRAM_ID_VALID, SESSION_ID_1, USERNAME_USER1);
            sessionManagerUnderTest.joinDiagram(DIAGRAM_ID_VALID, SESSION_ID_2, USERNAME_USER2);

            // ACT: Get active sessions
            Set<String> activeSessions = sessionManagerUnderTest.getActiveSessions(DIAGRAM_ID_VALID);

            // CHECKDB: Verify correct sessions returned
            assertEquals(2, activeSessions.size(), "Should return 2 sessions");
            assertTrue(activeSessions.contains(SESSION_ID_1), "Should contain session 1");
            assertTrue(activeSessions.contains(SESSION_ID_2), "Should contain session 2");
        }

        /**
         * Test Case: CT041
         * Scenario: Get sessions from diagram with no active sessions
         * Expected: Return empty set
         */
        @Test
        @DisplayName("CT041: Edge Case - Get sessions from empty diagram")
        public void testGetActiveSessions_WhenNoDiagramSessions_ShouldReturnEmptySet() {
            // ACT: Get sessions from non-existent diagram
            Set<String> activeSessions = sessionManagerUnderTest.getActiveSessions(DIAGRAM_ID_NONEXISTENT);

            // CHECKDB: Verify empty set
            assertNotNull(activeSessions, "Should return non-null set");
            assertTrue(activeSessions.isEmpty(), "Should return empty set");
        }

        /**
         * Test Case: CT042
         * Scenario: Returned set is independent copy (modifications don't affect
         * internal state)
         * Expected: Modifying returned set should not affect internal tracking
         */
        @Test
        @DisplayName("CT042: Safety Check - Returned set is defensive copy")
        public void testGetActiveSessions_WhenModifyingReturnedSet_ShouldNotAffectInternalState() {
            // ARRANGE: User joins
            sessionManagerUnderTest.joinDiagram(DIAGRAM_ID_VALID, SESSION_ID_1, USERNAME_USER1);

            // ACT: Get sessions and modify returned set
            Set<String> activeSessions = sessionManagerUnderTest.getActiveSessions(DIAGRAM_ID_VALID);
            activeSessions.add("fake-session"); // Try to add fake session

            // CHECKDB: Verify internal state not affected
            Set<String> sessionsAgain = sessionManagerUnderTest.getActiveSessions(DIAGRAM_ID_VALID);
            assertEquals(1, sessionsAgain.size(), "Internal state should not be modified");
            assertFalse(sessionsAgain.contains("fake-session"), "Fake session should not be tracked");
        }
    }

    // ============================================================================
    // TEST GROUP 4: getDiagramForSession() - Get diagram from session
    // ============================================================================

    @Nested
    @DisplayName("Test Group: getDiagramForSession()")
    class GetDiagramForSessionTests {

        /**
         * Test Case: CT043
         * Scenario: Get diagram when session exists
         * Expected: Return correct diagram ID
         */
        @Test
        @DisplayName("CT043: Happy Path - Get diagram for session")
        public void testGetDiagramForSession_WhenSessionExists_ShouldReturnDiagramId() {
            // ARRANGE: User joins diagram
            sessionManagerUnderTest.joinDiagram(DIAGRAM_ID_VALID, SESSION_ID_1, USERNAME_USER1);

            // ACT: Get diagram for session
            Long diagramId = sessionManagerUnderTest.getDiagramForSession(SESSION_ID_1);

            // ASSERT: Verify correct diagram
            assertEquals(DIAGRAM_ID_VALID, diagramId, "Should return correct diagram ID");
        }

        /**
         * Test Case: CT044
         * Scenario: Get diagram for non-existent session
         * Expected: Return null
         */
        @Test
        @DisplayName("CT044: Edge Case - Session not found")
        public void testGetDiagramForSession_WhenSessionNotExists_ShouldReturnNull() {
            // ACT: Get diagram for non-existent session
            Long diagramId = sessionManagerUnderTest.getDiagramForSession(SESSION_ID_1);

            // ASSERT: Verify null returned
            assertNull(diagramId, "Should return null for non-existent session");
        }

        /**
         * Test Case: CT045
         * Scenario: Session reassigned to different diagram
         * Expected: Return new diagram ID
         */
        @Test
        @DisplayName("CT045: State Change - Session reassigned to different diagram")
        public void testGetDiagramForSession_WhenSessionReassigned_ShouldReturnNewDiagram() {
            // ARRANGE: User joins first diagram
            sessionManagerUnderTest.joinDiagram(DIAGRAM_ID_VALID, SESSION_ID_1, USERNAME_USER1);
            assertEquals(DIAGRAM_ID_VALID, sessionManagerUnderTest.getDiagramForSession(SESSION_ID_1),
                    "Should initially map to diagram 1");

            // ACT: User joins different diagram with same session
            sessionManagerUnderTest.joinDiagram(DIAGRAM_ID_ANOTHER, SESSION_ID_1, USERNAME_USER1);

            // CHECKDB: Verify session now maps to new diagram
            Long newDiagramId = sessionManagerUnderTest.getDiagramForSession(SESSION_ID_1);
            assertEquals(DIAGRAM_ID_ANOTHER, newDiagramId, "Should map to diagram 2 after join");
        }
    }

    // ============================================================================
    // TEST GROUP 5: getUsernameForSession() - Get username from session
    // ============================================================================

    @Nested
    @DisplayName("Test Group: getUsernameForSession()")
    class GetUsernameForSessionTests {

        /**
         * Test Case: CT046
         * Scenario: Get username when session exists
         * Expected: Return correct username
         */
        @Test
        @DisplayName("CT046: Happy Path - Get username for session")
        public void testGetUsernameForSession_WhenSessionExists_ShouldReturnUsername() {
            // ARRANGE: User joins
            sessionManagerUnderTest.joinDiagram(DIAGRAM_ID_VALID, SESSION_ID_1, USERNAME_USER1);

            // ACT: Get username for session
            String username = sessionManagerUnderTest.getUsernameForSession(SESSION_ID_1);

            // ASSERT: Verify correct username
            assertEquals(USERNAME_USER1, username, "Should return correct username");
        }

        /**
         * Test Case: CT047
         * Scenario: Get username for non-existent session
         * Expected: Return null
         */
        @Test
        @DisplayName("CT047: Edge Case - Session not found")
        public void testGetUsernameForSession_WhenSessionNotExists_ShouldReturnNull() {
            // ACT: Get username for non-existent session
            String username = sessionManagerUnderTest.getUsernameForSession(SESSION_ID_1);

            // ASSERT: Verify null
            assertNull(username, "Should return null for non-existent session");
        }
    }

    // ============================================================================
    // TEST GROUP 6: getActiveUsernames() - Get all active users in diagram
    // ============================================================================

    @Nested
    @DisplayName("Test Group: getActiveUsernames()")
    class GetActiveUsernamesTests {

        /**
         * Test Case: CT048
         * Scenario: Get usernames when multiple users active
         * Expected: Return set with all unique usernames
         */
        @Test
        @DisplayName("CT048: Happy Path - Get active usernames")
        public void testGetActiveUsernames_WhenMultipleUsersActive_ShouldReturnAllUsernames() {
            // ARRANGE: Multiple users join
            sessionManagerUnderTest.joinDiagram(DIAGRAM_ID_VALID, SESSION_ID_1, USERNAME_USER1);
            sessionManagerUnderTest.joinDiagram(DIAGRAM_ID_VALID, SESSION_ID_2, USERNAME_USER2);
            sessionManagerUnderTest.joinDiagram(DIAGRAM_ID_VALID, SESSION_ID_3, USERNAME_USER3);

            // ACT: Get active usernames
            Set<String> activeUsernames = sessionManagerUnderTest.getActiveUsernames(DIAGRAM_ID_VALID);

            // CHECKDB: Verify all unique users
            assertEquals(3, activeUsernames.size(), "Should have 3 unique users");
            assertTrue(activeUsernames.contains(USERNAME_USER1), "Should contain user1");
            assertTrue(activeUsernames.contains(USERNAME_USER2), "Should contain user2");
            assertTrue(activeUsernames.contains(USERNAME_USER3), "Should contain user3");
        }

        /**
         * Test Case: CT049
         * Scenario: Get usernames when same user has multiple sessions
         * Expected: Return set with only 1 username (unique)
         */
        @Test
        @DisplayName("CT049: Edge Case - Same user multiple sessions")
        public void testGetActiveUsernames_WhenSameUserMultipleSessions_ShouldReturnUniqueUsers() {
            // ARRANGE: Same user joins with 2 sessions
            sessionManagerUnderTest.joinDiagram(DIAGRAM_ID_VALID, SESSION_ID_1, USERNAME_USER1);
            sessionManagerUnderTest.joinDiagram(DIAGRAM_ID_VALID, SESSION_ID_2, USERNAME_USER1);

            // ACT: Get active usernames
            Set<String> activeUsernames = sessionManagerUnderTest.getActiveUsernames(DIAGRAM_ID_VALID);

            // CHECKDB: Verify only 1 unique user
            assertEquals(1, activeUsernames.size(), "Should have only 1 unique user");
            assertTrue(activeUsernames.contains(USERNAME_USER1), "Should contain user1");
        }

        /**
         * Test Case: CT050
         * Scenario: Get usernames from empty diagram
         * Expected: Return empty set
         */
        @Test
        @DisplayName("CT050: Edge Case - No users in diagram")
        public void testGetActiveUsernames_WhenNoDiagramUsers_ShouldReturnEmptySet() {
            // ACT: Get usernames from empty diagram
            Set<String> activeUsernames = sessionManagerUnderTest.getActiveUsernames(DIAGRAM_ID_NONEXISTENT);

            // ASSERT: Verify empty set
            assertNotNull(activeUsernames, "Should return non-null set");
            assertTrue(activeUsernames.isEmpty(), "Should return empty set");
        }
    }

    // ============================================================================
    // TEST GROUP 7: getActiveUserCount() - Count active users/sessions in diagram
    // ============================================================================

    @Nested
    @DisplayName("Test Group: getActiveUserCount()")
    class GetActiveUserCountTests {

        /**
         * Test Case: CT051
         * Scenario: Count when multiple users active
         * Expected: Return total session count (not unique users)
         */
        @Test
        @DisplayName("CT051: Happy Path - Count active sessions")
        public void testGetActiveUserCount_WhenMultipleSessionsActive_ShouldReturnSessionCount() {
            // ARRANGE: Multiple users join
            sessionManagerUnderTest.joinDiagram(DIAGRAM_ID_VALID, SESSION_ID_1, USERNAME_USER1);
            sessionManagerUnderTest.joinDiagram(DIAGRAM_ID_VALID, SESSION_ID_2, USERNAME_USER2);

            // ACT: Get active user count
            int count = sessionManagerUnderTest.getActiveUserCount(DIAGRAM_ID_VALID);

            // ASSERT: Verify count
            assertEquals(2, count, "Should return 2 active sessions");
        }

        /**
         * Test Case: CT052
         * Scenario: Same user with multiple sessions
         * Expected: Return total sessions (counts multiple sessions from same user)
         */
        @Test
        @DisplayName("CT052: Edge Case - Count includes same user multiple sessions")
        public void testGetActiveUserCount_WhenSameUserMultipleSessions_ShouldCountAllSessions() {
            // ARRANGE: Same user joins with 2 sessions
            sessionManagerUnderTest.joinDiagram(DIAGRAM_ID_VALID, SESSION_ID_1, USERNAME_USER1);
            sessionManagerUnderTest.joinDiagram(DIAGRAM_ID_VALID, SESSION_ID_2, USERNAME_USER1);

            // ACT: Get active user count
            int count = sessionManagerUnderTest.getActiveUserCount(DIAGRAM_ID_VALID);

            // ASSERT: Verify count includes all sessions
            assertEquals(2, count, "Should count all sessions including from same user");
        }

        /**
         * Test Case: CT053
         * Scenario: Count from empty diagram
         * Expected: Return 0
         */
        @Test
        @DisplayName("CT053: Edge Case - No sessions")
        public void testGetActiveUserCount_WhenNoDiagramSessions_ShouldReturnZero() {
            // ACT: Get count from empty diagram
            int count = sessionManagerUnderTest.getActiveUserCount(DIAGRAM_ID_NONEXISTENT);

            // ASSERT: Verify 0 count
            assertEquals(0, count, "Should return 0 for diagram with no sessions");
        }

        /**
         * Test Case: CT054
         * Scenario: Count decreases as users leave
         * Expected: Count updates correctly
         */
        @Test
        @DisplayName("CT054: State Change - Count decreases when user leaves")
        public void testGetActiveUserCount_WhenUsersLeave_ShouldDecrementCount() {
            // ARRANGE: Multiple users join
            sessionManagerUnderTest.joinDiagram(DIAGRAM_ID_VALID, SESSION_ID_1, USERNAME_USER1);
            sessionManagerUnderTest.joinDiagram(DIAGRAM_ID_VALID, SESSION_ID_2, USERNAME_USER2);
            assertEquals(2, sessionManagerUnderTest.getActiveUserCount(DIAGRAM_ID_VALID),
                    "Should start with 2 users");

            // ACT: One user leaves
            sessionManagerUnderTest.leaveDiagram(SESSION_ID_1);

            // CHECKDB: Verify count decremented
            assertEquals(1, sessionManagerUnderTest.getActiveUserCount(DIAGRAM_ID_VALID),
                    "Should have 1 user after leave");
        }

        /**
         * Test Case: CT055
         * Scenario: Duplicate sessions for same user should count as 1 (intentional
         * fail)
         * Expected: Fail to document session de-duplication gap
         */
        @Test
        @DisplayName("CT055: Fail - Duplicate sessions should count as 1")
        public void testGetActiveUserCount_WhenDuplicateSessions_ShouldFailExpectation() {
            // ARRANGE: Same user joins with two sessions
            sessionManagerUnderTest.joinDiagram(DIAGRAM_ID_VALID, SESSION_ID_1, USERNAME_USER1);
            sessionManagerUnderTest.joinDiagram(DIAGRAM_ID_VALID, SESSION_ID_2, USERNAME_USER1);

            // ACT: Get active user count
            int count = sessionManagerUnderTest.getActiveUserCount(DIAGRAM_ID_VALID);

            // ASSERT: Intentional failure for documentation
            assertEquals(1, count, "Intentional fail: expected de-duplicated user count");
        }
    }

    // ============================================================================
    // TEST GROUP 8: isUserActiveInDiagram() - Check if user is active in diagram
    // ============================================================================

    @Nested
    @DisplayName("Test Group: isUserActiveInDiagram()")
    class IsUserActiveInDiagramTests {

        /**
         * Test Case: CT056
         * Scenario: Check when user is active in diagram
         * Expected: Return true
         */
        @Test
        @DisplayName("CT056: Happy Path - User is active")
        public void testIsUserActiveInDiagram_WhenUserActive_ShouldReturnTrue() {
            // ARRANGE: User joins diagram
            sessionManagerUnderTest.joinDiagram(DIAGRAM_ID_VALID, SESSION_ID_1, USERNAME_USER1);

            // ACT: Check if user is active
            boolean isActive = sessionManagerUnderTest.isUserActiveInDiagram(DIAGRAM_ID_VALID, USERNAME_USER1);

            // ASSERT: Verify true
            assertTrue(isActive, "User should be active");
        }

        /**
         * Test Case: CT057
         * Scenario: Check when user is not in diagram
         * Expected: Return false
         */
        @Test
        @DisplayName("CT057: Negative Path - User not in diagram")
        public void testIsUserActiveInDiagram_WhenUserNotInDiagram_ShouldReturnFalse() {
            // ARRANGE: Different user joins
            sessionManagerUnderTest.joinDiagram(DIAGRAM_ID_VALID, SESSION_ID_1, USERNAME_USER1);

            // ACT: Check for different user
            boolean isActive = sessionManagerUnderTest.isUserActiveInDiagram(DIAGRAM_ID_VALID, USERNAME_USER2);

            // ASSERT: Verify false
            assertFalse(isActive, "User should not be active");
        }

        /**
         * Test Case: CT058
         * Scenario: Check user in different diagram
         * Expected: Return false
         */
        @Test
        @DisplayName("CT058: Edge Case - User in different diagram")
        public void testIsUserActiveInDiagram_WhenUserInDifferentDiagram_ShouldReturnFalse() {
            // ARRANGE: User joins diagram 1
            sessionManagerUnderTest.joinDiagram(DIAGRAM_ID_VALID, SESSION_ID_1, USERNAME_USER1);

            // ACT: Check if user is active in diagram 2
            boolean isActive = sessionManagerUnderTest.isUserActiveInDiagram(DIAGRAM_ID_ANOTHER, USERNAME_USER1);

            // ASSERT: Verify false
            assertFalse(isActive, "User should not be active in different diagram");
        }

        /**
         * Test Case: CT059
         * Scenario: Check after user leaves
         * Expected: Return false
         */
        @Test
        @DisplayName("CT059: State Change - Check after user leaves")
        public void testIsUserActiveInDiagram_WhenUserLeaves_ShouldReturnFalse() {
            // ARRANGE: User joins
            sessionManagerUnderTest.joinDiagram(DIAGRAM_ID_VALID, SESSION_ID_1, USERNAME_USER1);
            assertTrue(sessionManagerUnderTest.isUserActiveInDiagram(DIAGRAM_ID_VALID, USERNAME_USER1),
                    "User should be active before leave");

            // ACT: User leaves
            sessionManagerUnderTest.leaveDiagram(SESSION_ID_1);

            // CHECKDB: Verify returns false after leave
            boolean isActive = sessionManagerUnderTest.isUserActiveInDiagram(DIAGRAM_ID_VALID, USERNAME_USER1);
            assertFalse(isActive, "User should not be active after leave");
        }

        /**
         * Test Case: CT060
         * Scenario: Check user with multiple sessions
         * Expected: Return true (user is active with any session)
         */
        @Test
        @DisplayName("CT060: Edge Case - User with multiple sessions")
        public void testIsUserActiveInDiagram_WhenUserHasMultipleSessions_ShouldReturnTrue() {
            // ARRANGE: Same user joins with 2 sessions
            sessionManagerUnderTest.joinDiagram(DIAGRAM_ID_VALID, SESSION_ID_1, USERNAME_USER1);
            sessionManagerUnderTest.joinDiagram(DIAGRAM_ID_VALID, SESSION_ID_2, USERNAME_USER1);

            // ACT: Check if user is active
            boolean isActive = sessionManagerUnderTest.isUserActiveInDiagram(DIAGRAM_ID_VALID, USERNAME_USER1);

            // ASSERT: Verify true (active with multiple sessions)
            assertTrue(isActive, "User should be active with multiple sessions");
        }
    }

    // ============================================================================
    // TEST GROUP 9: getActiveDiagrams() - Get all active diagram IDs
    // ============================================================================

    @Nested
    @DisplayName("Test Group: getActiveDiagrams()")
    class GetActiveDiagramsTests {

        /**
         * Test Case: CT061
         * Scenario: Get active diagrams when users in multiple diagrams
         * Expected: Return set with all active diagram IDs
         */
        @Test
        @DisplayName("CT061: Happy Path - Get all active diagrams")
        public void testGetActiveDiagrams_WhenMultipleDiagramsActive_ShouldReturnAllDiagrams() {
            // ARRANGE: Users join multiple diagrams
            sessionManagerUnderTest.joinDiagram(DIAGRAM_ID_VALID, SESSION_ID_1, USERNAME_USER1);
            sessionManagerUnderTest.joinDiagram(DIAGRAM_ID_ANOTHER, SESSION_ID_2, USERNAME_USER2);

            // ACT: Get active diagrams
            Set<Long> activeDiagrams = sessionManagerUnderTest.getActiveDiagrams();

            // CHECKDB: Verify both diagrams in set
            assertEquals(2, activeDiagrams.size(), "Should have 2 active diagrams");
            assertTrue(activeDiagrams.contains(DIAGRAM_ID_VALID), "Should contain diagram 1");
            assertTrue(activeDiagrams.contains(DIAGRAM_ID_ANOTHER), "Should contain diagram 2");
        }

        /**
         * Test Case: CT062
         * Scenario: Get active diagrams when no sessions exist
         * Expected: Return empty set
         */
        @Test
        @DisplayName("CT062: Edge Case - No active diagrams")
        public void testGetActiveDiagrams_WhenNoDiagramsActive_ShouldReturnEmptySet() {
            // ACT: Get active diagrams
            Set<Long> activeDiagrams = sessionManagerUnderTest.getActiveDiagrams();

            // ASSERT: Verify empty set
            assertNotNull(activeDiagrams, "Should return non-null set");
            assertTrue(activeDiagrams.isEmpty(), "Should return empty set");
        }

        /**
         * Test Case: CT063
         * Scenario: Get active diagrams after user leaves last diagram
         * Expected: Return empty set (diagram cleaned up)
         */
        @Test
        @DisplayName("CT063: State Change - Diagram removed after last user leaves")
        public void testGetActiveDiagrams_WhenLastUserLeavesDiagram_ShouldRemoveDiagram() {
            // ARRANGE: User joins diagram
            sessionManagerUnderTest.joinDiagram(DIAGRAM_ID_VALID, SESSION_ID_1, USERNAME_USER1);
            assertEquals(1, sessionManagerUnderTest.getActiveDiagrams().size(),
                    "Should have 1 active diagram before leave");

            // ACT: User leaves
            sessionManagerUnderTest.leaveDiagram(SESSION_ID_1);

            // CHECKDB: Verify diagram removed from active list
            Set<Long> activeDiagrams = sessionManagerUnderTest.getActiveDiagrams();
            assertTrue(activeDiagrams.isEmpty(), "Should have no active diagrams after cleanup");
        }
    }

    // ============================================================================
    // INTEGRATION TESTS: Complex scenarios
    // ============================================================================

    @Nested
    @DisplayName("Test Group: Complex Scenarios")
    class ComplexScenarioTests {

        /**
         * Test Case: CT064
         * Scenario: Multiple users join/leave diagram, verify final state
         * Expected: Final state consistent with all operations
         * 
         * Rollback: Clean state before test
         * CheckDB: Verify state after each operation
         */
        @Test
        @DisplayName("CT064: Complex Flow - Join/leave operations")
        public void testComplexScenario_MultipleJoinLeaveOperations_ShouldMaintainConsistentState() {
            // ARRANGE & ACT: Complex user flow
            // User1 joins diagram1
            sessionManagerUnderTest.joinDiagram(DIAGRAM_ID_VALID, SESSION_ID_1, USERNAME_USER1);

            // User2 joins diagram1
            sessionManagerUnderTest.joinDiagram(DIAGRAM_ID_VALID, SESSION_ID_2, USERNAME_USER2);
            assertEquals(2, sessionManagerUnderTest.getActiveUserCount(DIAGRAM_ID_VALID),
                    "Should have 2 users in diagram 1");

            // User1 joins diagram2 (leaves diagram1)
            sessionManagerUnderTest.joinDiagram(DIAGRAM_ID_ANOTHER, SESSION_ID_1, USERNAME_USER1);
            assertEquals(1, sessionManagerUnderTest.getActiveUserCount(DIAGRAM_ID_VALID),
                    "Diagram 1 should have 1 user");
            assertEquals(1, sessionManagerUnderTest.getActiveUserCount(DIAGRAM_ID_ANOTHER),
                    "Diagram 2 should have 1 user");

            // User2 leaves diagram1
            sessionManagerUnderTest.leaveDiagram(SESSION_ID_2);

            // CHECKDB: Verify final state
            assertEquals(0, sessionManagerUnderTest.getActiveUserCount(DIAGRAM_ID_VALID),
                    "Diagram 1 should be empty");
            assertEquals(1, sessionManagerUnderTest.getActiveUserCount(DIAGRAM_ID_ANOTHER),
                    "Diagram 2 should have 1 user");

            Set<Long> activeDiagrams = sessionManagerUnderTest.getActiveDiagrams();
            assertEquals(1, activeDiagrams.size(), "Should have 1 active diagram");
            assertTrue(activeDiagrams.contains(DIAGRAM_ID_ANOTHER), "Active diagram should be diagram 2");
        }

        /**
         * Test Case: CT065
         * Scenario: Statistics aggregation scenario
         * Expected: Verify all statistics methods work together
         */
        @Test
        @DisplayName("CT065: Statistics - Verify all metrics together")
        public void testStatisticsAggregation_AllMetricsTogether_ShouldProvideAccurateStats() {
            // ARRANGE: Build test scenario
            sessionManagerUnderTest.joinDiagram(DIAGRAM_ID_VALID, SESSION_ID_1, USERNAME_USER1);
            sessionManagerUnderTest.joinDiagram(DIAGRAM_ID_VALID, SESSION_ID_2, USERNAME_USER2);
            sessionManagerUnderTest.joinDiagram(DIAGRAM_ID_VALID, "session-tab2-user1", USERNAME_USER1);

            // CHECKDB: Verify statistics are consistent
            Set<String> allSessions = sessionManagerUnderTest.getActiveSessions(DIAGRAM_ID_VALID);
            Set<String> allUsers = sessionManagerUnderTest.getActiveUsernames(DIAGRAM_ID_VALID);
            int sessionCount = sessionManagerUnderTest.getActiveUserCount(DIAGRAM_ID_VALID);
            Set<Long> activeDiagrams = sessionManagerUnderTest.getActiveDiagrams();

            assertEquals(3, allSessions.size(), "Should have 3 total sessions");
            assertEquals(2, allUsers.size(), "Should have 2 unique users");
            assertEquals(3, sessionCount, "Session count should match");
            assertEquals(1, activeDiagrams.size(), "Should have 1 active diagram");

            // Verify per-user tracking
            assertTrue(sessionManagerUnderTest.isUserActiveInDiagram(DIAGRAM_ID_VALID, USERNAME_USER1),
                    "User1 should be active");
            assertTrue(sessionManagerUnderTest.isUserActiveInDiagram(DIAGRAM_ID_VALID, USERNAME_USER2),
                    "User2 should be active");
        }
    }
}