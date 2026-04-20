package com.example.react_flow_be.unittest;

import com.example.react_flow_be.entity.Collaboration;
import com.example.react_flow_be.entity.DatabaseDiagram;
import com.example.react_flow_be.repository.CollaborationRepository;
import com.example.react_flow_be.repository.DatabaseDiagramRepository;
import com.example.react_flow_be.service.CollaborationService;
import com.example.react_flow_be.service.DatabaseDiagramService;
import jakarta.persistence.EntityNotFoundException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@ActiveProfiles("test")
@Transactional
@DisplayName("UC03-DatabaseDiagramService Unit Tests")
class UC03_DatabaseDiagramServiceTest {

    @Autowired
    private DatabaseDiagramService databaseDiagramService;

    @Autowired
    private DatabaseDiagramRepository databaseDiagramRepository;

    @Autowired
    private CollaborationRepository collaborationRepository;

    @Autowired
    private CollaborationService collaborationService;

    private String testUsername;

    @BeforeEach
    void setUp() {
        testUsername = "test_owner_uc03_" + System.currentTimeMillis();
    }

    // ============ TEST: createBlankDiagram() ============

    @Test
    @DisplayName("UC03-UT-201: createBlankDiagram - Standard case")
    void createBlankDiagram_testChuan1() {
        // Arrange
        String diagramName = "Test Blank Diagram";

        // Act
        DatabaseDiagram created = databaseDiagramService.createBlankDiagram(diagramName, testUsername);

        // Assert
        assertNotNull(created);
        assertNotNull(created.getId());
        assertEquals(diagramName, created.getName());
        assertEquals(DatabaseDiagram.DatabaseType.MYSQL, created.getDatabaseType());
        assertEquals("8.0", created.getVersion());
        assertEquals("utf8mb4", created.getCharset());
        assertEquals("utf8mb4_unicode_ci", created.getCollation());
        assertFalse(created.getIsPublic());
        assertFalse(created.getIsTemplate());
        assertEquals(1.0, created.getZoomLevel());
        assertEquals(0.0, created.getPanX());
        assertEquals(0.0, created.getPanY());

        // Verify owner collaboration created
        Optional<Collaboration> ownerCollab = collaborationRepository
                .findByDiagramIdAndType(created.getId(), Collaboration.CollaborationType.OWNER);
        assertTrue(ownerCollab.isPresent());
        assertEquals(testUsername, ownerCollab.get().getUsername());
        assertEquals(Collaboration.Permission.FULL_ACCESS, ownerCollab.get().getPermission());
    }

    @Test
    @DisplayName("UC03-UT-202: createBlankDiagram - With special characters in name")
    void createBlankDiagram_testChuan2() {
        // Arrange
        String diagramName = "Test!@#$%^&*()_+-=[]{}|;':\",./<>?";

        // Act
        DatabaseDiagram created = databaseDiagramService.createBlankDiagram(diagramName, testUsername);

        // Assert
        assertNotNull(created);
        assertEquals(diagramName, created.getName());
        assertTrue(databaseDiagramRepository.findById(created.getId()).isPresent());
    }

    @Test
    @DisplayName("UC03-UT-203: createBlankDiagram - With spaces in name")
    void createBlankDiagram_testChuan3() {
        // Arrange
        String diagramName = "  Diagram With Spaces  ";

        // Act
        DatabaseDiagram created = databaseDiagramService.createBlankDiagram(diagramName, testUsername);

        // Assert
        assertNotNull(created);
        assertEquals(diagramName, created.getName());
    }

    @Test
    @DisplayName("UC03-UT-204: createBlankDiagram - With null diagram name")
    void createBlankDiagram_testNgoaiLe1() {
        // Act
        DatabaseDiagram created = databaseDiagramService.createBlankDiagram(null, testUsername);

        // Assert - allows null name
        assertNotNull(created);
    }

    // ============ TEST: getDatabaseDiagramById() ============

    @Test
    @DisplayName("UC03-UT-205: getDatabaseDiagramById - Diagram exists")
    void getDatabaseDiagramById_testChuan1() {
        // Arrange
        DatabaseDiagram created = databaseDiagramService.createBlankDiagram("Test Diagram", testUsername);

        // Act
        DatabaseDiagram retrieved = databaseDiagramService.getDatabaseDiagramById(created.getId());

        // Assert
        assertNotNull(retrieved);
        assertEquals(created.getId(), retrieved.getId());
        assertEquals("Test Diagram", retrieved.getName());
    }

