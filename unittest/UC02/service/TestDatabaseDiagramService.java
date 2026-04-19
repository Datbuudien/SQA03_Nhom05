package com.example.react_flow_be.service;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import org.mockito.Mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import org.mockito.junit.jupiter.MockitoExtension;

import com.example.react_flow_be.entity.DatabaseDiagram;
import com.example.react_flow_be.repository.DatabaseDiagramRepository;

import jakarta.persistence.EntityNotFoundException;

/**
 * TestDatabaseDiagramService: Unit Test cho DatabaseDiagramService (Database Diagram Management)
 * 
 * Mục tiêu: Đảm bảo diagram cơ sở dữ liệu được quản lý chính xác
 * 
 * Coverage Level 2 (Branch Coverage):
 * - getAllDatabaseDiagrams: lấy tất cả diagrams
 * - getDatabaseDiagramById: lấy diagram theo id thành công, diagram không tồn tại
 * - updateDiagramName: cập nhật tên thành công, diagram không tồn tại
 * - createBlankDiagram: tạo diagram trống
 * - createSampleDatabaseDiagram: tạo diagram sample
 */
@DisplayName("TestDatabaseDiagramService - Database Diagram Management (UC02)")
@ExtendWith(MockitoExtension.class)
public class TestDatabaseDiagramService {

    private DatabaseDiagramService databaseDiagramService;

    @Mock
    private DatabaseDiagramRepository databaseDiagramRepository;

    @Mock
    private CollaborationService collaborationService;

    private DatabaseDiagram validDiagram;
    private final Long diagramId = 1L;
    private final String diagramName = "Test Database Diagram";
    private final String ownerUsername = "owner_user";

    @BeforeEach
    public void setUp() {
        databaseDiagramService = new DatabaseDiagramService(databaseDiagramRepository, collaborationService);

        // Create valid database diagram
        validDiagram = new DatabaseDiagram();
        validDiagram.setId(diagramId);
        validDiagram.setName(diagramName);
        validDiagram.setDescription("Test Description");
        validDiagram.setIsPublic(false);
        validDiagram.setIsTemplate(false);
        validDiagram.setIsDeleted(false);
        validDiagram.setCreatedAt(LocalDateTime.now());
        validDiagram.setUpdatedAt(LocalDateTime.now());
        validDiagram.setDatabaseType(DatabaseDiagram.DatabaseType.MYSQL);
        validDiagram.setVersion("8.0");
        validDiagram.setCharset("utf8mb4");
        validDiagram.setCollation("utf8mb4_unicode_ci");
    }

    // ==================== Test getAllDatabaseDiagrams ====================

    @Test
    @DisplayName("getAllDatabaseDiagrams_testChuan1 - Get all database diagrams")
    public void getAllDatabaseDiagrams_testChuan1() {
        // Standard case: retrieve all diagrams
        List<DatabaseDiagram> diagrams = new ArrayList<>();
        diagrams.add(validDiagram);

        DatabaseDiagram diagram2 = new DatabaseDiagram();
        diagram2.setId(2L);
        diagram2.setName("Another Diagram");
        diagram2.setDatabaseType(DatabaseDiagram.DatabaseType.POSTGRESQL);
        diagrams.add(diagram2);

        when(databaseDiagramRepository.findAll()).thenReturn(diagrams);

        List<DatabaseDiagram> result = databaseDiagramService.getAllDatabaseDiagrams();

        assertNotNull(result);
        assertEquals(2, result.size());
        verify(databaseDiagramRepository).findAll();
    }

    @Test
    @DisplayName("getAllDatabaseDiagrams_testChuan2 - Get diagrams returns empty list")
    public void getAllDatabaseDiagrams_testChuan2() {
        // Standard case: no diagrams exist
        when(databaseDiagramRepository.findAll()).thenReturn(new ArrayList<>());

        List<DatabaseDiagram> result = databaseDiagramService.getAllDatabaseDiagrams();

        assertNotNull(result);
        assertTrue(result.isEmpty());
    }

    // ==================== Test getDatabaseDiagramById ====================

    @Test
    @DisplayName("getDatabaseDiagramById_testChuan1 - Get diagram by id successfully")
    public void getDatabaseDiagramById_testChuan1() {
        // Standard case: diagram exists
        when(databaseDiagramRepository.findById(diagramId)).thenReturn(Optional.of(validDiagram));

        DatabaseDiagram result = databaseDiagramService.getDatabaseDiagramById(diagramId);

        assertNotNull(result);
        assertEquals(diagramId, result.getId());
        assertEquals(diagramName, result.getName());
        verify(databaseDiagramRepository).findById(diagramId);
    }

    @Test
    @DisplayName("getDatabaseDiagramById_ngoaile1 - Diagram not found throws exception")
    public void getDatabaseDiagramById_ngoaile1() {
        // Error case: diagram not found
        when(databaseDiagramRepository.findById(999L)).thenReturn(Optional.empty());

        assertThrows(EntityNotFoundException.class, 
                () -> databaseDiagramService.getDatabaseDiagramById(999L));
    }

