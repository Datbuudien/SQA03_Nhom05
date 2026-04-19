package com.example.react_flow_be.unittest;

import com.example.react_flow_be.controller.MigrationController;
import com.example.react_flow_be.dto.MigrationDetailDto;
import com.example.react_flow_be.dto.MigrationHistoryDto;
import com.example.react_flow_be.entity.DatabaseDiagram;
import com.example.react_flow_be.entity.Migration;
import com.example.react_flow_be.entity.Model;
import com.example.react_flow_be.repository.MigrationRepository;
import com.example.react_flow_be.service.AttributeService;
import com.example.react_flow_be.service.DatabaseDiagramService;
import com.example.react_flow_be.service.ModelService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@ActiveProfiles("test")
@Transactional
@DisplayName("UC05-MigrationController Unit Tests")
class UC05_MigrationControllerTest {

    @Autowired
    private MigrationController migrationController;

    @Autowired
    private DatabaseDiagramService databaseDiagramService;

    @Autowired
    private ModelService modelService;

    @Autowired
    private AttributeService attributeService;

    @Autowired
    private MigrationRepository migrationRepository;

    // ==================== Nested: GetHistory Tests ====================
    @Nested
    @DisplayName("GetHistory - getMigrationHistory() Tests")
    class GetHistoryTests {
        private DatabaseDiagram testDiagram;
        private String testUsername;
        private MockHttpServletRequest mockRequest;

        @BeforeEach
        void setUp() {
            testUsername = "ctrl_user_" + System.currentTimeMillis();
            testDiagram = databaseDiagramService.createBlankDiagram("UC05 Controller Test", testUsername);
            mockRequest = new MockHttpServletRequest();
            mockRequest.addHeader("X-Username", testUsername);
        }

        @Test
        @DisplayName("UC05-UT-725: getMigrationHistory - Return history with 200 OK")
        void getMigrationHistory_testChuan1() {
            Migration m1 = new Migration();
            m1.setUsername("user1");
            m1.setSnapshotHash("hash1");
            m1.setSnapshotJson("{}");
            m1.setDatabaseDiagram(testDiagram);
            migrationRepository.save(m1);

            ResponseEntity<List<MigrationHistoryDto>> response =
                migrationController.getMigrationHistory(testDiagram.getId(), mockRequest);

            assertEquals(HttpStatus.OK, response.getStatusCode());
            assertNotNull(response.getBody());
            assertEquals(1, response.getBody().size());
        }

        @Test
        @DisplayName("UC05-UT-726: getMigrationHistory - Empty list when no migrations")
        void getMigrationHistory_testChuan2() {
            ResponseEntity<List<MigrationHistoryDto>> response =
                migrationController.getMigrationHistory(testDiagram.getId(), mockRequest);

            assertEquals(HttpStatus.OK, response.getStatusCode());
            assertNotNull(response.getBody());
            assertTrue(response.getBody().isEmpty());
        }

        @Test
        @DisplayName("UC05-UT-727: getMigrationHistory - Multiple records ordered by creation")
        void getMigrationHistory_testChuan3() {
            for (int i = 1; i <= 3; i++) {
                Migration m = new Migration();
                m.setUsername("user" + i);
                m.setSnapshotHash("hash" + i);
                m.setSnapshotJson("{}");
                m.setDatabaseDiagram(testDiagram);
                migrationRepository.save(m);
            }

            ResponseEntity<List<MigrationHistoryDto>> response =
                migrationController.getMigrationHistory(testDiagram.getId(), mockRequest);

            assertEquals(HttpStatus.OK, response.getStatusCode());
            assertEquals(3, response.getBody().size());
            assertEquals("user3", response.getBody().get(0).getUsername());
            assertEquals("user2", response.getBody().get(1).getUsername());
            assertEquals("user1", response.getBody().get(2).getUsername());
        }

        @Test
        @DisplayName("UC05-UT-728: getMigrationHistory - Non-existent diagram returns error")
        void getMigrationHistory_testNgoaiLe1() {
            ResponseEntity<List<MigrationHistoryDto>> response =
                migrationController.getMigrationHistory(999999L, mockRequest);

            assertEquals(HttpStatus.INTERNAL_SERVER_ERROR, response.getStatusCode());
        }
    }

