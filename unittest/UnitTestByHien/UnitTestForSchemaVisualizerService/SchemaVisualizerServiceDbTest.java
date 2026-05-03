package com.example.react_flow_be.service;

import com.example.react_flow_be.dto.DatabaseDiagramDto;
import com.example.react_flow_be.dto.collaboration.CollaborationDTO;
import com.example.react_flow_be.entity.Collaboration;
import com.example.react_flow_be.entity.DatabaseDiagram;
import com.example.react_flow_be.entity.Model;
import com.example.react_flow_be.repository.AttributeRepository;
import com.example.react_flow_be.repository.ConnectionRepository;
import com.example.react_flow_be.repository.DatabaseDiagramRepository;
import com.example.react_flow_be.repository.ModelRepository;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.SpringBootConfiguration;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@SpringBootTest(classes = SchemaVisualizerServiceDbTest.JpaTestApplication.class, properties = {
        "spring.datasource.url=jdbc:h2:mem:schema_visualizer_service_db_test;MODE=MySQL;DB_CLOSE_DELAY=-1;DB_CLOSE_ON_EXIT=FALSE",
        "spring.datasource.driver-class-name=org.h2.Driver",
        "spring.datasource.username=sa",
        "spring.datasource.password=",
        "spring.jpa.hibernate.ddl-auto=create-drop",
        "spring.jpa.database-platform=org.hibernate.dialect.H2Dialect",
        "spring.jpa.properties.hibernate.dialect=org.hibernate.dialect.H2Dialect",
        "eureka.client.enabled=false"
}, webEnvironment = SpringBootTest.WebEnvironment.NONE)
@Import({ SchemaVisualizerService.class, DatabaseDiagramService.class, ModelService.class, AttributeService.class,
        ConnectionService.class })
@Transactional
@ActiveProfiles("test")
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.ANY)
@DisplayName("SchemaVisualizerService Integration Tests - H2 + Transaction Rollback")
class SchemaVisualizerServiceDbTest {

    @SpringBootConfiguration
    @EnableAutoConfiguration
    @EntityScan(basePackages = "com.example.react_flow_be.entity")
    @EnableJpaRepositories(basePackages = "com.example.react_flow_be.repository")
    static class JpaTestApplication {
    }

    @Autowired
    private SchemaVisualizerService serviceUnderTest;

    @Autowired
    private DatabaseDiagramRepository databaseDiagramRepository;

    @Autowired
    private ModelRepository modelRepository;

    @Autowired
    private AttributeRepository attributeRepository;

    @Autowired
    private ConnectionRepository connectionRepository;

    @Autowired
    private EntityManager entityManager;

    @MockBean
    private CollaborationService collaborationService;

    /**
     * TestCaseID: UN_SVS_DB_001
     * Mục tiêu:
     * - Kiểm chứng getSchemaData đọc đúng dữ liệu schema từ DB H2.
     * CheckDB:
     * - Tạo diagram + model trong DB, gọi service và xác minh DTO phản ánh đúng dữ
     * liệu.
     * Rollback:
     * - Dữ liệu test rollback sau test do @Transactional.
     */
    @Test
    void getSchemaData_shouldReturnDtoFromPersistedDiagram() {
        DatabaseDiagram diagram = createAndPersistDiagram("Schema A");

        when(collaborationService.getUserCollaboration(diagram.getId(), "alice"))
                .thenReturn(new CollaborationDTO(1L, "alice", Collaboration.CollaborationType.OWNER,
                        Collaboration.Permission.FULL_ACCESS, true, null, null));

        DatabaseDiagramDto result = serviceUnderTest.getSchemaData(diagram.getId(), "alice");

        assertEquals(diagram.getId(), result.getId());
        assertEquals("Schema A", result.getName());
        assertEquals(Collaboration.Permission.FULL_ACCESS, result.getPermission());
        assertEquals(0, result.getModels().size());
    }

    /**
     * TestCaseID: UN_SVS_DB_002
     * Mục tiêu:
     * - Kiểm chứng initializeSampleData tạo dữ liệu mẫu trong DB.
     * CheckDB:
     * - Sau khi gọi service phải có diagram mẫu, models, attributes và connections
     * được persist.
     * Rollback:
     * - Toàn bộ thay đổi rollback sau test.
     */
    @Test
    void initializeSampleData_shouldPersistSampleDiagramModelsAttributesAndConnections() {
        serviceUnderTest.initializeSampleData();
        entityManager.flush();
        entityManager.clear();

        assertTrue(databaseDiagramRepository.findAll().stream()
                .anyMatch(d -> "Blog System".equals(d.getName()) && Boolean.TRUE.equals(d.getIsTemplate())));
        assertEquals(3, modelRepository.findAll().size());
        assertEquals(23, attributeRepository.findAll().size());
        assertEquals(4, connectionRepository.findAll().size());
    }

