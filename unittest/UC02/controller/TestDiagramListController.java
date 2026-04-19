package com.example.react_flow_be.controller;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import org.mockito.Mock;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import com.example.react_flow_be.dto.DiagramListRequestDto;
import com.example.react_flow_be.dto.DiagramListResponseDto;
import com.example.react_flow_be.service.DatabaseDiagramListService;
import com.example.react_flow_be.service.DiagramManagementService;

import jakarta.servlet.http.HttpServletRequest;

/**
 * TestDiagramListController: Unit Test cho DiagramListController (Diagram List Management)
 * 
 * Mục tiêu: Đảm bảo API diagram list hoạt động chính xác với các filter và pagination
 * 
 * Coverage Level 2 (Branch Coverage):
 * - getDiagramList: lấy danh sách thành công, không có username, lỗi service
 * - getDiagramAll: lấy tất cả diagrams (admin), không có username, lỗi service
 * - deleteDiagram: xóa mềm, không có username, lỗi service
 * - restoreDiagram: khôi phục diagram, không có username, lỗi service
 * - permanentlyDeleteDiagram: xóa vĩnh viễn, không có username, lỗi service
 * - isOwner: kiểm tra owner, không có username, lỗi service
 * - getTrashCount: đếm diagram trong trash, không có username, lỗi service
 */
@DisplayName("TestDiagramListController - Diagram List Management (UC02)")
@ExtendWith(MockitoExtension.class)
public class TestDiagramListController {

    private DiagramListController diagramListController;

    @Mock
    private DatabaseDiagramListService databaseDiagramListService;

    @Mock
    private DiagramManagementService diagramManagementService;

    @Mock
    private HttpServletRequest httpServletRequest;

    private DiagramListResponseDto mockResponse;
    private final Long diagramId = 1L;
    private final String username = "test_user";
    private final String ownerUsername = "owner_user";

    @BeforeEach
    public void setUp() {
        diagramListController = new DiagramListController(
                databaseDiagramListService,
                diagramManagementService
        );

        // Create mock response
        mockResponse = new DiagramListResponseDto();
        mockResponse.setTotalCount(10);
        mockResponse.setHasMore(false);
    }

    // ==================== Test getDiagramList ====================

    @Test
    @DisplayName("getDiagramList_testChuan1 - Get diagram list successfully")
    public void getDiagramList_testChuan1() {
        // Standard case: successful retrieval with all filters
        when(httpServletRequest.getHeader("X-Username")).thenReturn(username);
        when(databaseDiagramListService.getDatabaseDiagramList(any(DiagramListRequestDto.class), eq(username)))
                .thenReturn(mockResponse);

        ResponseEntity<DiagramListResponseDto> response = diagramListController.getDiagramList(
                null, 20, null, null, null, "alltime", false, false, "updatedAt", "DESC",
                httpServletRequest);

        assertNotNull(response);
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertNotNull(response.getBody());
        assertEquals(10, response.getBody().getTotalCount());
        verify(databaseDiagramListService).getDatabaseDiagramList(any(DiagramListRequestDto.class), eq(username));
    }

    @Test
    @DisplayName("getDiagramList_testChuan2 - Get diagram list with pagination cursor")
    public void getDiagramList_testChuan2() {
        // Standard case: with cursor pagination
        when(httpServletRequest.getHeader("X-Username")).thenReturn(username);
        when(databaseDiagramListService.getDatabaseDiagramList(any(DiagramListRequestDto.class), eq(username)))
                .thenReturn(mockResponse);

        ResponseEntity<DiagramListResponseDto> response = diagramListController.getDiagramList(
                100L, 10, null, null, null, "alltime", false, false, "createdAt", "ASC",
                httpServletRequest);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        verify(databaseDiagramListService).getDatabaseDiagramList(any(DiagramListRequestDto.class), eq(username));
    }

    @Test
    @DisplayName("getDiagramList_testChuan3 - Get diagram list with search filters")
    public void getDiagramList_testChuan3() {
        // Standard case: with search and filters
        when(httpServletRequest.getHeader("X-Username")).thenReturn(username);
        when(databaseDiagramListService.getDatabaseDiagramList(any(DiagramListRequestDto.class), eq(username)))
                .thenReturn(mockResponse);

        ResponseEntity<DiagramListResponseDto> response = diagramListController.getDiagramList(
                null, 20, "a", "search_term", "me", "last7days", false, false, "name", "ASC",
                httpServletRequest);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        verify(databaseDiagramListService).getDatabaseDiagramList(any(DiagramListRequestDto.class), eq(username));
    }

