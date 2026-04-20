package com.example.react_flow_be.unittest;

import com.example.react_flow_be.dto.MigrationDetailDto;
import com.example.react_flow_be.dto.MigrationHistoryDto;
import com.example.react_flow_be.entity.DatabaseDiagram;
import com.example.react_flow_be.entity.Migration;
import com.example.react_flow_be.repository.DatabaseDiagramRepository;
import com.example.react_flow_be.repository.MigrationRepository;
import com.example.react_flow_be.service.AttributeService;
import com.example.react_flow_be.service.DatabaseDiagramService;
import com.example.react_flow_be.service.MigrationService;
import com.example.react_flow_be.service.ModelService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
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
@DisplayName("UC05-MigrationService Unit Tests")
class UC05_MigrationServiceTest {

    @Autowired
    private MigrationService migrationService;

    @Autowired
    private DatabaseDiagramService databaseDiagramService;

    @Autowired
    private ModelService modelService;

    @Autowired
    private AttributeService attributeService;

    @Autowired
    private MigrationRepository migrationRepository;

    @Autowired
    private DatabaseDiagramRepository databaseDiagramRepository;

    // ==================== Nested: CreateSnapshot Tests ====================
    @Nested
    @DisplayName("CreateSnapshot - createSnapshotOnDisconnect() Tests")
    class CreateSnapshotTests {
        private DatabaseDiagram testDiagram;
        private String testUsername;

        @BeforeEach
        void setUp() {
            testUsername = "test_user_uc05_" + System.currentTimeMillis();
            testDiagram = databaseDiagramService.createBlankDiagram("UC05 Test Diagram", testUsername);
        }

        @Test
        @DisplayName("UC05-UT-701: createSnapshotOnDisconnect - Create first snapshot successfully")
        void createSnapshotOnDisconnect_testChuan1() {
            modelService.createModel("User", "m_uc05_001", 100.0, 100.0, false, testDiagram);
            String initialHash = testDiagram.getLastSnapshotHash();

            boolean result = migrationService.createSnapshotOnDisconnect(testDiagram.getId(), testUsername);

            assertTrue(result);
            DatabaseDiagram updatedDiagram = databaseDiagramRepository.findById(testDiagram.getId()).orElseThrow();
            assertNotNull(updatedDiagram.getLastSnapshotHash());
            assertNotEquals(initialHash, updatedDiagram.getLastSnapshotHash());

            List<Migration> migrations = migrationRepository.findByDatabaseDiagramIdOrderByCreatedAtDesc(testDiagram.getId());
            assertEquals(1, migrations.size());
            assertEquals(testUsername, migrations.get(0).getUsername());
            assertNotNull(migrations.get(0).getSnapshotJson());
        }

        @Test
        @DisplayName("UC05-UT-702: createSnapshotOnDisconnect - No changes detected (same hash)")
        void createSnapshotOnDisconnect_testChuan2() {
            modelService.createModel("Product", "m_uc05_002", 200.0, 200.0, false, testDiagram);
            migrationService.createSnapshotOnDisconnect(testDiagram.getId(), testUsername);

            testDiagram = databaseDiagramRepository.findById(testDiagram.getId()).orElseThrow();
            String hashAfterFirstSnapshot = testDiagram.getLastSnapshotHash();

            boolean result = migrationService.createSnapshotOnDisconnect(testDiagram.getId(), testUsername);

            assertFalse(result);
            List<Migration> migrations = migrationRepository.findByDatabaseDiagramIdOrderByCreatedAtDesc(testDiagram.getId());
            assertEquals(1, migrations.size());

            DatabaseDiagram finalDiagram = databaseDiagramRepository.findById(testDiagram.getId()).orElseThrow();
            assertEquals(hashAfterFirstSnapshot, finalDiagram.getLastSnapshotHash());
        }

        @Test
        @DisplayName("UC05-UT-703: createSnapshotOnDisconnect - Changes detected, new snapshot created")
        void createSnapshotOnDisconnect_testChuan3() {
            var model = modelService.createModel("Category", "m_uc05_003", 150.0, 150.0, false, testDiagram);
            attributeService.createAttribute("attr_uc05_001", model, "id", "BIGINT", false, 0, false, true);
            migrationService.createSnapshotOnDisconnect(testDiagram.getId(), testUsername);

            attributeService.createAttribute("attr_uc05_002", model, "name", "VARCHAR", false, 1, true, false);

            boolean result = migrationService.createSnapshotOnDisconnect(testDiagram.getId(), testUsername + "_2");

            assertTrue(result);
            List<Migration> migrations = migrationRepository.findByDatabaseDiagramIdOrderByCreatedAtDesc(testDiagram.getId());
            assertEquals(2, migrations.size());
            assertEquals(testUsername + "_2", migrations.get(0).getUsername());
        }