    /**
     * TestCaseID: UN_SVS_DB_003
     * Mục tiêu:
     * - Kiểm chứng addModel tạo bản ghi model thật trong DB và trả về id.
     * CheckDB:
     * - Đọc lại model bằng id trả về để xác minh dữ liệu.
     * Rollback:
     * - Dữ liệu test rollback sau test.
     */
    @Test
    void addModel_shouldPersistModelAndReturnId() {
        DatabaseDiagram diagram = createAndPersistDiagram("For Add Model");

        String createdModelId = serviceUnderTest.addModel("Order", diagram.getId(), "node-order", 10.0, 20.0);

        assertNotNull(createdModelId);
        Model reloadedModel = modelRepository.findById(createdModelId).orElseThrow();
        assertEquals(createdModelId, reloadedModel.getId());
        assertEquals(10.0, reloadedModel.getPositionX());
        assertEquals(20.0, reloadedModel.getPositionY());
    }

    /**
     * TestCaseID: UN_SVS_DB_004
     * Mục tiêu:
     * - Kiểm chứng updateModelName cập nhật tên model trong DB với timestamp mới.
     * CheckDB:
     * - Flush/Clear rồi đọc lại DB để xác minh name/nodeId/nameUpdatedAt đã đổi.
     * Rollback:
     * - Thay đổi rollback sau test.
     */
    @Test
    void updateModelName_shouldPersistNewName_whenTimestampIsValid() {
        DatabaseDiagram diagram = createAndPersistDiagram("For Update Model");
        Model model = new Model();
        model.setId("m-update");
        model.setNodeId("old-node");
        model.setName("Old Name");
        model.setDatabaseDiagram(diagram);
        model.setNameUpdatedAt(LocalDateTime.of(2026, 5, 1, 10, 0));
        modelRepository.save(model);

        LocalDateTime newTimestamp = LocalDateTime.of(2026, 5, 2, 10, 0);
        boolean result = serviceUnderTest.updateModelName("m-update", "New Name", newTimestamp);

        assertTrue(result);
        entityManager.flush();
        entityManager.clear();
        Model reloadedModel = modelRepository.findById("m-update").orElseThrow();
        assertEquals("New Name", reloadedModel.getName());
        assertEquals("New Name", reloadedModel.getNodeId());
        assertEquals(newTimestamp, reloadedModel.getNameUpdatedAt());
    }

    /**
     * TestCaseID: UN_SVS_DB_005
     * Mục tiêu:
     * - Kiểm chứng deleteModel xóa model thật khỏi DB.
     * CheckDB:
     * - Trước khi gọi có model, sau khi gọi findById phải empty.
     * Rollback:
     * - Dữ liệu rollback sau test.
     */
    @Test
    void deleteModel_shouldRemoveModelFromDatabase() {
        DatabaseDiagram diagram = createAndPersistDiagram("For Delete Model");
        Model model = new Model();
        model.setId("m-delete");
        model.setNodeId("node-delete");
        model.setName("Delete Me");
        model.setDatabaseDiagram(diagram);
        modelRepository.save(model);

        boolean result = serviceUnderTest.deleteModel("m-delete");

        assertTrue(result);
        assertTrue(modelRepository.findById("m-delete").isEmpty());
    }

    private DatabaseDiagram createAndPersistDiagram(String name) {
        DatabaseDiagram diagram = new DatabaseDiagram();
        diagram.setName(name);
        diagram.setDescription("test");
        diagram.setDatabaseType(DatabaseDiagram.DatabaseType.MYSQL);
        diagram.setVersion("8.0");
        diagram.setCharset("utf8mb4");
        diagram.setCollation("utf8mb4_unicode_ci");
        diagram.setIsPublic(false);
        diagram.setIsTemplate(false);
        diagram.setZoomLevel(1.0);
        diagram.setPanX(0.0);
        diagram.setPanY(0.0);
        diagram.setModels(new ArrayList<>());
        return databaseDiagramRepository.save(diagram);
    }
}
