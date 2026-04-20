/**
 * ============================================================================
 * UNIT TEST CLASS: DiagramListControllerTest
 * ============================================================================
 * Purpose: Unit tests for DiagramListController (diagram count)
 * - Focus: /api/diagrams/list returns totalCount
 * - Uses MockMvc with mocked services
 * 
 * Author: QA Team
 * Date: 19/4/2026
 * Version: 1.0
 * ============================================================================
 */

package com.example.react_flow_be.controller;

import com.example.react_flow_be.dto.DiagramListResponseDto;
import com.example.react_flow_be.service.DatabaseDiagramListService;
import com.example.react_flow_be.service.DiagramManagementService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.web.servlet.MockMvc;

import java.util.Collections;

import static org.hamcrest.Matchers.is;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(DiagramListController.class)
@ContextConfiguration(classes = DiagramListController.class)
@AutoConfigureMockMvc(addFilters = false)
@DisplayName("DiagramListController - Unit Tests")
class DiagramListControllerTest {

        @Autowired
        private MockMvc mockMvc;

        @MockBean
        private DatabaseDiagramListService databaseDiagramListService;

        @MockBean
        private DiagramManagementService diagramManagementService;

        private static final String USERNAME = "user1";

        /**
         * Test Case: TK025
         * Scenario: Missing X-Username header
         * Expected: 401 Unauthorized
         */
        @Test
        @DisplayName("TK025: Missing username header returns 401")
        void testGetDiagramList_WhenMissingUsername_ShouldReturnUnauthorized() throws Exception {
                mockMvc.perform(get("/api/diagrams/list"))
                                .andExpect(status().isUnauthorized());
        }

        /**
         * Test Case: TK026
         * Scenario: Wrong header name but expecting OK (intentional fail)
         * Expected: Fail to document header mismatch
         */
        @Test
        @DisplayName("TK026: Fail - Expect 200 with wrong header name")
        void testGetDiagramList_WhenWrongHeaderName_ShouldFailExpectation() throws Exception {
                mockMvc.perform(get("/api/diagrams/list")
                                .header("X-UserName", USERNAME))
                                .andExpect(status().isOk());
        }

        /**
         * Test Case: TK027
         * Scenario: Valid request returns totalCount
         * Expected: 200 OK with totalCount
         */
        @Test
        @DisplayName("TK027: /api/diagrams/list returns totalCount")
        void testGetDiagramList_ShouldReturnTotalCount() throws Exception {
                DiagramListResponseDto response = new DiagramListResponseDto(
                                Collections.emptyList(),
                                null,
                                false,
                                7);

                when(databaseDiagramListService.getDatabaseDiagramList(any(), eq(USERNAME)))
                                .thenReturn(response);

                mockMvc.perform(get("/api/diagrams/list")
                                .header("X-Username", USERNAME)
                                .param("pageSize", "1"))
                                .andExpect(status().isOk())
                                .andExpect(jsonPath("$.totalCount", is(7)));

                verify(databaseDiagramListService, times(1)).getDatabaseDiagramList(any(), eq(USERNAME));
        }

        /**
         * Test Case: TK028
         * Scenario: Service throws exception
         * Expected: 500 Internal Server Error
         */
        @Test
        @DisplayName("TK028: /api/diagrams/list returns 500 on error")
        void testGetDiagramList_WhenServiceThrows_ShouldReturnServerError() throws Exception {
                when(databaseDiagramListService.getDatabaseDiagramList(any(), eq(USERNAME)))
                                .thenThrow(new RuntimeException("error"));

                mockMvc.perform(get("/api/diagrams/list")
                                .header("X-Username", USERNAME))
                                .andExpect(status().isInternalServerError());
        }

        /**
         * Test Case: TK029
         * Scenario: Missing X-Username header for /all
         * Expected: 401 Unauthorized
         */
        @Test
        @DisplayName("TK029: /api/diagrams/all missing header returns 401")
        void testGetDiagramAll_WhenMissingUsername_ShouldReturnUnauthorized() throws Exception {
                mockMvc.perform(get("/api/diagrams/all"))
                                .andExpect(status().isUnauthorized());
        }

        /**
         * Test Case: TK030
         * Scenario: /all returns totalCount
         * Expected: 200 OK with totalCount
         */
        @Test
        @DisplayName("TK030: /api/diagrams/all returns totalCount")
        void testGetDiagramAll_ShouldReturnTotalCount() throws Exception {
                DiagramListResponseDto response = new DiagramListResponseDto(
                                Collections.emptyList(),
                                null,
                                false,
                                9);

                when(databaseDiagramListService.getDatabaseDiagramAll(any(), eq(USERNAME)))
                                .thenReturn(response);

                mockMvc.perform(get("/api/diagrams/all")
                                .header("X-Username", USERNAME)
                                .param("pageSize", "1"))
                                .andExpect(status().isOk())
                                .andExpect(jsonPath("$.totalCount", is(9)));

                verify(databaseDiagramListService, times(1)).getDatabaseDiagramAll(any(), eq(USERNAME));
        }

        /**
         * Test Case: TK031
         * Scenario: /all service throws exception
         * Expected: 500 Internal Server Error
         */
        @Test
        @DisplayName("TK031: /api/diagrams/all returns 500 on error")
        void testGetDiagramAll_WhenServiceThrows_ShouldReturnServerError() throws Exception {
                when(databaseDiagramListService.getDatabaseDiagramAll(any(), eq(USERNAME)))
                                .thenThrow(new RuntimeException("error"));

                mockMvc.perform(get("/api/diagrams/all")
                                .header("X-Username", USERNAME))
                                .andExpect(status().isInternalServerError());
        }
}