        @Test
        @DisplayName("UC05-UT-704: createSnapshotOnDisconnect - Diagram not found")
        void createSnapshotOnDisconnect_testNgoaiLe1() {
            boolean result = migrationService.createSnapshotOnDisconnect(999999L, testUsername);
            assertFalse(result);
        }

        @Test
        @DisplayName("UC05-UT-705: createSnapshotOnDisconnect - Empty diagram (no models)")
        void createSnapshotOnDisconnect_testChuan4() {
            boolean result = migrationService.createSnapshotOnDisconnect(testDiagram.getId(), testUsername);

            assertTrue(result);
            List<Migration> migrations = migrationRepository.findByDatabaseDiagramIdOrderByCreatedAtDesc(testDiagram.getId());
            assertEquals(1, migrations.size());
            assertNotNull(migrations.get(0).getSnapshotJson());
        }
    }

    // ==================== Nested: GetHistory Tests ====================
    @Nested
    @DisplayName("GetHistory - getMigrationHistory() Tests")
    class GetHistoryTests {
        private DatabaseDiagram testDiagram;

        @BeforeEach
        void setUp() {
            String owner = "test_owner_history_" + System.currentTimeMillis();
            testDiagram = databaseDiagramService.createBlankDiagram("UC05 History Test", owner);
        }

        @Test
        @DisplayName("UC05-UT-706: getMigrationHistory - Retrieve multiple migration records")
        void getMigrationHistory_testChuan1() {
            Migration m1 = new Migration();
            m1.setUsername("user1");
            m1.setSnapshotHash("hash1");
            m1.setSnapshotJson("{\"models\": []}");
            m1.setDatabaseDiagram(testDiagram);
            migrationRepository.save(m1);

            Migration m2 = new Migration();
            m2.setUsername("user2");
            m2.setSnapshotHash("hash2");
            m2.setSnapshotJson("{\"models\": []}");
            m2.setDatabaseDiagram(testDiagram);
            migrationRepository.save(m2);

            Migration m3 = new Migration();
            m3.setUsername("user3");
            m3.setSnapshotHash("hash3");
            m3.setSnapshotJson("{\"models\": []}");
            m3.setDatabaseDiagram(testDiagram);
            migrationRepository.save(m3);

            List<MigrationHistoryDto> history = migrationService.getMigrationHistory(testDiagram.getId());

            assertNotNull(history);
            assertEquals(3, history.size());
            assertEquals("user3", history.get(0).getUsername());
            assertEquals("user2", history.get(1).getUsername());
            assertEquals("user1", history.get(2).getUsername());

            for (MigrationHistoryDto dto : history) {
                assertEquals(testDiagram.getId(), dto.getDiagramId());
                assertNotNull(dto.getSnapshotHash());
                assertNotNull(dto.getCreatedAt());
            }
        }

        @Test
        @DisplayName("UC05-UT-707: getMigrationHistory - Single migration record")
        void getMigrationHistory_testChuan2() {
            Migration m1 = new Migration();
            m1.setUsername("single_user");
            m1.setSnapshotHash("single_hash");
            m1.setSnapshotJson("{\"test\": \"data\"}");
            m1.setDatabaseDiagram(testDiagram);
            migrationRepository.save(m1);

            List<MigrationHistoryDto> history = migrationService.getMigrationHistory(testDiagram.getId());

            assertNotNull(history);
            assertEquals(1, history.size());
            assertEquals("single_user", history.get(0).getUsername());
        }

        @Test
        @DisplayName("UC05-UT-708: getMigrationHistory - No migration records exist")
        void getMigrationHistory_testChuan3() {
            List<MigrationHistoryDto> history = migrationService.getMigrationHistory(testDiagram.getId());

            assertNotNull(history);
            assertTrue(history.isEmpty());
        }

