/**
 * ============================================================================
 * UNIT TEST CLASS: DiagramCollaborationControllerTest
 * ============================================================================
 * Purpose: Unit tests for DiagramCollaborationController (collaboration + stats)
 * - Covers all controller endpoints for collaboration and online users
 * - Uses MockMvc with mocked services (unit test only)
 * - Includes Test Case IDs and meaningful names
 * 
 * Test Scope:
 * - Controller: DiagramCollaborationController
 * - Use cases: Collaboration + Statistics (online users)
 * 
 
 * Version: 1.0
 * ============================================================================
 */

package com.example.react_flow_be.controller;

import com.example.react_flow_be.config.DiagramSessionManager;
import com.example.react_flow_be.dto.collaboration.AddCollaboratorRequest;
import com.example.react_flow_be.dto.collaboration.CollaborationDTO;
import com.example.react_flow_be.dto.collaboration.UpdatePermissionRequest;
import com.example.react_flow_be.entity.Collaboration;
import com.example.react_flow_be.service.CollaborationService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.context.ContextConfiguration;

import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.hamcrest.Matchers.hasSize;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Test class for DiagramCollaborationController
 * 
 * CHECKDB STRATEGY:
 * - verify() ensures service methods are invoked with correct parameters
 * 
 * ROLLBACK STRATEGY:
 * - No shared state, all dependencies mocked
 */
@WebMvcTest(DiagramCollaborationController.class)
@ContextConfiguration(classes = DiagramCollaborationController.class)
@AutoConfigureMockMvc(addFilters = false)
@DisplayName("DiagramCollaborationController - Unit Tests")
class DiagramCollaborationControllerTest {

        @Autowired
        private MockMvc mockMvc;

        @Autowired
        private ObjectMapper objectMapper;

        @MockBean
        private CollaborationService collaborationService;

        @MockBean
        private DiagramSessionManager sessionManager;

        private static final Long DIAGRAM_ID_VALID = 1L;
        private static final Long COLLABORATION_ID_VALID = 10L;

        private static final String USERNAME_OWNER = "owner_user";
        private static final String USERNAME_COLLABORATOR = "collaborator_user";

        /**
         * Test Case: CT066
         * Scenario: Get collaborations for diagram
         * Expected: 200 OK with list of collaborations
         */
        @Test
        @DisplayName("CT066: Get collaborations returns list")
        void testGetCollaborations_ShouldReturnList() throws Exception {
                CollaborationDTO owner = new CollaborationDTO(
                                1L,
                                USERNAME_OWNER,
                                Collaboration.CollaborationType.OWNER,
                                Collaboration.Permission.FULL_ACCESS,
                                true,
                                null,
                                null);
                CollaborationDTO participant = new CollaborationDTO(
                                2L,
                                USERNAME_COLLABORATOR,
                                Collaboration.CollaborationType.PARTICIPANTS,
                                Collaboration.Permission.VIEW,
                                true,
                                null,
                                null);
                List<CollaborationDTO> results = Arrays.asList(owner, participant);

                when(collaborationService.getCollaborations(DIAGRAM_ID_VALID)).thenReturn(results);

                mockMvc.perform(get("/api/diagram/{diagramId}/collaborations", DIAGRAM_ID_VALID))
                                .andExpect(status().isOk())
                                .andExpect(jsonPath("$", hasSize(2)))
                                .andExpect(jsonPath("$[0].username").value(USERNAME_OWNER))
                                .andExpect(jsonPath("$[1].username").value(USERNAME_COLLABORATOR));

                verify(collaborationService, times(1)).getCollaborations(DIAGRAM_ID_VALID);
        }

        /**
         * Test Case: CT067
         * Scenario: Inactive collaborator should be filtered out (intentional fail)
         * Expected: Fail to document inactive filtering gap
         */
        @Test
        @DisplayName("CT067: Fail - Inactive collaborator should be excluded")
        void testGetCollaborations_ShouldFailOnWrongSize() throws Exception {
                CollaborationDTO owner = new CollaborationDTO(
                                1L,
                                USERNAME_OWNER,
                                Collaboration.CollaborationType.OWNER,
                                Collaboration.Permission.FULL_ACCESS,
                                true,
                                null,
                                null);
                CollaborationDTO inactiveParticipant = new CollaborationDTO(
                                2L,
                                USERNAME_COLLABORATOR,
                                Collaboration.CollaborationType.PARTICIPANTS,
                                Collaboration.Permission.VIEW,
                                false,
                                null,
                                null);
                List<CollaborationDTO> results = Arrays.asList(owner, inactiveParticipant);

                when(collaborationService.getCollaborations(DIAGRAM_ID_VALID)).thenReturn(results);

                mockMvc.perform(get("/api/diagram/{diagramId}/collaborations", DIAGRAM_ID_VALID))
                                .andExpect(status().isOk())
                                .andExpect(jsonPath("$", hasSize(1)));
        }

