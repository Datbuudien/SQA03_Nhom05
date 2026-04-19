package com.example.react_flow_be.unittest;

import com.example.react_flow_be.repository.MigrationRepository;
import com.example.react_flow_be.entity.DatabaseDiagram;
import com.example.react_flow_be.entity.Migration;
import com.example.react_flow_be.service.DatabaseDiagramService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@ActiveProfiles("test")
@Transactional
@DisplayName("UC05-MigrationRepository: Query Methods Unit Tests")
class UC05_MigrationRepository_QueryTest {

    @Autowired
    private MigrationRepository migrationRepository;

    @Autowired
    private DatabaseDiagramService databaseDiagramService;

    private DatabaseDiagram testDiagram;
    private DatabaseDiagram testDiagram2;

    @BeforeEach
    void setUp() {
        String owner1 = "repo_user_" + System.currentTimeMillis();
        testDiagram = databaseDiagramService.createBlankDiagram("Repo Test Diagram 1", owner1);

        String owner2 = "repo_user_" + (System.currentTimeMillis() + 1);
        testDiagram2 = databaseDiagramService.createBlankDiagram("Repo Test Diagram 2", owner2);
    }

    // UC05-UT-718: findByDatabaseDiagramIdOrderByCreatedAtDesc - Multiple records
    @Test
    @DisplayName("UC05-UT-718: findByDatabaseDiagramIdOrderByCreatedAtDesc - Multiple records sorted DESC")
    void findByDatabaseDiagramIdOrderByCreatedAtDesc_testChuan1() {
        // Arrange
        for (int i = 1; i <= 3; i++) {
            Migration m = new Migration();
            m.setUsername("user" + i);
            m.setSnapshotHash("hash" + i);
            m.setSnapshotJson("{}");
            m.setDatabaseDiagram(testDiagram);
            migrationRepository.save(m);
        }

        // Act
        List<Migration> migrations = migrationRepository.findByDatabaseDiagramIdOrderByCreatedAtDesc(testDiagram.getId());

        // Assert
        assertEquals(3, migrations.size());
        assertTrue(migrations.get(0).getCreatedAt().isAfter(migrations.get(1).getCreatedAt()) ||
                   migrations.get(0).getCreatedAt().equals(migrations.get(1).getCreatedAt()));
        assertTrue(migrations.get(1).getCreatedAt().isAfter(migrations.get(2).getCreatedAt()) ||
                   migrations.get(1).getCreatedAt().equals(migrations.get(2).getCreatedAt()));
    }

    // UC05-UT-719: findByDatabaseDiagramIdOrderByCreatedAtDesc - Single record
    @Test
    @DisplayName("UC05-UT-719: findByDatabaseDiagramIdOrderByCreatedAtDesc - Single migration record")
    void findByDatabaseDiagramIdOrderByCreatedAtDesc_testChuan2() {
        // Arrange
        Migration m = new Migration();
        m.setUsername("single_user");
        m.setSnapshotHash("single_hash");
        m.setSnapshotJson("{}");
        m.setDatabaseDiagram(testDiagram);
        migrationRepository.save(m);

        // Act
        List<Migration> migrations = migrationRepository.findByDatabaseDiagramIdOrderByCreatedAtDesc(testDiagram.getId());

        // Assert
        assertEquals(1, migrations.size());
        assertEquals("single_user", migrations.get(0).getUsername());
    }

    // UC05-UT-720: findByDatabaseDiagramIdOrderByCreatedAtDesc - Empty result
    @Test
    @DisplayName("UC05-UT-720: findByDatabaseDiagramIdOrderByCreatedAtDesc - No records for diagram")
    void findByDatabaseDiagramIdOrderByCreatedAtDesc_testChuan3() {
        // Act
        List<Migration> migrations = migrationRepository.findByDatabaseDiagramIdOrderByCreatedAtDesc(testDiagram.getId());

        // Assert
        assertNotNull(migrations);
        assertTrue(migrations.isEmpty());
    }