    @Test
    @DisplayName("getDiagramList_ngoaile1 - No username in header returns UNAUTHORIZED")
    public void getDiagramList_ngoaile1() {
        // Error case: missing X-Username header
        when(httpServletRequest.getHeader("X-Username")).thenReturn(null);

        ResponseEntity<DiagramListResponseDto> response = diagramListController.getDiagramList(
                null, 20, null, null, null, "alltime", false, false, "updatedAt", "DESC",
                httpServletRequest);

        assertEquals(HttpStatus.UNAUTHORIZED, response.getStatusCode());
        verify(databaseDiagramListService, never()).getDatabaseDiagramList(any(), any());
    }

    @Test
    @DisplayName("getDiagramList_ngoaile2 - Empty username header returns UNAUTHORIZED")
    public void getDiagramList_ngoaile2() {
        // Error case: empty X-Username header
        when(httpServletRequest.getHeader("X-Username")).thenReturn("");

        ResponseEntity<DiagramListResponseDto> response = diagramListController.getDiagramList(
                null, 20, null, null, null, "alltime", false, false, "updatedAt", "DESC",
                httpServletRequest);

        assertEquals(HttpStatus.UNAUTHORIZED, response.getStatusCode());
        verify(databaseDiagramListService, never()).getDatabaseDiagramList(any(), any());
    }

    @Test
    @DisplayName("getDiagramList_ngoaile3 - Service throws exception returns INTERNAL_SERVER_ERROR")
    public void getDiagramList_ngoaile3() {
        // Error case: service exception
        when(httpServletRequest.getHeader("X-Username")).thenReturn(username);
        when(databaseDiagramListService.getDatabaseDiagramList(any(DiagramListRequestDto.class), eq(username)))
                .thenThrow(new RuntimeException("Database error"));

        ResponseEntity<DiagramListResponseDto> response = diagramListController.getDiagramList(
                null, 20, null, null, null, "alltime", false, false, "updatedAt", "DESC",
                httpServletRequest);

        assertEquals(HttpStatus.INTERNAL_SERVER_ERROR, response.getStatusCode());
    }

    // ==================== Test getDiagramAll ====================

    @Test
    @DisplayName("getDiagramAll_testChuan1 - Get all diagrams (admin)")
    public void getDiagramAll_testChuan1() {
        // Standard case: admin retrieves all diagrams
        when(httpServletRequest.getHeader("X-Username")).thenReturn(username);
        when(databaseDiagramListService.getDatabaseDiagramAll(any(DiagramListRequestDto.class), eq(username)))
                .thenReturn(mockResponse);

        ResponseEntity<DiagramListResponseDto> response = diagramListController.getDiagramAll(
                null, 20, null, null, null, "alltime", false, false, "updatedAt", "DESC",
                httpServletRequest);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        verify(databaseDiagramListService).getDatabaseDiagramAll(any(DiagramListRequestDto.class), eq(username));
    }

    @Test
    @DisplayName("getDiagramAll_ngoaile1 - No username in header returns UNAUTHORIZED")
    public void getDiagramAll_ngoaile1() {
        // Error case: missing username
        when(httpServletRequest.getHeader("X-Username")).thenReturn(null);

        ResponseEntity<DiagramListResponseDto> response = diagramListController.getDiagramAll(
                null, 20, null, null, null, "alltime", false, false, "updatedAt", "DESC",
                httpServletRequest);

        assertEquals(HttpStatus.UNAUTHORIZED, response.getStatusCode());
    }

    // ==================== Test deleteDiagram ====================

    @Test
    @DisplayName("deleteDiagram_testChuan1 - Soft delete diagram successfully")
    public void deleteDiagram_testChuan1() {
        // Standard case: successful soft delete
        when(httpServletRequest.getHeader("X-Username")).thenReturn(ownerUsername);
        doNothing().when(diagramManagementService).softDeleteDiagram(diagramId, ownerUsername);

        ResponseEntity<?> response = diagramListController.deleteDiagram(diagramId, httpServletRequest);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        verify(diagramManagementService).softDeleteDiagram(diagramId, ownerUsername);
    }