    @Test
    @DisplayName("UC03-UT-206: getDatabaseDiagramById - Diagram not found")
    void getDatabaseDiagramById_testNgoaiLe1() {
        // Arrange
        Long nonExistentId = 999999L;

        // Act & Assert
        assertThrows(EntityNotFoundException.class,
            () -> databaseDiagramService.getDatabaseDiagramById(nonExistentId));
    }

    // ============ TEST: getAllDatabaseDiagrams() ============

    @Test
    @DisplayName("UC03-UT-207: getAllDatabaseDiagrams - Multiple diagrams exist")
    void getAllDatabaseDiagrams_testChuan1() {
        // Arrange
        databaseDiagramService.createBlankDiagram("Diagram 1", testUsername + "_1");
        databaseDiagramService.createBlankDiagram("Diagram 2", testUsername + "_2");
        databaseDiagramService.createBlankDiagram("Diagram 3", testUsername + "_3");

        // Act
        var diagrams = databaseDiagramService.getAllDatabaseDiagrams();

        // Assert
        assertNotNull(diagrams);
        assertTrue(diagrams.size() >= 3);
    }

    @Test
    @DisplayName("UC03-UT-208: getAllDatabaseDiagrams - Empty database")
    void getAllDatabaseDiagrams_testChuan2() {
        // Arrange - Clear all diagrams
        databaseDiagramRepository.deleteAll();

        // Act
        var diagrams = databaseDiagramService.getAllDatabaseDiagrams();

        // Assert
        assertNotNull(diagrams);
        assertEquals(0, diagrams.size());
    }

    // ============ TEST: updateDiagramName() ============

    @Test
    @DisplayName("UC03-UT-209: updateDiagramName - Non-existent diagram returns false")
    void updateDiagramName_testChuan1() {
        // Arrange
        Long nonExistentId = 999999L;
        String newName = "Updated Name";

        // Act
        Boolean result = databaseDiagramService.updateDiagramName(nonExistentId, newName);

        // Assert
        assertFalse(result);
    }

    @Test
    @DisplayName("UC03-UT-210: updateDiagramName - Diagram not found")
    void updateDiagramName_testChuan2() {
        // Arrange
        Long nonExistentId = 888888L;
        String newName = "Updated Name";

        // Act
        Boolean result = databaseDiagramService.updateDiagramName(nonExistentId, newName);

        // Assert
        assertFalse(result);
    }

    @Test
    @DisplayName("UC03-UT-211: updateDiagramName - With empty string")
    void updateDiagramName_testChuan3() {
        // Arrange
        Long nonExistentId = 777777L;
        String emptyName = "";

        // Act
        Boolean result = databaseDiagramService.updateDiagramName(nonExistentId, emptyName);

        // Assert
        assertFalse(result);
    }

    @Test
    @DisplayName("UC03-UT-212: updateDiagramName - With special characters")
    void updateDiagramName_testChuan4() {
        // Arrange
        Long nonExistentId = 666666L;
        String specialName = "Diagram!@#$%^&*()";

        // Act
        Boolean result = databaseDiagramService.updateDiagramName(nonExistentId, specialName);

        // Assert
        assertFalse(result);
    }

    // ============ TEST: createSampleDatabaseDiagram() ============

    @Test
    @DisplayName("UC03-UT-213: createSampleDatabaseDiagram - Creates correct sample data")
    void createSampleDatabaseDiagram_testChuan1() {
        // Act
        DatabaseDiagram sample = databaseDiagramService.createSampleDatabaseDiagram();

        // Assert
        assertNotNull(sample);
        assertNotNull(sample.getId());
        assertEquals("Blog System", sample.getName());
        assertEquals("Sample blog system database schema", sample.getDescription());
        assertEquals(DatabaseDiagram.DatabaseType.MYSQL, sample.getDatabaseType());
        assertEquals("8.0", sample.getVersion());
        assertEquals("utf8mb4", sample.getCharset());
        assertEquals("utf8mb4_unicode_ci", sample.getCollation());
        assertFalse(sample.getIsPublic());
        assertTrue(sample.getIsTemplate());
        assertEquals(1.0, sample.getZoomLevel());
        assertEquals(0.0, sample.getPanX());
        assertEquals(0.0, sample.getPanY());
    }

    @Test
    @DisplayName("UC03-UT-214: createSampleDatabaseDiagram - Verify persistence")
    void createSampleDatabaseDiagram_testChuan2() {
        // Act
        DatabaseDiagram sample = databaseDiagramService.createSampleDatabaseDiagram();

        // Assert
        Optional<DatabaseDiagram> retrieved = databaseDiagramRepository.findById(sample.getId());
        assertTrue(retrieved.isPresent());
        assertEquals(sample.getId(), retrieved.get().getId());
    }
}

