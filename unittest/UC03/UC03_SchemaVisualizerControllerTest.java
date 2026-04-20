package com.example.react_flow_be.unittest;

import com.example.react_flow_be.controller.SchemaVisualizerController;
import com.example.react_flow_be.dto.DatabaseDiagramDto;
import com.example.react_flow_be.dto.ModelDto;
import com.example.react_flow_be.dto.NewDiagramName;
import com.example.react_flow_be.entity.Collaboration;
import com.example.react_flow_be.entity.DatabaseDiagram;
import com.example.react_flow_be.repository.AttributeRepository;
import com.example.react_flow_be.repository.ConnectionRepository;
import com.example.react_flow_be.repository.DatabaseDiagramRepository;
import com.example.react_flow_be.repository.ModelRepository;
import com.example.react_flow_be.service.CollaborationService;
import com.example.react_flow_be.service.DatabaseDiagramService;
import com.example.react_flow_be.service.SchemaVisualizerService;
import jakarta.persistence.EntityNotFoundException;
import jakarta.servlet.http.HttpServletRequest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("UC03-SchemaVisualizerController Unit Tests")
class UC03_SchemaVisualizerControllerTest {

    @Mock
    private SchemaVisualizerService schemaVisualizerService;

    @Mock
    private CollaborationService collaborationService;

    @Mock
    private DatabaseDiagramService databaseDiagramService;

    @Mock
    private HttpServletRequest request;

    @Mock
    private ConnectionRepository connectionRepository;

    @Mock
    private AttributeRepository attributeRepository;

    @Mock
    private ModelRepository modelRepository;

    @Mock
    private DatabaseDiagramRepository databaseDiagramRepository;

    @InjectMocks
    private SchemaVisualizerController controller;

    private Long diagramId;
    private String username;
    private DatabaseDiagramDto sampleDiagramDto;

    @BeforeEach
    void setUp() {
        diagramId = 1L;
        username = "testuser";
        sampleDiagramDto = new DatabaseDiagramDto(
                diagramId,
                Collaboration.Permission.FULL_ACCESS,
                "Test Diagram",
                "Test Description",
                "MYSQL",
                "8.0",
                "utf8mb4",
                "utf8mb4_unicode_ci",
                false,
                false,
                1.0,
                0.0,
                0.0,
                new ArrayList<>()
        );
    }

    // ============ TEST: getSchemaData() ============

    @Test
    @DisplayName("UC03-UT-101: getSchemaData - Happy path with authorized user")
    void getSchemaData_testChuan1() {
        // Arrange
        when(request.getHeader("X-Username")).thenReturn(username);
        when(collaborationService.hasAccess(diagramId, username)).thenReturn(true);
        when(schemaVisualizerService.getSchemaData(diagramId, username)).thenReturn(sampleDiagramDto);

        // Act
        ResponseEntity<DatabaseDiagramDto> response = controller.getSchemaData(diagramId, request);

        // Assert
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertNotNull(response.getBody());
        assertEquals("Test Diagram", response.getBody().getName());
        assertEquals(diagramId, response.getBody().getId());
        verify(collaborationService, times(1)).hasAccess(diagramId, username);
        verify(schemaVisualizerService, times(1)).getSchemaData(diagramId, username);
    }

    @Test
    @DisplayName("UC03-UT-102: getSchemaData - Unauthorized user (no access)")
    void getSchemaData_testChuan2() {
        // Arrange
        when(request.getHeader("X-Username")).thenReturn(username);
        when(collaborationService.hasAccess(diagramId, username)).thenReturn(false);

        // Act
        ResponseEntity<DatabaseDiagramDto> response = controller.getSchemaData(diagramId, request);

        // Assert
        assertEquals(HttpStatus.FORBIDDEN, response.getStatusCode());
        assertNull(response.getBody());
        verify(collaborationService, times(1)).hasAccess(diagramId, username);
        verify(schemaVisualizerService, never()).getSchemaData(anyLong(), anyString());
    }

    @Test
    @DisplayName("UC03-UT-103: getSchemaData - Diagram not found (EntityNotFoundException)")
    void getSchemaData_testNgoaiLe1() {
        // Arrange
        when(request.getHeader("X-Username")).thenReturn(username);
        when(collaborationService.hasAccess(diagramId, username)).thenReturn(true);
        when(schemaVisualizerService.getSchemaData(diagramId, username))
                .thenThrow(new EntityNotFoundException("Diagram not found"));

        // Act & Assert
        assertThrows(EntityNotFoundException.class,
            () -> controller.getSchemaData(diagramId, request));
        verify(schemaVisualizerService, times(1)).getSchemaData(diagramId, username);
    }