        @Test
        @DisplayName("UC05-UT-709: getMigrationHistory - Ordered by creation time descending")
        void getMigrationHistory_testChuan4() {
            Migration m1 = new Migration();
            m1.setUsername("first_user");
            m1.setSnapshotHash("hash_first");
            m1.setSnapshotJson("{}");
            m1.setDatabaseDiagram(testDiagram);
            migrationRepository.save(m1);

            try { Thread.sleep(50); } catch (InterruptedException e) { }

            Migration m2 = new Migration();
            m2.setUsername("second_user");
            m2.setSnapshotHash("hash_second");
            m2.setSnapshotJson("{}");
            m2.setDatabaseDiagram(testDiagram);
            migrationRepository.save(m2);

            List<MigrationHistoryDto> history = migrationService.getMigrationHistory(testDiagram.getId());

            assertEquals(2, history.size());
            assertTrue(history.get(0).getCreatedAt().isAfter(history.get(1).getCreatedAt()));
        }

        @Test
        @DisplayName("UC05-UT-710: getMigrationHistory - Diagram ID does not exist")
        void getMigrationHistory_testNgoaiLe1() {
            List<MigrationHistoryDto> history = migrationService.getMigrationHistory(999999L);

            assertNotNull(history);
            assertTrue(history.isEmpty());
        }

        @Test
        @DisplayName("UC05-UT-711: getMigrationHistory - Verify all fields are populated")
        void getMigrationHistory_testChuan5() {
            DatabaseDiagram diagram2 = databaseDiagramService.createBlankDiagram("Separate Diagram", "owner2");

            Migration m1 = new Migration();
            m1.setUsername("diagram1_user");
            m1.setSnapshotHash("diagram1_hash");
            m1.setSnapshotJson("{\"version\": 1}");
            m1.setDatabaseDiagram(testDiagram);
            migrationRepository.save(m1);

            Migration m2 = new Migration();
            m2.setUsername("diagram2_user");
            m2.setSnapshotHash("diagram2_hash");
            m2.setSnapshotJson("{\"version\": 2}");
            m2.setDatabaseDiagram(diagram2);
            migrationRepository.save(m2);

            List<MigrationHistoryDto> history = migrationService.getMigrationHistory(testDiagram.getId());

            assertEquals(1, history.size());
            MigrationHistoryDto dto = history.get(0);
            assertEquals(testDiagram.getId(), dto.getDiagramId());
            assertEquals("UC05 History Test", dto.getDiagramName());
            assertEquals("diagram1_user", dto.getUsername());
            assertEquals("diagram1_hash", dto.getSnapshotHash());
            assertNotNull(dto.getId());
            assertNotNull(dto.getCreatedAt());
        }
    }

    // ==================== Nested: GetDetail Tests ====================
    @Nested
    @DisplayName("GetDetail - getMigrationDetail() Tests")
    class GetDetailTests {
        private DatabaseDiagram testDiagram;
        private Migration testMigration;

        @BeforeEach
        void setUp() {
            String owner = "test_owner_detail_" + System.currentTimeMillis();
            testDiagram = databaseDiagramService.createBlankDiagram("UC05 Detail Test", owner);

            testMigration = new Migration();
            testMigration.setUsername("test_detail_user");
            testMigration.setSnapshotHash("test_detail_hash_abc123");
            testMigration.setSnapshotJson("{\"models\": [{\"id\": \"m1\", \"name\": \"User\"}]}");
            testMigration.setDatabaseDiagram(testDiagram);
            testMigration = migrationRepository.save(testMigration);
        }

        @Test
        @DisplayName("UC05-UT-712: getMigrationDetail - Successfully retrieve migration detail")
        void getMigrationDetail_testChuan1() {
            MigrationDetailDto detail = migrationService.getMigrationDetail(testMigration.getId());

            assertNotNull(detail);
            assertEquals(testMigration.getId(), detail.getId());
            assertEquals("test_detail_user", detail.getUsername());
            assertEquals("test_detail_hash_abc123", detail.getSnapshotHash());
            assertEquals(testDiagram.getId(), detail.getDiagramId());
            assertEquals("UC05 Detail Test", detail.getDiagramName());
            assertEquals("{\"models\": [{\"id\": \"m1\", \"name\": \"User\"}]}", detail.getSnapshotJson());
            assertNotNull(detail.getCreatedAt());
        }