    @Test
    @DisplayName("deleteDiagram_ngoaile1 - Delete without username returns UNAUTHORIZED")
    public void deleteDiagram_ngoaile1() {
        // Error case: missing username
        when(httpServletRequest.getHeader("X-Username")).thenReturn(null);

        ResponseEntity<?> response = diagramListController.deleteDiagram(diagramId, httpServletRequest);

        assertEquals(HttpStatus.UNAUTHORIZED, response.getStatusCode());
        verify(diagramManagementService, never()).softDeleteDiagram(any(), any());
    }

    @Test
    @DisplayName("deleteDiagram_ngoaile2 - Service throws exception returns INTERNAL_SERVER_ERROR")
    public void deleteDiagram_ngoaile2() {
        // Error case: service exception
        when(httpServletRequest.getHeader("X-Username")).thenReturn(ownerUsername);
        doThrow(new RuntimeException("Delete failed")).when(diagramManagementService)
                .softDeleteDiagram(diagramId, ownerUsername);

        ResponseEntity<?> response = diagramListController.deleteDiagram(diagramId, httpServletRequest);

        assertEquals(HttpStatus.INTERNAL_SERVER_ERROR, response.getStatusCode());
    }

    // ==================== Test restoreDiagram ====================

    @Test
    @DisplayName("restoreDiagram_testChuan1 - Restore diagram successfully")
    public void restoreDiagram_testChuan1() {
        // Standard case: successful restore
        when(httpServletRequest.getHeader("X-Username")).thenReturn(ownerUsername);
        doNothing().when(diagramManagementService).restoreDiagram(diagramId, ownerUsername);

        ResponseEntity<?> response = diagramListController.restoreDiagram(diagramId, httpServletRequest);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        verify(diagramManagementService).restoreDiagram(diagramId, ownerUsername);
    }

    @Test
    @DisplayName("restoreDiagram_ngoaile1 - Restore without username returns UNAUTHORIZED")
    public void restoreDiagram_ngoaile1() {
        // Error case: missing username
        when(httpServletRequest.getHeader("X-Username")).thenReturn(null);

        ResponseEntity<?> response = diagramListController.restoreDiagram(diagramId, httpServletRequest);

        assertEquals(HttpStatus.UNAUTHORIZED, response.getStatusCode());
        verify(diagramManagementService, never()).restoreDiagram(any(), any());
    }

    @Test
    @DisplayName("restoreDiagram_ngoaile2 - Service throws exception returns INTERNAL_SERVER_ERROR")
    public void restoreDiagram_ngoaile2() {
        // Error case: service exception
        when(httpServletRequest.getHeader("X-Username")).thenReturn(ownerUsername);
        doThrow(new RuntimeException("Restore failed")).when(diagramManagementService)
                .restoreDiagram(diagramId, ownerUsername);

        ResponseEntity<?> response = diagramListController.restoreDiagram(diagramId, httpServletRequest);

        assertEquals(HttpStatus.INTERNAL_SERVER_ERROR, response.getStatusCode());
    }

    // ==================== Test permanentlyDeleteDiagram ====================

    @Test
    @DisplayName("permanentlyDeleteDiagram_testChuan1 - Permanently delete successfully")
    public void permanentlyDeleteDiagram_testChuan1() {
        // Standard case: successful permanent delete
        when(httpServletRequest.getHeader("X-Username")).thenReturn(ownerUsername);
        doNothing().when(diagramManagementService).permanentlyDeleteDiagram(diagramId, ownerUsername);

        ResponseEntity<?> response = diagramListController.permanentlyDeleteDiagram(diagramId, httpServletRequest);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        verify(diagramManagementService).permanentlyDeleteDiagram(diagramId, ownerUsername);
    }

    @Test
    @DisplayName("permanentlyDeleteDiagram_ngoaile1 - Delete without username returns UNAUTHORIZED")
    public void permanentlyDeleteDiagram_ngoaile1() {
        // Error case: missing username
        when(httpServletRequest.getHeader("X-Username")).thenReturn(null);

        ResponseEntity<?> response = diagramListController.permanentlyDeleteDiagram(diagramId, httpServletRequest);

        assertEquals(HttpStatus.UNAUTHORIZED, response.getStatusCode());
        verify(diagramManagementService, never()).permanentlyDeleteDiagram(any(), any());
    }

    // ==================== Test isOwner ====================

    @Test
    @DisplayName("isOwner_testChuan1 - Check owner successfully returns true")
    public void isOwner_testChuan1() {
        // Standard case: user is owner
        when(httpServletRequest.getHeader("X-Username")).thenReturn(ownerUsername);
        when(diagramManagementService.isOwner(diagramId, ownerUsername)).thenReturn(true);

        ResponseEntity<Boolean> response = diagramListController.isOwner(diagramId, httpServletRequest);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertTrue(response.getBody());
        verify(diagramManagementService).isOwner(diagramId, ownerUsername);
    }

