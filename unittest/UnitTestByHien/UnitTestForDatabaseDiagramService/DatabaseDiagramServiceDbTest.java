package com.example.react_flow_be.service;

import com.example.react_flow_be.entity.DatabaseDiagram;
import com.example.react_flow_be.repository.DatabaseDiagramRepository;
import jakarta.persistence.EntityNotFoundException;
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
import jakarta.persistence.EntityManager;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

@SpringBootTest(classes = DatabaseDiagramServiceDbTest.JpaTestApplication.class, properties = {
        "spring.datasource.url=jdbc:h2:mem:database_diagram_service_db_test;MODE=MySQL;DB_CLOSE_DELAY=-1;DB_CLOSE_ON_EXIT=FALSE",
        "spring.datasource.driver-class-name=org.h2.Driver",
        "spring.datasource.username=sa",
        "spring.datasource.password=",
        "spring.jpa.hibernate.ddl-auto=create-drop",
        "spring.jpa.database-platform=org.hibernate.dialect.H2Dialect",
        "spring.jpa.properties.hibernate.dialect=org.hibernate.dialect.H2Dialect",
        "eureka.client.enabled=false"
}, webEnvironment = SpringBootTest.WebEnvironment.NONE)
@Import(DatabaseDiagramService.class)
@Transactional
@ActiveProfiles("test")
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.ANY)
@DisplayName("DatabaseDiagramService Integration Tests - H2 + Transaction Rollback")
class DatabaseDiagramServiceDbTest {

    @SpringBootConfiguration
    @EnableAutoConfiguration
    @EntityScan(basePackages = "com.example.react_flow_be.entity")
    @EnableJpaRepositories(basePackages = "com.example.react_flow_be.repository")
    static class JpaTestApplication {
    }

    @Autowired
    private DatabaseDiagramService serviceUnderTest;

    @Autowired
    private DatabaseDiagramRepository databaseDiagramRepository;

    @Autowired
    private EntityManager entityManager;

    @MockBean
    private CollaborationService collaborationService;

    /**
     * TestCaseID: UN_DDS_DB_001
     * Mục tiêu:
     * - Kiểm chứng getDatabaseDiagramById đọc đúng dữ liệu từ DB H2.
     * CheckDB:
     * - Tạo trước 1 bản ghi diagram thật trong DB, gọi service và xác minh dữ liệu
     * khớp.
     * Rollback:
     * - Dữ liệu test tự rollback sau test do @Transactional.
     */
    @Test
    void getDatabaseDiagramById_shouldReturnPersistedDiagramFromH2() {
        DatabaseDiagram persistedDiagram = createAndPersistDiagram("DB Diagram A", false);

        DatabaseDiagram actualDiagram = serviceUnderTest.getDatabaseDiagramById(persistedDiagram.getId());

        assertEquals(persistedDiagram.getId(), actualDiagram.getId());
        assertEquals("DB Diagram A", actualDiagram.getName());
    }

    /**
     * TestCaseID: UN_DDS_DB_002
     * Mục tiêu:
     * - Kiểm chứng getDatabaseDiagramById ném EntityNotFoundException khi id không
     * tồn tại.
     * CheckDB:
     * - Dùng id chưa tồn tại trong DB.
     * Rollback:
     * - Không thay đổi DB.
     */
    @Test
    void getDatabaseDiagramById_shouldThrowEntityNotFoundException_whenIdNotFoundInH2() {
        assertThrows(EntityNotFoundException.class, () -> serviceUnderTest.getDatabaseDiagramById(999_999L));
    }

    /**
     * TestCaseID: UN_DDS_DB_003
     * Mục tiêu:
     * - Kiểm chứng updateDiagramName cập nhật tên thật trong DB và trả true.
     * CheckDB:
     * - Đọc lại repository sau update để xác minh name mới đã được persist.
     * Rollback:
     * - Dữ liệu thay đổi tự rollback sau test.
     */
    @Test
    void updateDiagramName_shouldUpdateNameInDatabaseAndReturnTrue() {
        DatabaseDiagram persistedDiagram = createAndPersistDiagram("Old Name", false);

        Boolean actualResult = serviceUnderTest.updateDiagramName(persistedDiagram.getId(), "New Name");

        assertTrue(actualResult);
        entityManager.flush();
        entityManager.clear();
        DatabaseDiagram reloadedDiagram = databaseDiagramRepository.findById(persistedDiagram.getId()).orElseThrow();
        assertEquals("New Name", reloadedDiagram.getName());
    }