    // ==================== Test updateDiagramName ====================

    @Test
    @DisplayName("updateDiagramName_testChuan1 - Update diagram name successfully")
    public void updateDiagramName_testChuan1() {
        // Standard case: update name
        String newName = "Updated Diagram Name";
        when(databaseDiagramRepository.existsById(diagramId)).thenReturn(true);
        when(databaseDiagramRepository.updateNameById(diagramId, newName)).thenReturn(1);

        Boolean result = databaseDiagramService.updateDiagramName(diagramId, newName);

        assertTrue(result);
    }

    @Test
    @DisplayName("updateDiagramName_testChuan2 - Update with same name returns true")
    public void updateDiagramName_testChuan2() {
        // Standard case: update with same name
        when(databaseDiagramRepository.existsById(diagramId)).thenReturn(true);
        when(databaseDiagramRepository.updateNameById(diagramId, diagramName)).thenReturn(1);

        Boolean result = databaseDiagramService.updateDiagramName(diagramId, diagramName);

        assertTrue(result);
    }

    @Test
    @DisplayName("updateDiagramName_ngoaile1 - Update non-existent diagram throws exception")
    public void updateDiagramName_ngoaile1() {
        // Error case: diagram not found
        when(databaseDiagramRepository.existsById(999L)).thenReturn(false);

        assertThrows(EntityNotFoundException.class, 
                () -> databaseDiagramService.updateDiagramName(999L, "New Name"));

        verify(databaseDiagramRepository, never()).updateNameById(any(), any());
    }

    @Test
    @DisplayName("updateDiagramName_ngoaile2 - Update with empty name throws exception")
    public void updateDiagramName_ngoaile2() {
        // Error case: empty name
        assertThrows(IllegalArgumentException.class, 
                () -> databaseDiagramService.updateDiagramName(diagramId, ""));

        verify(databaseDiagramRepository, never()).updateNameById(any(), any());
    }

    @Test
    @DisplayName("updateDiagramName_ngoaile3 - Update with null name throws exception")
    public void updateDiagramName_ngoaile3() {
        // Error case: null name
        assertThrows(IllegalArgumentException.class, 
                () -> databaseDiagramService.updateDiagramName(diagramId, null));

        verify(databaseDiagramRepository, never()).updateNameById(any(), any());
    }

    // ==================== Test createBlankDiagram ====================

    @Test
    @DisplayName("createBlankDiagram_testChuan1 - Create blank diagram successfully")
    public void createBlankDiagram_testChuan1() {
        // Standard case: create blank diagram
        String newDiagramName = "New Blank Diagram";
        
        DatabaseDiagram newDiagram = new DatabaseDiagram();
        newDiagram.setId(10L);
        newDiagram.setName(newDiagramName);
        newDiagram.setIsDeleted(false);
        newDiagram.setCreatedAt(LocalDateTime.now());

        when(databaseDiagramRepository.save(any(DatabaseDiagram.class))).thenReturn(newDiagram);
        when(collaborationService.createOwner(anyLong(), eq(ownerUsername)))
                .thenReturn(null); // Mock DTO return

        DatabaseDiagram result = databaseDiagramService.createBlankDiagram(newDiagramName, ownerUsername);

        assertNotNull(result);
        assertEquals(newDiagramName, result.getName());
        assertFalse(result.getIsDeleted());
        verify(databaseDiagramRepository).save(any(DatabaseDiagram.class));
        verify(collaborationService).createOwner(anyLong(), eq(ownerUsername));
    }

    @Test
    @DisplayName("createBlankDiagram_testChuan2 - Create diagram with empty name")
    public void createBlankDiagram_testChuan2() {
        // Error case: empty diagram name
        assertThrows(IllegalArgumentException.class, 
                () -> databaseDiagramService.createBlankDiagram("", ownerUsername));

        verify(databaseDiagramRepository, never()).save(any());
    }

    @Test
    @DisplayName("createBlankDiagram_ngoaile1 - Create diagram with null name throws exception")
    public void createBlankDiagram_ngoaile1() {
        // Error case: null diagram name
        assertThrows(IllegalArgumentException.class, 
                () -> databaseDiagramService.createBlankDiagram(null, ownerUsername));

        verify(databaseDiagramRepository, never()).save(any());
    }

    @Test
    @DisplayName("createBlankDiagram_ngoaile2 - Create diagram with empty username throws exception")
    public void createBlankDiagram_ngoaile2() {
        // Error case: empty username
        assertThrows(IllegalArgumentException.class, 
                () -> databaseDiagramService.createBlankDiagram("New Diagram", ""));

        verify(databaseDiagramRepository, never()).save(any());
    }

    @Test
    @DisplayName("createBlankDiagram_ngoaile3 - Create diagram with null username throws exception")
    public void createBlankDiagram_ngoaile3() {
        // Error case: null username
        assertThrows(IllegalArgumentException.class, 
                () -> databaseDiagramService.createBlankDiagram("New Diagram", null));

        verify(databaseDiagramRepository, never()).save(any());
    }