    @Test
    @DisplayName("isOwner_testChuan2 - Check owner returns false")
    public void isOwner_testChuan2() {
        // Standard case: user is not owner
        when(httpServletRequest.getHeader("X-Username")).thenReturn(username);
        when(diagramManagementService.isOwner(diagramId, username)).thenReturn(false);

        ResponseEntity<Boolean> response = diagramListController.isOwner(diagramId, httpServletRequest);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertFalse(response.getBody());
    }

    @Test
    @DisplayName("isOwner_ngoaile1 - Check owner without username returns UNAUTHORIZED")
    public void isOwner_ngoaile1() {
        // Error case: missing username
        when(httpServletRequest.getHeader("X-Username")).thenReturn(null);

        ResponseEntity<Boolean> response = diagramListController.isOwner(diagramId, httpServletRequest);

        assertEquals(HttpStatus.UNAUTHORIZED, response.getStatusCode());
        verify(diagramManagementService, never()).isOwner(any(), any());
    }

    // ==================== Test getTrashCount ====================

    @Test
    @DisplayName("getTrashCount_testChuan1 - Get trash count successfully")
    public void getTrashCount_testChuan1() {
        // Standard case: retrieve trash count
        when(httpServletRequest.getHeader("X-Username")).thenReturn(username);
        when(diagramManagementService.getTrashCount(username)).thenReturn(5L);

        ResponseEntity<Long> response = diagramListController.getTrashCount(httpServletRequest);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertEquals(5L, response.getBody());
        verify(diagramManagementService).getTrashCount(username);
    }

    @Test
    @DisplayName("getTrashCount_testChuan2 - Get trash count when empty")
    public void getTrashCount_testChuan2() {
        // Standard case: no items in trash
        when(httpServletRequest.getHeader("X-Username")).thenReturn(username);
        when(diagramManagementService.getTrashCount(username)).thenReturn(0L);

        ResponseEntity<Long> response = diagramListController.getTrashCount(httpServletRequest);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertEquals(0L, response.getBody());
    }

    @Test
    @DisplayName("getTrashCount_ngoaile1 - Get trash count without username returns UNAUTHORIZED")
    public void getTrashCount_ngoaile1() {
        // Error case: missing username
        when(httpServletRequest.getHeader("X-Username")).thenReturn(null);

        ResponseEntity<Long> response = diagramListController.getTrashCount(httpServletRequest);

        assertEquals(HttpStatus.UNAUTHORIZED, response.getStatusCode());
        verify(diagramManagementService, never()).getTrashCount(any());
    }

    // ==================== Integration Tests ====================

    @Test
    @DisplayName("integration_testChuan1 - Full delete, restore, check count flow")
    public void integration_testChuan1() {
        // Integration test: delete, restore, check trash count
        when(httpServletRequest.getHeader("X-Username")).thenReturn(ownerUsername);

        // Delete
        doNothing().when(diagramManagementService).softDeleteDiagram(diagramId, ownerUsername);
        ResponseEntity<?> deleteResponse = diagramListController.deleteDiagram(diagramId, httpServletRequest);
        assertEquals(HttpStatus.OK, deleteResponse.getStatusCode());

        // Get trash count
        when(diagramManagementService.getTrashCount(ownerUsername)).thenReturn(1L);
        ResponseEntity<Long> countResponse = diagramListController.getTrashCount(httpServletRequest);
        assertEquals(1L, countResponse.getBody());

        // Restore
        doNothing().when(diagramManagementService).restoreDiagram(diagramId, ownerUsername);
        ResponseEntity<?> restoreResponse = diagramListController.restoreDiagram(diagramId, httpServletRequest);
        assertEquals(HttpStatus.OK, restoreResponse.getStatusCode());

        // Check count again
        when(diagramManagementService.getTrashCount(ownerUsername)).thenReturn(0L);
        ResponseEntity<Long> finalCountResponse = diagramListController.getTrashCount(httpServletRequest);
        assertEquals(0L, finalCountResponse.getBody());

        verify(diagramManagementService).softDeleteDiagram(diagramId, ownerUsername);
        verify(diagramManagementService).restoreDiagram(diagramId, ownerUsername);
        verify(diagramManagementService, times(2)).getTrashCount(ownerUsername);
    }
}