        @Test
        @DisplayName("UC05-UT-713: getMigrationDetail - All fields are correctly populated")
        void getMigrationDetail_testChuan2() {
            String complexJson = "{\"version\": \"1.0\", \"models\": [{\"id\": \"m1\", \"name\": \"User\", " +
                    "\"attributes\": [{\"name\": \"id\", \"type\": \"BIGINT\"}]}], \"connections\": []}";

            Migration migration = new Migration();
            migration.setUsername("complex_user");
            migration.setSnapshotHash("complex_hash_xyz789");
            migration.setSnapshotJson(complexJson);
            migration.setDatabaseDiagram(testDiagram);
            migration = migrationRepository.save(migration);

            MigrationDetailDto detail = migrationService.getMigrationDetail(migration.getId());

            assertEquals(migration.getId(), detail.getId());
            assertEquals("complex_user", detail.getUsername());
            assertEquals("complex_hash_xyz789", detail.getSnapshotHash());
            assertEquals(complexJson, detail.getSnapshotJson());
            assertTrue(detail.getSnapshotJson().contains("User"));
            assertTrue(detail.getSnapshotJson().contains("BIGINT"));
        }

        @Test
        @DisplayName("UC05-UT-714: getMigrationDetail - Empty diagram snapshot")
        void getMigrationDetail_testChuan3() {
            Migration emptyMigration = new Migration();
            emptyMigration.setUsername("empty_user");
            emptyMigration.setSnapshotHash("empty_hash");
            emptyMigration.setSnapshotJson("{}");
            emptyMigration.setDatabaseDiagram(testDiagram);
            emptyMigration = migrationRepository.save(emptyMigration);

            MigrationDetailDto detail = migrationService.getMigrationDetail(emptyMigration.getId());

            assertNotNull(detail);
            assertEquals("{}", detail.getSnapshotJson());
            assertEquals("empty_user", detail.getUsername());
        }

        @Test
        @DisplayName("UC05-UT-715: getMigrationDetail - Large snapshot data")
        void getMigrationDetail_testChuan4() {
            StringBuilder largeJson = new StringBuilder("{\"models\": [");
            for (int i = 0; i < 100; i++) {
                if (i > 0) largeJson.append(",");
                largeJson.append("{\"id\": \"m").append(i).append("\", \"name\": \"Model").append(i).append("\"}");
            }
            largeJson.append("]}");

            Migration largeMigration = new Migration();
            largeMigration.setUsername("large_data_user");
            largeMigration.setSnapshotHash("large_hash");
            largeMigration.setSnapshotJson(largeJson.toString());
            largeMigration.setDatabaseDiagram(testDiagram);
            largeMigration = migrationRepository.save(largeMigration);

            MigrationDetailDto detail = migrationService.getMigrationDetail(largeMigration.getId());

            assertNotNull(detail);
            assertEquals(largeJson.toString(), detail.getSnapshotJson());
            assertTrue(detail.getSnapshotJson().length() > 1000);
        }

        @Test
        @DisplayName("UC05-UT-716: getMigrationDetail - Migration ID does not exist")
        void getMigrationDetail_testNgoaiLe1() {
            assertThrows(RuntimeException.class,
                () -> migrationService.getMigrationDetail(999999L));
        }

        @Test
        @DisplayName("UC05-UT-717: getMigrationDetail - Correct diagram reference")
        void getMigrationDetail_testChuan5() {
            DatabaseDiagram diagram2 = databaseDiagramService.createBlankDiagram(
                    "Another Diagram", "owner2");

            Migration m1 = new Migration();
            m1.setUsername("user1");
            m1.setSnapshotHash("hash1");
            m1.setSnapshotJson("{\"diagram\": 1}");
            m1.setDatabaseDiagram(testDiagram);
            m1 = migrationRepository.save(m1);

            Migration m2 = new Migration();
            m2.setUsername("user2");
            m2.setSnapshotHash("hash2");
            m2.setSnapshotJson("{\"diagram\": 2}");
            m2.setDatabaseDiagram(diagram2);
            m2 = migrationRepository.save(m2);

            MigrationDetailDto detail1 = migrationService.getMigrationDetail(m1.getId());
            MigrationDetailDto detail2 = migrationService.getMigrationDetail(m2.getId());

            assertEquals("UC05 Detail Test", detail1.getDiagramName());
            assertEquals("Another Diagram", detail2.getDiagramName());
            assertNotEquals(detail1.getDiagramId(), detail2.getDiagramId());
        }
    }
}