    // ==================== Test createSampleDatabaseDiagram ====================

    @Test
    @DisplayName("createSampleDatabaseDiagram_testChuan1 - Create sample diagram successfully")
    public void createSampleDatabaseDiagram_testChuan1() {
        // Standard case: create sample diagram with predefined data
        DatabaseDiagram sampleDiagram = new DatabaseDiagram();
        sampleDiagram.setId(20L);
        sampleDiagram.setName("Sample Database Diagram");
        sampleDiagram.setDescription("Sample database structure");
        sampleDiagram.setDatabaseType(DatabaseDiagram.DatabaseType.MYSQL);
        sampleDiagram.setVersion("8.0");
        sampleDiagram.setCharset("utf8mb4");
        sampleDiagram.setCollation("utf8mb4_unicode_ci");
        sampleDiagram.setIsTemplate(true);
        sampleDiagram.setCreatedAt(LocalDateTime.now());

        when(databaseDiagramRepository.save(any(DatabaseDiagram.class))).thenReturn(sampleDiagram);

        DatabaseDiagram result = databaseDiagramService.createSampleDatabaseDiagram();

        assertNotNull(result);
        assertEquals("Sample Database Diagram", result.getName());
        assertEquals(DatabaseDiagram.DatabaseType.MYSQL, result.getDatabaseType());
        assertTrue(result.getIsTemplate());
        verify(databaseDiagramRepository).save(any(DatabaseDiagram.class));
    }

    // ==================== Integration Tests ====================

    @Test
    @DisplayName("integration_testChuan1 - Create blank diagram and update name")
    public void integration_testChuan1() {
        // Integration test: create then update
        DatabaseDiagram newDiagram = new DatabaseDiagram();
        newDiagram.setId(10L);
        newDiagram.setName("Initial Name");
        newDiagram.setCreatedAt(LocalDateTime.now());
        newDiagram.setIsDeleted(false);

        // Create blank diagram
        when(databaseDiagramRepository.save(any(DatabaseDiagram.class))).thenReturn(newDiagram);
        when(collaborationService.createOwner(10L, ownerUsername))
                .thenReturn(null);

        DatabaseDiagram created = databaseDiagramService.createBlankDiagram("Initial Name", ownerUsername);
        assertNotNull(created);

        // Update name
        String updatedName = "Updated Name";
        newDiagram.setName(updatedName);
        when(databaseDiagramRepository.findById(10L)).thenReturn(Optional.of(newDiagram));
        when(databaseDiagramRepository.save(any(DatabaseDiagram.class))).thenReturn(newDiagram);

        Boolean updateResult = databaseDiagramService.updateDiagramName(10L, updatedName);
        assertTrue(updateResult);
        assertEquals(updatedName, newDiagram.getName());
    }

    @Test
    @DisplayName("integration_testChuan2 - Get all diagrams and find by id")
    public void integration_testChuan2() {
        // Integration test: get all then retrieve specific one
        List<DatabaseDiagram> diagrams = new ArrayList<>();
        diagrams.add(validDiagram);

        DatabaseDiagram diagram2 = new DatabaseDiagram();
        diagram2.setId(2L);
        diagram2.setName("Another Diagram");
        diagrams.add(diagram2);

        when(databaseDiagramRepository.findAll()).thenReturn(diagrams);
        when(databaseDiagramRepository.findById(diagramId)).thenReturn(Optional.of(validDiagram));

        List<DatabaseDiagram> allDiagrams = databaseDiagramService.getAllDatabaseDiagrams();
        assertEquals(2, allDiagrams.size());

        DatabaseDiagram retrieved = databaseDiagramService.getDatabaseDiagramById(diagramId);
        assertNotNull(retrieved);
        assertEquals(diagramName, retrieved.getName());
    }

    @Test
    @DisplayName("integration_testChuan3 - Create sample and list all diagrams")
    public void integration_testChuan3() {
        // Integration test: create sample and retrieve in list
        DatabaseDiagram sampleDiagram = new DatabaseDiagram();
        sampleDiagram.setId(20L);
        sampleDiagram.setName("Sample Database Diagram");
        sampleDiagram.setIsTemplate(true);

        List<DatabaseDiagram> diagrams = new ArrayList<>();
        diagrams.add(validDiagram);
        diagrams.add(sampleDiagram);

        when(databaseDiagramRepository.save(any(DatabaseDiagram.class))).thenReturn(sampleDiagram);
        when(databaseDiagramRepository.findAll()).thenReturn(diagrams);

        DatabaseDiagram created = databaseDiagramService.createSampleDatabaseDiagram();
        assertNotNull(created);

        List<DatabaseDiagram> allDiagrams = databaseDiagramService.getAllDatabaseDiagrams();
        assertEquals(2, allDiagrams.size());
    }
}