    // ==================== Nested: GetDetail Tests ====================
    @Nested
    @DisplayName("GetDetail - getMigrationDetail() Tests")
    class GetDetailTests {
        private DatabaseDiagram testDiagram;
        private String testUsername;
        private MockHttpServletRequest mockRequest;

        @BeforeEach
        void setUp() {
            testUsername = "ctrl_detail_user_" + System.currentTimeMillis();
            testDiagram = databaseDiagramService.createBlankDiagram("UC05 Detail Ctrl Test", testUsername);
            mockRequest = new MockHttpServletRequest();
            mockRequest.addHeader("X-Username", testUsername);
        }

        @Test
        @DisplayName("UC05-UT-729: getMigrationDetail - Return detail with 200 OK")
        void getMigrationDetail_testChuan1() {
            Migration m = new Migration();
            m.setUsername("detail_user");
            m.setSnapshotHash("detail_hash");
            m.setSnapshotJson("{\"models\": []}");
            m.setDatabaseDiagram(testDiagram);
            m = migrationRepository.save(m);

            ResponseEntity<MigrationDetailDto> response =
                migrationController.getMigrationDetail(m.getId(), mockRequest);

            assertEquals(HttpStatus.OK, response.getStatusCode());
            assertNotNull(response.getBody());
            assertEquals(m.getId(), response.getBody().getId());
        }

        @Test
        @DisplayName("UC05-UT-730: getMigrationDetail - All fields are complete")
        void getMigrationDetail_testChuan2() {
            String complexJson = "{\"version\": \"1.0\", \"models\": [{\"id\": \"m1\"}]}";
            Migration m = new Migration();
            m.setUsername("full_detail_user");
            m.setSnapshotHash("full_detail_hash");
            m.setSnapshotJson(complexJson);
            m.setDatabaseDiagram(testDiagram);
            m = migrationRepository.save(m);

            ResponseEntity<MigrationDetailDto> response =
                migrationController.getMigrationDetail(m.getId(), mockRequest);

            assertEquals(HttpStatus.OK, response.getStatusCode());
            MigrationDetailDto detail = response.getBody();
            assertNotNull(detail);
            assertEquals(complexJson, detail.getSnapshotJson());
        }

        @Test
        @DisplayName("UC05-UT-731: getMigrationDetail - Not found returns 404")
        void getMigrationDetail_testNgoaiLe1() {
            ResponseEntity<MigrationDetailDto> response =
                migrationController.getMigrationDetail(999999L, mockRequest);

            assertEquals(HttpStatus.NOT_FOUND, response.getStatusCode());
        }
    }

    // ==================== Nested: CreateSnapshot Tests ====================
    @Nested
    @DisplayName("CreateSnapshot - createManualSnapshot() Tests")
    class CreateSnapshotTests {
        private DatabaseDiagram testDiagram;
        private String testUsername;
        private MockHttpServletRequest mockRequest;

        @BeforeEach
        void setUp() {
            testUsername = "ctrl_snap_user_" + System.currentTimeMillis();
            testDiagram = databaseDiagramService.createBlankDiagram("UC05 Snapshot Ctrl Test", testUsername);
            mockRequest = new MockHttpServletRequest();
            mockRequest.addHeader("X-Username", testUsername);
        }

        @Test
        @DisplayName("UC05-UT-732: createManualSnapshot - Successfully create snapshot")
        void createManualSnapshot_testChuan1() {
            Model model = modelService.createModel("User", "m_snap_ctrl_001", 100.0, 100.0, false, testDiagram);
            attributeService.createAttribute("attr_snap_ctrl_001", model, "id", "BIGINT", false, 0, false, true);

            ResponseEntity<String> response =
                migrationController.createManualSnapshot(testDiagram.getId(), mockRequest);

            assertEquals(HttpStatus.OK, response.getStatusCode());
            assertTrue(response.getBody().contains("successfully"));

            List<Migration> migrations = migrationRepository.findByDatabaseDiagramIdOrderByCreatedAtDesc(testDiagram.getId());
            assertEquals(1, migrations.size());
        }