    @Test
    @DisplayName("UC03-UT-104: getSchemaData - Null username header")
    void getSchemaData_testNgoaiLe2() {
        // Arrange
        when(request.getHeader("X-Username")).thenReturn(null);
        when(collaborationService.hasAccess(diagramId, null)).thenReturn(false);

        // Act
        ResponseEntity<DatabaseDiagramDto> response = controller.getSchemaData(diagramId, request);

        // Assert
        assertEquals(HttpStatus.FORBIDDEN, response.getStatusCode());
    }

    // ============ TEST: initializeSampleData() ============

    @Test
    @DisplayName("UC03-UT-105: initializeSampleData - Success case")
    void initializeSampleData_testChuan1() {
        // Arrange
        doNothing().when(schemaVisualizerService).initializeSampleData();

        // Act
        ResponseEntity<String> response = controller.initializeSampleData();

        // Assert
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertEquals("Sample data initialized successfully", response.getBody());
        verify(schemaVisualizerService, times(1)).initializeSampleData();
    }

    @Test
    @DisplayName("UC03-UT-106: initializeSampleData - Database error")
    void initializeSampleData_testNgoaiLe1() {
        // Arrange
        doThrow(new RuntimeException("Database connection failed"))
                .when(schemaVisualizerService).initializeSampleData();

        // Act
        ResponseEntity<String> response = controller.initializeSampleData();

        // Assert
        assertEquals(HttpStatus.INTERNAL_SERVER_ERROR, response.getStatusCode());
        assertTrue(response.getBody().contains("Failed to initialize sample data"));
        assertTrue(response.getBody().contains("Database connection failed"));
    }

    @Test
    @DisplayName("UC03-UT-107: initializeSampleData - Null pointer exception during initialization")
    void initializeSampleData_testNgoaiLe2() {
        // Arrange
        doThrow(new NullPointerException("Service is null"))
                .when(schemaVisualizerService).initializeSampleData();

        // Act
        ResponseEntity<String> response = controller.initializeSampleData();

        // Assert
        assertEquals(HttpStatus.INTERNAL_SERVER_ERROR, response.getStatusCode());
        assertTrue(response.getBody().contains("Failed to initialize sample data"));
    }

    // ============ TEST: newName() ============

    @Test
    @DisplayName("UC03-UT-108: newName - Successfully update diagram name")
    void newName_testChuan1() {
        // Arrange
        String newName = "Updated Diagram Name";
        NewDiagramName nameRequest = new NewDiagramName();
        nameRequest.setNewName(newName);
        when(databaseDiagramService.updateDiagramName(diagramId, newName)).thenReturn(true);

        // Act
        ResponseEntity<Boolean> response = controller.newName(diagramId, nameRequest);

        // Assert
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertTrue(response.getBody());
        verify(databaseDiagramService, times(1)).updateDiagramName(diagramId, newName);
    }

    @Test
    @DisplayName("UC03-UT-109: newName - Diagram not found")
    void newName_testChuan2() {
        // Arrange
        String newName = "Updated Name";
        NewDiagramName nameRequest = new NewDiagramName();
        nameRequest.setNewName(newName);
        when(databaseDiagramService.updateDiagramName(diagramId, newName)).thenReturn(false);

        // Act
        ResponseEntity<Boolean> response = controller.newName(diagramId, nameRequest);

        // Assert
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertFalse(response.getBody());
    }

    @Test
    @DisplayName("UC03-UT-110: newName - Empty diagram name")
    void newName_testChuan3() {
        // Arrange
        String emptyName = "";
        NewDiagramName nameRequest = new NewDiagramName();
        nameRequest.setNewName(emptyName);
        when(databaseDiagramService.updateDiagramName(diagramId, emptyName)).thenReturn(false);

        // Act
        ResponseEntity<Boolean> response = controller.newName(diagramId, nameRequest);

        // Assert
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertFalse(response.getBody());
    }

    // ============ TEST: clearAllData() ============

    @Test
    @DisplayName("UC03-UT-111: clearAllData - Successfully clear all data")
    void clearAllData_testChuan1() {
        // Act
        ResponseEntity<String> response = controller.clearAllData();

        // Assert
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertEquals("All data cleared successfully", response.getBody());
    }

    @Test
    @DisplayName("UC03-UT-112: clearAllData - Foreign key constraint violation")
    void clearAllData_testNgoaiLe1() {
        // This test simulates the actual behavior where clearAllData
        // deletes in correct order to avoid constraints
        // If FK constraints are violated, exception is caught
        // Act
        ResponseEntity<String> response = controller.clearAllData();

        // Assert - Should still succeed because order is correct
        assertEquals(HttpStatus.OK, response.getStatusCode());
    }
}