        /**
         * Test Case: CT068
         * Scenario: Add collaborator
         * Expected: 200 OK with created collaborator DTO
         */
        @Test
        @DisplayName("CT068: Add collaborator returns DTO")
        void testAddCollaborator_ShouldReturnDto() throws Exception {
                AddCollaboratorRequest request = new AddCollaboratorRequest();
                request.setUsername(USERNAME_COLLABORATOR);
                request.setPermission(Collaboration.Permission.VIEW);

                CollaborationDTO response = new CollaborationDTO(
                                COLLABORATION_ID_VALID,
                                USERNAME_COLLABORATOR,
                                Collaboration.CollaborationType.PARTICIPANTS,
                                Collaboration.Permission.VIEW,
                                true,
                                null,
                                null);

                when(collaborationService.addCollaborator(
                                eq(DIAGRAM_ID_VALID),
                                eq(USERNAME_COLLABORATOR),
                                eq(Collaboration.Permission.VIEW)))
                                .thenReturn(response);

                mockMvc.perform(post("/api/diagram/{diagramId}/collaborations", DIAGRAM_ID_VALID)
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(request)))
                                .andExpect(status().isOk())
                                .andExpect(jsonPath("$.id").value(COLLABORATION_ID_VALID))
                                .andExpect(jsonPath("$.username").value(USERNAME_COLLABORATOR))
                                .andExpect(jsonPath("$.permission").value("VIEW"));

                verify(collaborationService, times(1)).addCollaborator(
                                DIAGRAM_ID_VALID,
                                USERNAME_COLLABORATOR,
                                Collaboration.Permission.VIEW);
        }

        /**
         * Test Case: CT069
         * Scenario: Update permission
         * Expected: 200 OK
         */
        @Test
        @DisplayName("CT069: Update permission returns OK")
        void testUpdatePermission_ShouldReturnOk() throws Exception {
                UpdatePermissionRequest request = new UpdatePermissionRequest();
                request.setPermission(Collaboration.Permission.EDIT);

                mockMvc.perform(patch("/api/diagram/{diagramId}/collaborations/{collaborationId}",
                                DIAGRAM_ID_VALID, COLLABORATION_ID_VALID)
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(request)))
                                .andExpect(status().isOk());

                verify(collaborationService, times(1))
                                .updatePermission(COLLABORATION_ID_VALID, Collaboration.Permission.EDIT);
        }

        /**
         * Test Case: CT070
         * Scenario: Remove collaborator
         * Expected: 200 OK
         */
        @Test
        @DisplayName("CT070: Remove collaborator returns OK")
        void testRemoveCollaborator_ShouldReturnOk() throws Exception {
                mockMvc.perform(delete("/api/diagram/{diagramId}/collaborations/{collaborationId}",
                                DIAGRAM_ID_VALID, COLLABORATION_ID_VALID))
                                .andExpect(status().isOk());

                verify(collaborationService, times(1)).removeCollaborator(COLLABORATION_ID_VALID);
        }

        /**
         * Test Case: CT071
         * Scenario: Check access
         * Expected: 200 OK with boolean
         */
        @Test
        @DisplayName("CT071: Check access returns boolean")
        void testCheckAccess_ShouldReturnBoolean() throws Exception {
                when(collaborationService.hasAccess(DIAGRAM_ID_VALID, USERNAME_COLLABORATOR)).thenReturn(true);

                mockMvc.perform(get("/api/diagram/{diagramId}/collaborations/check-access", DIAGRAM_ID_VALID)
                                .param("username", USERNAME_COLLABORATOR))
                                .andExpect(status().isOk())
                                .andExpect(jsonPath("$").value(true));

                verify(collaborationService, times(1))
                                .hasAccess(DIAGRAM_ID_VALID, USERNAME_COLLABORATOR);
        }

        /**
         * Test Case: CT072
         * Scenario: Get owner
         * Expected: 200 OK with owner DTO
         */
        @Test
        @DisplayName("CT072: Get owner returns DTO")
        void testGetOwner_ShouldReturnDto() throws Exception {
                CollaborationDTO owner = new CollaborationDTO(
                                1L,
                                USERNAME_OWNER,
                                Collaboration.CollaborationType.OWNER,
                                Collaboration.Permission.FULL_ACCESS,
                                true,
                                null,
                                null);

                when(collaborationService.getOwner(DIAGRAM_ID_VALID)).thenReturn(owner);

                mockMvc.perform(get("/api/diagram/{diagramId}/collaborations/owner", DIAGRAM_ID_VALID))
                                .andExpect(status().isOk())
                                .andExpect(jsonPath("$.username").value(USERNAME_OWNER))
                                .andExpect(jsonPath("$.permission").value("FULL_ACCESS"));

                verify(collaborationService, times(1)).getOwner(DIAGRAM_ID_VALID);
        }

        /**
         * Test Case: CT073
         * Scenario: Get online users
         * Expected: 200 OK with list of usernames
         */
        @Test
        @DisplayName("CT073: Get online users returns set")
        void testGetOnlineUsers_ShouldReturnSet() throws Exception {
                Set<String> onlineUsers = new HashSet<>(Arrays.asList("user_a", "user_b"));
                when(sessionManager.getActiveUsernames(DIAGRAM_ID_VALID)).thenReturn(onlineUsers);

                mockMvc.perform(get("/api/diagram/{diagramId}/online-users", DIAGRAM_ID_VALID))
                                .andExpect(status().isOk())
                                .andExpect(jsonPath("$", hasSize(2)))
                                .andExpect(jsonPath("$", containsInAnyOrder("user_a", "user_b")));

                verify(sessionManager, times(1)).getActiveUsernames(DIAGRAM_ID_VALID);
        }
}