    // UC05-UT-721: findByDatabaseDiagramIdOrderByCreatedAtDesc - Diagram isolation
    @Test
    @DisplayName("UC05-UT-721: findByDatabaseDiagramIdOrderByCreatedAtDesc - Correct diagram isolation")
    void findByDatabaseDiagramIdOrderByCreatedAtDesc_testChuan4() {
        // Arrange
        Migration m1 = new Migration();
        m1.setUsername("diagram1_user");
        m1.setSnapshotHash("diagram1_hash");
        m1.setSnapshotJson("{}");
        m1.setDatabaseDiagram(testDiagram);
        migrationRepository.save(m1);

        Migration m2 = new Migration();
        m2.setUsername("diagram2_user");
        m2.setSnapshotHash("diagram2_hash");
        m2.setSnapshotJson("{}");
        m2.setDatabaseDiagram(testDiagram2);
        migrationRepository.save(m2);

        // Act
        List<Migration> migrationsForDiagram1 = migrationRepository.findByDatabaseDiagramIdOrderByCreatedAtDesc(testDiagram.getId());
        List<Migration> migrationsForDiagram2 = migrationRepository.findByDatabaseDiagramIdOrderByCreatedAtDesc(testDiagram2.getId());

        // Assert
        assertEquals(1, migrationsForDiagram1.size());
        assertEquals("diagram1_user", migrationsForDiagram1.get(0).getUsername());

        assertEquals(1, migrationsForDiagram2.size());
        assertEquals("diagram2_user", migrationsForDiagram2.get(0).getUsername());
    }

    // UC05-UT-722: findByDatabaseDiagramIdOrderByCreatedAtDesc - Non-existent diagram
    @Test
    @DisplayName("UC05-UT-722: findByDatabaseDiagramIdOrderByCreatedAtDesc - Non-existent diagram returns empty")
    void findByDatabaseDiagramIdOrderByCreatedAtDesc_testNgoaiLe1() {
        // Act
        List<Migration> migrations = migrationRepository.findByDatabaseDiagramIdOrderByCreatedAtDesc(999999L);

        // Assert
        assertNotNull(migrations);
        assertTrue(migrations.isEmpty());
    }

    // UC05-UT-723: findByDatabaseDiagramIdOrderByCreatedAtDesc - Large dataset
    @Test
    @DisplayName("UC05-UT-723: findByDatabaseDiagramIdOrderByCreatedAtDesc - Large dataset maintains order")
    void findByDatabaseDiagramIdOrderByCreatedAtDesc_testChuan5() {
        // Arrange
        for (int i = 0; i < 10; i++) {
            Migration m = new Migration();
            m.setUsername("user" + i);
            m.setSnapshotHash("hash" + i);
            m.setSnapshotJson("{}");
            m.setDatabaseDiagram(testDiagram);
            migrationRepository.save(m);

            try { Thread.sleep(10); } catch (InterruptedException e) { }
        }

        // Act
        List<Migration> migrations = migrationRepository.findByDatabaseDiagramIdOrderByCreatedAtDesc(testDiagram.getId());

        // Assert
        assertEquals(10, migrations.size());
        for (int i = 0; i < migrations.size() - 1; i++) {
            assertTrue(migrations.get(i).getCreatedAt().isAfter(migrations.get(i + 1).getCreatedAt()) ||
                       migrations.get(i).getCreatedAt().equals(migrations.get(i + 1).getCreatedAt()));
        }
    }

    // UC05-UT-724: findTopByDatabaseDiagramIdOrderByCreatedAtDesc - Get latest
    @Test
    @DisplayName("UC05-UT-724: findTopByDatabaseDiagramIdOrderByCreatedAtDesc - Latest migration first")
    void findTopByDatabaseDiagramIdOrderByCreatedAtDesc_testChuan1() {
        // Arrange
        for (int i = 1; i <= 3; i++) {
            Migration m = new Migration();
            m.setUsername("user" + i);
            m.setSnapshotHash("hash" + i);
            m.setSnapshotJson("{}");
            m.setDatabaseDiagram(testDiagram);
            migrationRepository.save(m);
        }

        // Act
        List<Migration> migrations = migrationRepository.findTopByDatabaseDiagramIdOrderByCreatedAtDesc(testDiagram.getId());

        // Assert
        assertNotNull(migrations);
        assertTrue(migrations.size() > 0);
        assertEquals("user3", migrations.get(0).getUsername());
    }
}