        @Test
        @DisplayName("UC05-UT-733: createManualSnapshot - No changes detected")
        void createManualSnapshot_testChuan2() {
            Model model = modelService.createModel("Product", "m_snap_ctrl_002", 200.0, 200.0, false, testDiagram);
            attributeService.createAttribute("attr_snap_ctrl_002", model, "id", "BIGINT", false, 0, false, true);

            migrationController.createManualSnapshot(testDiagram.getId(), mockRequest);

            ResponseEntity<String> response =
                migrationController.createManualSnapshot(testDiagram.getId(), mockRequest);

            assertEquals(HttpStatus.OK, response.getStatusCode());
            assertTrue(response.getBody().contains("not created"));

            List<Migration> migrations = migrationRepository.findByDatabaseDiagramIdOrderByCreatedAtDesc(testDiagram.getId());
            assertEquals(1, migrations.size());
        }

        @Test
        @DisplayName("UC05-UT-734: createManualSnapshot - Changes detected, new snapshot created")
        void createManualSnapshot_testChuan3() {
            Model model = modelService.createModel("Category", "m_snap_ctrl_003", 150.0, 150.0, false, testDiagram);
            attributeService.createAttribute("attr_snap_ctrl_003", model, "id", "BIGINT", false, 0, false, true);

            migrationController.createManualSnapshot(testDiagram.getId(), mockRequest);

            attributeService.createAttribute("attr_snap_ctrl_004", model, "name", "VARCHAR", false, 1, true, false);

            ResponseEntity<String> response =
                migrationController.createManualSnapshot(testDiagram.getId(), mockRequest);

            assertEquals(HttpStatus.OK, response.getStatusCode());
            assertTrue(response.getBody().contains("successfully"));

            List<Migration> migrations = migrationRepository.findByDatabaseDiagramIdOrderByCreatedAtDesc(testDiagram.getId());
            assertEquals(2, migrations.size());
        }

        @Test
        @DisplayName("UC05-UT-735: createManualSnapshot - Non-existent diagram returns 500")
        void createManualSnapshot_testNgoaiLe1() {
            ResponseEntity<String> response =
                migrationController.createManualSnapshot(999999L, mockRequest);

            assertEquals(HttpStatus.INTERNAL_SERVER_ERROR, response.getStatusCode());
            assertTrue(response.getBody().contains("Error"));
        }

        @Test
        @DisplayName("UC05-UT-736: createManualSnapshot - Complex diagram with multiple models")
        void createManualSnapshot_testChuan4() {
            Model m1 = modelService.createModel("User", "m_snap_ctrl_005", 100.0, 100.0, false, testDiagram);
            attributeService.createAttribute("attr_snap_ctrl_005", m1, "id", "BIGINT", false, 0, false, true);
            attributeService.createAttribute("attr_snap_ctrl_006", m1, "email", "VARCHAR", false, 1, true, false);

            Model m2 = modelService.createModel("Post", "m_snap_ctrl_006", 300.0, 100.0, false, testDiagram);
            attributeService.createAttribute("attr_snap_ctrl_007", m2, "id", "BIGINT", false, 0, false, true);
            attributeService.createAttribute("attr_snap_ctrl_008", m2, "user_id", "BIGINT", false, 1, true, false);

            ResponseEntity<String> response =
                migrationController.createManualSnapshot(testDiagram.getId(), mockRequest);

            assertEquals(HttpStatus.OK, response.getStatusCode());

            List<Migration> migrations = migrationRepository.findByDatabaseDiagramIdOrderByCreatedAtDesc(testDiagram.getId());
            assertEquals(1, migrations.size());
            assertNotNull(migrations.get(0).getSnapshotJson());
            assertTrue(migrations.get(0).getSnapshotJson().contains("User"));
            assertTrue(migrations.get(0).getSnapshotJson().contains("Post"));
        }

        @Test
        @DisplayName("UC05-UT-737: createManualSnapshot - Empty diagram creates snapshot")
        void createManualSnapshot_testChuan5() {
            ResponseEntity<String> response =
                migrationController.createManualSnapshot(testDiagram.getId(), mockRequest);

            assertEquals(HttpStatus.OK, response.getStatusCode());

            List<Migration> migrations = migrationRepository.findByDatabaseDiagramIdOrderByCreatedAtDesc(testDiagram.getId());
            assertEquals(1, migrations.size());
        }
    }
}