    /**
     * TestCaseID: UN_DDS_DB_004
     * Mục tiêu:
     * - Kiểm chứng updateDiagramName trả false khi id không tồn tại.
     * CheckDB:
     * - Xác minh repository không tạo thêm bản ghi mới.
     * Rollback:
     * - Không có thay đổi dữ liệu.
     */
    @Test
    void updateDiagramName_shouldReturnFalse_whenIdDoesNotExist() {
        long totalBeforeUpdate = databaseDiagramRepository.count();

        Boolean actualResult = serviceUnderTest.updateDiagramName(123_456L, "No Effect");

        assertFalse(actualResult);
        assertEquals(totalBeforeUpdate, databaseDiagramRepository.count());
    }

    /**
     * TestCaseID: UN_DDS_DB_005
     * Mục tiêu:
     * - Kiểm chứng createBlankDiagram tạo bản ghi thật trong DB với các giá trị mặc
     * định đúng.
     * - Kiểm chứng gọi collaborationService.createOwner đúng tham số.
     * CheckDB:
     * - Đọc lại DB bằng id trả về và so sánh toàn bộ trường mặc định chính.
     * Rollback:
     * - Bản ghi mới được rollback sau test.
     */
    @Test
    void createBlankDiagram_shouldPersistDiagramAndCallCreateOwner() {
        DatabaseDiagram createdDiagram = serviceUnderTest.createBlankDiagram("New Blank Diagram", "owner_user");

        assertNotNull(createdDiagram.getId());
        Optional<DatabaseDiagram> reloadedDiagramOpt = databaseDiagramRepository.findById(createdDiagram.getId());
        assertTrue(reloadedDiagramOpt.isPresent());

        DatabaseDiagram reloadedDiagram = reloadedDiagramOpt.get();
        assertEquals("New Blank Diagram", reloadedDiagram.getName());
        assertEquals("", reloadedDiagram.getDescription());
        assertEquals(DatabaseDiagram.DatabaseType.MYSQL, reloadedDiagram.getDatabaseType());
        assertEquals("8.0", reloadedDiagram.getVersion());
        assertEquals("utf8mb4", reloadedDiagram.getCharset());
        assertEquals("utf8mb4_unicode_ci", reloadedDiagram.getCollation());
        assertFalse(reloadedDiagram.getIsPublic());
        assertFalse(reloadedDiagram.getIsTemplate());
        assertEquals(1.0, reloadedDiagram.getZoomLevel());
        assertEquals(0.0, reloadedDiagram.getPanX());
        assertEquals(0.0, reloadedDiagram.getPanY());

        verify(collaborationService, times(1)).createOwner(createdDiagram.getId(), "owner_user");
    }

    /**
     * TestCaseID: UN_DDS_DB_006
     * Mục tiêu:
     * - Kiểm chứng createSampleDatabaseDiagram tạo bản ghi sample thật trong DB.
     * CheckDB:
     * - Đọc lại DB và xác minh thông tin sample + isTemplate = true.
     * Rollback:
     * - Dữ liệu sample được rollback sau test.
     */
    @Test
    void createSampleDatabaseDiagram_shouldPersistSampleDiagramInH2() {
        DatabaseDiagram sampleDiagram = serviceUnderTest.createSampleDatabaseDiagram();

        assertNotNull(sampleDiagram.getId());
        DatabaseDiagram reloadedDiagram = databaseDiagramRepository.findById(sampleDiagram.getId()).orElseThrow();
        assertEquals("Blog System", reloadedDiagram.getName());
        assertEquals("Sample blog system database schema", reloadedDiagram.getDescription());
        assertEquals(DatabaseDiagram.DatabaseType.MYSQL, reloadedDiagram.getDatabaseType());
        assertTrue(reloadedDiagram.getIsTemplate());
        assertFalse(reloadedDiagram.getIsPublic());
        assertEquals(1.0, reloadedDiagram.getZoomLevel());
        assertEquals(0.0, reloadedDiagram.getPanX());
        assertEquals(0.0, reloadedDiagram.getPanY());
    }

    private DatabaseDiagram createAndPersistDiagram(String name, boolean isTemplate) {
        DatabaseDiagram diagram = new DatabaseDiagram();
        diagram.setName(name);
        diagram.setDescription("test description");
        diagram.setDatabaseType(DatabaseDiagram.DatabaseType.MYSQL);
        diagram.setVersion("8.0");
        diagram.setCharset("utf8mb4");
        diagram.setCollation("utf8mb4_unicode_ci");
        diagram.setIsPublic(false);
        diagram.setIsTemplate(isTemplate);
        diagram.setZoomLevel(1.0);
        diagram.setPanX(0.0);
        diagram.setPanY(0.0);
        return databaseDiagramRepository.save(diagram);
    }
}
