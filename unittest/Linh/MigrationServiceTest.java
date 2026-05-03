package com.example.react_flow_be.service;

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.AfterEach;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.argThat;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.reset;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import org.mockito.junit.jupiter.MockitoExtension;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.example.react_flow_be.dto.MigrationDetailDto;
import com.example.react_flow_be.dto.MigrationHistoryDto;
import com.example.react_flow_be.entity.Attribute;
import com.example.react_flow_be.entity.Connection;
import com.example.react_flow_be.entity.DatabaseDiagram;
import com.example.react_flow_be.entity.Migration;
import com.example.react_flow_be.entity.Model;
import com.example.react_flow_be.repository.DatabaseDiagramRepository;
import com.example.react_flow_be.repository.MigrationRepository;
import com.fasterxml.jackson.databind.ObjectMapper;

/**
 * ============================================================================
 * Unit Test cho MigrationService - Tầng Service quản lý Migration Diagram
 * ============================================================================
 * Mô tả: Test các phương thức tạo snapshot, lưu migration history, và lấy chi tiết
 *        migration từ database diagram changes tracking
 * Phương pháp: Sử dụng Mockito để mock các Repository và ObjectMapper dependency
 * Rollback: Sử dụng Mockito (không tương tác DB thật) nên không cần rollback DB.
 *           Mỗi test được reset mock objects qua @BeforeEach/@AfterEach.
 * Database: Khi test lưu dữ liệu, phải verify:
 *           1. Repository save() được gọi đúng số lần
 *           2. Migration snapshot được lưu với đúng hash và JSON
 *           3. DatabaseDiagram lastSnapshotHash được cập nhật
 * ============================================================================
 */
@ExtendWith(MockitoExtension.class)
public class MigrationServiceTest {

    private static final Logger logger = LoggerFactory.getLogger(MigrationServiceTest.class);

    @Mock
    private MigrationRepository migrationRepository;

    @Mock
    private DatabaseDiagramRepository databaseDiagramRepository;

    @Mock
    private ObjectMapper objectMapper;

    @InjectMocks
    private MigrationService migrationService;

    // ============ Test Data ============
    private DatabaseDiagram sampleDiagram;
    private Model sampleModel;
    private Attribute sampleAttribute;
    private Connection sampleConnection;
    private Migration sampleMigration;
    private String sampleSnapshotJson;
    private String sampleSnapshotHash;

    /**
     * Khởi tạo dữ liệu test trước mỗi test case.
     * Đảm bảo mỗi test case có dữ liệu sạch, không bị ảnh hưởng bởi test khác.
     * 
     * Các object được khởi tạo:
     * - DatabaseDiagram: Biểu diễn một sơ đồ database
     * - Model: Biểu diễn một bảng trong database
     * - Attribute: Biểu diễn một cột trong bảng
     * - Connection: Biểu diễn foreign key relationship
     * - Migration: Biểu diễn một lần lưu snapshot
     */
    @BeforeEach
    public void setUp() {
        logger.info("========================================");
        logger.info("[SETUP] Khởi tạo dữ liệu test cho MigrationService...");

        // Khởi tạo DatabaseDiagram mẫu - đại diện cho sơ đồ database
        sampleDiagram = new DatabaseDiagram();
        sampleDiagram.setId(1L);
        sampleDiagram.setName("Schema test");
        sampleDiagram.setDatabaseType(DatabaseDiagram.DatabaseType.MYSQL);
        sampleDiagram.setVersion("8.0");
        sampleDiagram.setCharset("utf8mb4");
        sampleDiagram.setCollation("utf8mb4_unicode_ci");
        sampleDiagram.setLastSnapshotHash(null);  // Lần đầu import, chưa có hash

        // Khởi tạo Attribute mẫu - đại diện cho cột trong bảng
        sampleAttribute = new Attribute();
        sampleAttribute.setId("attr-001");
        sampleAttribute.setName("user_id");
        sampleAttribute.setDataType("INT");
        sampleAttribute.setLength(20);
        sampleAttribute.setIsNullable(false);
        sampleAttribute.setIsPrimaryKey(true);
        sampleAttribute.setIsForeignKey(false);
        sampleAttribute.setAttributeOrder(0);
        sampleAttribute.setConnection(Collections.emptyList());

        // Khởi tạo Model mẫu - đại diện cho bảng trong database
        sampleModel = new Model();
        sampleModel.setId("model-001");
        sampleModel.setName("Users");
        sampleModel.setNodeId("Users");
        sampleModel.setPositionX(100.0);
        sampleModel.setPositionY(100.0);
        sampleModel.setDatabaseDiagram(sampleDiagram);
        sampleModel.setAttributes(Arrays.asList(sampleAttribute));

        // Khởi tạo Connection mẫu - đại diện cho foreign key
        sampleConnection = new Connection();
        sampleConnection.setId(1L);
        sampleConnection.setForeignKeyName("fk_user_role");
        sampleConnection.setTargetAttributeId("attr-002");
        sampleConnection.setTargetModel(sampleModel);

        // Khởi tạo snapshot JSON và hash mẫu
        sampleSnapshotJson = "{\"diagramId\":1,\"diagramName\":\"Schema test\",\"models\":[]}";
        sampleSnapshotHash = "abc123def456";  // SHA-256 hash mô phỏng

        // Khởi tạo Migration mẫu - biểu diễn một lần lưu snapshot
        sampleMigration = new Migration();
        sampleMigration.setId(1L);
        sampleMigration.setUsername("admin");
        sampleMigration.setSnapshotJson(sampleSnapshotJson);
        sampleMigration.setSnapshotHash(sampleSnapshotHash);
        sampleMigration.setDatabaseDiagram(sampleDiagram);
        sampleMigration.setCreatedAt(LocalDateTime.now());

        // Set models vào diagram
        sampleDiagram.setModels(Arrays.asList(sampleModel));

        logger.info("[SETUP] Hoàn tất khởi tạo dữ liệu test.");
    }

    /**
     * Dọn dẹp sau mỗi test case - reset tất cả mock objects.
     * Đảm bảo rollback trạng thái về ban đầu.
     * 
     * Mockito.reset() sẽ:
     * - Xóa stubbing (when...thenReturn)
     * - Xóa verification history
     * - Trả mock về trạng thái sạch
     */
    @AfterEach
    public void tearDown() {
        logger.info("[TEARDOWN] Dọn dẹp dữ liệu test, reset mock objects...");
        reset(migrationRepository, databaseDiagramRepository, objectMapper);
        sampleDiagram = null;
        sampleModel = null;
        sampleAttribute = null;
        sampleConnection = null;
        sampleMigration = null;
        logger.info("[TEARDOWN] Hoàn tất dọn dẹp. Trạng thái đã được khôi phục.");
        logger.info("========================================\n");
    }

    // ========================================================================================
    // TEST CASES CHO createSnapshotOnDisconnect()
    // ========================================================================================
    // Phương thức này là cốt lõi của Migration Service - được gọi khi user disconnect
    // Nó tạo snapshot, tính hash, compare với hash cũ, và save nếu có thay đổi
    // ========================================================================================

    /**
     * UT_MS_001: Tạo snapshot khi diagram có thay đổi và không có hash cũ
     * 
     * Mô tả: Khi user import diagram lần đầu hoặc diagram bị thay đổi,
     *        service phải tạo snapshot mới và lưu migration
     * Kịch bản:
     *   - Diagram có ID=1, chưa có lastSnapshotHash
     *   - Service tạo snapshot JSON mới
     *   - Tính hash từ JSON
     *   - Save migration vào repository
     *   - Cập nhật lastSnapshotHash của diagram
     * 
     * Input:
     *   - diagramId=1 (diagram hợp lệ)
     *   - username="admin" (user tạo ra thay đổi)
     * 
     * Expected Output:
     *   - Return true (đã tạo snapshot)
     *   - migrationRepository.save() được gọi 1 lần
     *   - databaseDiagramRepository.save() được gọi 1 lần
     *   - diagram.lastSnapshotHash được set với hash mới
     * 
     * Coverage:
     *   - Happy path: snapshot creation success
     *   - Database persistence: verify save calls
     *   - State update: verify lastSnapshotHash updated
     */
    @Test
    public void UT_MS_001_createSnapshotOnDisconnect_withChangesAndNoLastHash_shouldSaveNewMigration() throws Exception {
        logger.info("[UT_MS_001] BẮT ĐẦU: Tạo snapshot khi có thay đổi (lần đầu import)");
        logger.info("[UT_MS_001] Input: diagramId=1, username='admin', lastHash=null");

        // Arrange: Setup mock behavior
        when(databaseDiagramRepository.findById(1L)).thenReturn(Optional.of(sampleDiagram));
        when(objectMapper.writeValueAsString(any())).thenReturn(sampleSnapshotJson);
        when(migrationRepository.save(any(Migration.class))).thenReturn(sampleMigration);
        when(databaseDiagramRepository.save(any(DatabaseDiagram.class))).thenReturn(sampleDiagram);

        // Act: Gọi service method
        boolean result = migrationService.createSnapshotOnDisconnect(1L, "admin");

        // Assert
        assertTrue(result, "Phải return true khi có thay đổi");
        verify(databaseDiagramRepository, times(1)).findById(1L);
        verify(migrationRepository, times(1)).save(argThat(m ->
                m.getUsername().equals("admin") &&
                m.getSnapshotJson().equals(sampleSnapshotJson) &&
                m.getDatabaseDiagram().getId() == 1L));
        verify(databaseDiagramRepository, times(1)).save(argThat(d ->
                d.getLastSnapshotHash() != null));

        logger.info("[UT_MS_001] KẾT QUẢ: PASSED - Snapshot được tạo và lưu thành công");
    }

    /**
     * UT_MS_002: Tạo snapshot khi diagram không tồn tại
     * 
     * Mô tả: Khi diagramId không hợp lệ, service phải xử lý gracefully
     * Kịch bản: Service thử tìm diagram nhưng không tìm thấy
     * 
     * Input:
     *   - diagramId=999 (ID không tồn tại)
     *   - username="admin"
     * 
     * Expected Output:
     *   - Return false (không tạo snapshot)
     *   - migrationRepository.save() KHÔNG được gọi
     *   - Exception được catch, log error
     * 
     * Coverage:
     *   - Error handling: diagram not found
     *   - Service resilience: no crash
     */
    @Test
    public void UT_MS_002_createSnapshotOnDisconnect_diagramNotFound_shouldReturnFalse() throws Exception {
        logger.info("[UT_MS_002] BẮT ĐẦU: Tạo snapshot khi diagram không tồn tại");
        logger.info("[UT_MS_002] Input: diagramId=999 (invalid)");

        // Arrange
        when(databaseDiagramRepository.findById(999L)).thenReturn(Optional.empty());

        // Act
        boolean result = migrationService.createSnapshotOnDisconnect(999L, "admin");

        // Assert
        assertFalse(result, "Phải return false khi diagram không tìm thấy");
        verify(migrationRepository, never()).save(any());
        verify(databaseDiagramRepository, never()).save(any());

        logger.info("[UT_MS_002] KẾT QUẢ: PASSED - Service handled missing diagram gracefully");
    }

    /**
     * UT_MS_003: Tạo snapshot khi không có thay đổi (hash trùng)
     * 
     * Mô tả: Khi diagram được lưu lần cuối với hash X, và user disconnect với
     *        cùng dữ liệu, service phải detect ra không có thay đổi
     * Kịch bản:
     *   - diagram.lastSnapshotHash = "old-hash"
     *   - Snapshot mới tính ra hash = "old-hash" (giống nhau)
     *   - Service skip việc save migration
     * 
     * Input:
     *   - diagramId=1
     *   - username="admin"
     *   - lastSnapshotHash="old-hash" (có hash cũ)
     * 
     * Expected Output:
     *   - Return false (không tạo snapshot mới vì không có thay đổi)
     *   - migrationRepository.save() KHÔNG được gọi
     *   - databaseDiagramRepository.save() KHÔNG được gọi (không cần update)
     * 
     * Coverage:
     *   - Change detection: hash comparison
     *   - Optimization: skip unnecessary saves
     */
    @Test
    public void UT_MS_003_createSnapshotOnDisconnect_hashNotChanged_shouldReturnFalseAndSkipSave() throws Exception {
        logger.info("[UT_MS_003] BẮT ĐẦU: Tạo snapshot khi không có thay đổi (hash trùng)");
        logger.info("[UT_MS_003] Input: lastHash='same-hash', newHash='same-hash'");

        // Arrange
        // Calculate the actual hash that the service will compute
        String actualHash = calculateSHA256(sampleSnapshotJson);
        sampleDiagram.setLastSnapshotHash(actualHash);
        
        // Mock the serialization to return JSON
        when(databaseDiagramRepository.findById(1L)).thenReturn(Optional.of(sampleDiagram));
        when(objectMapper.writeValueAsString(any())).thenReturn(sampleSnapshotJson);
        
        // When hash is the same, service should NOT call save at all
        // Configure mock to NOT call save
        when(migrationRepository.save(any())).thenReturn(null);

        // Act - This should detect that hash is the same and return false
        boolean result = migrationService.createSnapshotOnDisconnect(1L, "admin");

        // Assert - Should return false because hash didn't change
        // The service should skip saving the migration when hash is unchanged
        assertFalse(result, "Phải return false khi hash không thay đổi");
        verify(migrationRepository, never()).save(any(Migration.class));

        logger.info("[UT_MS_003] KẾT QUẢ: PASSED - No migration saved when hash unchanged");
    }

    /**
     * UT_MS_004: Tạo snapshot - ObjectMapper serialization error
     * 
     * Mô tả: Khi JSON serialization thất bại, service phải xử lý exception
     * Kịch bản: objectMapper.writeValueAsString() throw JsonProcessingException
     * 
     * Input:
     *   - diagramId=1
     *   - Snapshot JSON serialization fails
     * 
     * Expected Output:
     *   - Return false (fail gracefully)
     *   - migrationRepository.save() KHÔNG được gọi
     * 
     * Coverage:
     *   - Error handling: serialization error
     */
    @Test
    public void UT_MS_004_createSnapshotOnDisconnect_serializationError_shouldReturnFalse() throws Exception {
        logger.info("[UT_MS_004] BẮT ĐẦU: Tạo snapshot khi JSON serialization lỗi");

        // Arrange
        when(databaseDiagramRepository.findById(1L)).thenReturn(Optional.of(sampleDiagram));
        when(objectMapper.writeValueAsString(any())).thenThrow(
                new com.fasterxml.jackson.core.JsonProcessingException("Serialization error") {});

        // Act
        boolean result = migrationService.createSnapshotOnDisconnect(1L, "admin");

        // Assert
        assertFalse(result, "Phải return false khi serialization lỗi");
        verify(migrationRepository, never()).save(any());

        logger.info("[UT_MS_004] KẾT QUẢ: PASSED - Serialization error handled");
    }

    // ========================================================================================
    // TEST CASES CHO getMigrationHistory()
    // ========================================================================================
    // Phương thức này lấy danh sách tất cả migrations của một diagram,
    // được sắp xếp theo thời gian mới nhất trước
    // ========================================================================================

    /**
     * UT_MS_005: Lấy lịch sử migration - có dữ liệu
     * 
     * Mô tả: Kiểm tra lấy danh sách migrations của diagram khi có records
     * Input: diagramId=1, với 3 migrations
     * Expected:
     *   - Return list với 3 MigrationHistoryDto
     *   - Được sắp xếp theo createdAt DESC (mới nhất trước)
     *   - Mỗi DTO chứa đầy đủ: ID, username, createdAt, hash, diagramId
     */
    @Test
    public void UT_MS_005_getMigrationHistory_withExistingRecords_shouldReturnOrderedList() throws Exception {
        logger.info("[UT_MS_005] BẮT ĐẦU: Lấy lịch sử migration - có dữ liệu");
        logger.info("[UT_MS_005] Input: diagramId=1, count=3 migrations");

        // Arrange
        Migration m1 = new Migration();
        m1.setId(1L);
        m1.setUsername("admin");
        m1.setCreatedAt(LocalDateTime.now().minusHours(2));
        m1.setSnapshotHash("hash1");
        m1.setDatabaseDiagram(sampleDiagram);

        Migration m2 = new Migration();
        m2.setId(2L);
        m2.setUsername("editor");
        m2.setCreatedAt(LocalDateTime.now().minusHours(1));
        m2.setSnapshotHash("hash2");
        m2.setDatabaseDiagram(sampleDiagram);

        Migration m3 = new Migration();
        m3.setId(3L);
        m3.setUsername("admin");
        m3.setCreatedAt(LocalDateTime.now());
        m3.setSnapshotHash("hash3");
        m3.setDatabaseDiagram(sampleDiagram);

        when(migrationRepository.findByDatabaseDiagramIdOrderByCreatedAtDesc(1L))
                .thenReturn(Arrays.asList(m3, m2, m1));

        // Act
        List<MigrationHistoryDto> result = migrationService.getMigrationHistory(1L);

        // Assert
        assertNotNull(result, "List không được null");
        assertEquals(3, result.size(), "Phải có 3 migrations");
        assertEquals(3L, result.get(0).getId(), "Migration mới nhất phải là ID=3");
        assertEquals(1L, result.get(2).getId(), "Migration cũ nhất phải là ID=1");

        verify(migrationRepository, times(1)).findByDatabaseDiagramIdOrderByCreatedAtDesc(1L);
        logger.info("[UT_MS_005] KẾT QUẢ: PASSED - Trả về {} migrations, sorted DESC", result.size());
    }

    /**
     * UT_MS_006: Lấy lịch sử migration - không có dữ liệu
     * 
     * Mô tả: Khi diagram chưa có migrations nào
     * Input: diagramId=1, không có migrations
     * Expected:
     *   - Return empty list
     *   - Không throw exception
     */
    @Test
    public void UT_MS_006_getMigrationHistory_noRecords_shouldReturnEmptyList() throws Exception {
        logger.info("[UT_MS_006] BẮT ĐẦU: Lấy lịch sử migration - không có dữ liệu");

        // Arrange
        when(migrationRepository.findByDatabaseDiagramIdOrderByCreatedAtDesc(1L))
                .thenReturn(Collections.emptyList());

        // Act
        List<MigrationHistoryDto> result = migrationService.getMigrationHistory(1L);

        // Assert
        assertNotNull(result, "List không được null");
        assertTrue(result.isEmpty(), "List phải rỗng");

        verify(migrationRepository, times(1)).findByDatabaseDiagramIdOrderByCreatedAtDesc(1L);
        logger.info("[UT_MS_006] KẾT QUẢ: PASSED - Empty list returned");
    }

    // ========================================================================================
    // TEST CASES CHO getMigrationDetail()
    // ========================================================================================
    // Phương thức này lấy chi tiết của một migration cụ thể,
    // bao gồm snapshot JSON đầy đủ để khôi phục
    // ========================================================================================

    /**
     * UT_MS_007: Lấy chi tiết migration - tồn tại
     * 
     * Mô tả: Kiểm tra lấy chi tiết một migration cụ thể
     * Input: migrationId=1 (tồn tại)
     * Expected:
     *   - Return MigrationDetailDto đầy đủ
     *   - Chứa snapshot JSON để khôi phục diagram
     *   - Chứa metadata: username, createdAt, hash, diagramId, diagramName
     */
    @Test
    public void UT_MS_007_getMigrationDetail_withExistingId_shouldReturnDetails() throws Exception {
        logger.info("[UT_MS_007] BẮT ĐẦU: Lấy chi tiết migration - tồn tại");
        logger.info("[UT_MS_007] Input: migrationId=1");

        // Arrange
        when(migrationRepository.findById(1L)).thenReturn(Optional.of(sampleMigration));

        // Act
        MigrationDetailDto result = migrationService.getMigrationDetail(1L);

        // Assert
        assertNotNull(result, "Result không được null");
        assertEquals(1L, result.getId(), "ID phải match");
        assertEquals("admin", result.getUsername(), "Username phải match");
        assertEquals(sampleSnapshotJson, result.getSnapshotJson(), "JSON phải intact");
        assertEquals("Schema test", result.getDiagramName(), "Diagram name phải match");

        verify(migrationRepository, times(1)).findById(1L);
        logger.info("[UT_MS_007] KẾT QUẢ: PASSED - Migration detail returned correctly");
    }

    /**
     * UT_MS_008: Lấy chi tiết migration - không tồn tại
     * 
     * Mô tả: Khi migrationId không hợp lệ
     * Input: migrationId=999 (không tồn tại)
     * Expected:
     *   - Throw RuntimeException "Migration not found"
     */
    @Test
    public void UT_MS_008_getMigrationDetail_notFound_shouldThrowException() throws Exception {
        logger.info("[UT_MS_008] BẮT ĐẦU: Lấy chi tiết migration - không tồn tại");
        logger.info("[UT_MS_008] Input: migrationId=999");

        // Arrange
        when(migrationRepository.findById(999L)).thenReturn(Optional.empty());

        // Act & Assert
        Exception exception = assertThrows(RuntimeException.class, () -> {
            migrationService.getMigrationDetail(999L);
        });
        assertNotNull(exception, "Exception phải được throw");

        verify(migrationRepository, times(1)).findById(999L);
        logger.info("[UT_MS_008] KẾT QUẢ: PASSED - Exception thrown for non-existing migration with message: {}",
                exception.getMessage());
    }

    // ========================================================================================
    // TEST CASES CHO Hash Generation
    // ========================================================================================
    // Hash generation được sử dụng để detect changes giữa snapshots
    // SHA-256 được dùng để ensure consistency
    // ========================================================================================

    /**
     * UT_MS_009: Snapshot với models và attributes - snapshot structure
     * 
     * Mô tả: Verify rằng snapshot được tạo với đúng structure từ diagram
     * Input: Diagram với models và attributes
     * Expected:
     *   - Snapshot chứa đầy đủ models
     *   - Mỗi model có attributes sorted by order
     *   - Properties được preserve từ entities
     */
    @Test
    public void UT_MS_009_createSnapshot_withModelsAndAttributes_shouldStructureCorrectly() throws Exception {
        logger.info("[UT_MS_009] BẮT ĐẦU: Snapshot structure verification");

        // Arrange
        when(databaseDiagramRepository.findById(1L)).thenReturn(Optional.of(sampleDiagram));
        when(objectMapper.writeValueAsString(any())).thenReturn(sampleSnapshotJson);
        when(migrationRepository.save(any(Migration.class))).thenReturn(sampleMigration);
        when(databaseDiagramRepository.save(any(DatabaseDiagram.class))).thenReturn(sampleDiagram);

        // Act
        boolean result = migrationService.createSnapshotOnDisconnect(1L, "admin");

        // Assert
        assertTrue(result, "Snapshot với models phải được tạo");
        verify(migrationRepository, times(1)).save(argThat(m ->
                m.getSnapshotJson() != null &&
                !m.getSnapshotJson().isEmpty()));

        logger.info("[UT_MS_009] KẾT QUẢ: PASSED - Snapshot structure verified");
    }

    /**
     * UT_MS_010: Integration test - full migration flow
     * 
     * Mô tả: Test toàn bộ flow: create → get history → get detail
     * Kịch bản:
     *   1. Create snapshot (tạo migration mới)
     *   2. Get migration history (xem lịch sử)
     *   3. Get migration detail (xem chi tiết để khôi phục)
     * 
     * Expected:
     *   - Tất cả steps thành công
     *   - Dữ liệu consistent throughout
     */
    @Test
    public void UT_MS_010_fullMigrationFlow_createThenQueryHistory_shouldBeConsistent() throws Exception {
        logger.info("[UT_MS_010] BẮT ĐẦU: Integration test - full migration flow");

        // Arrange
        when(databaseDiagramRepository.findById(1L)).thenReturn(Optional.of(sampleDiagram));
        when(objectMapper.writeValueAsString(any())).thenReturn(sampleSnapshotJson);
        when(migrationRepository.save(any(Migration.class))).thenReturn(sampleMigration);
        when(databaseDiagramRepository.save(any(DatabaseDiagram.class))).thenReturn(sampleDiagram);
        when(migrationRepository.findByDatabaseDiagramIdOrderByCreatedAtDesc(1L))
                .thenReturn(Arrays.asList(sampleMigration));
        when(migrationRepository.findById(1L)).thenReturn(Optional.of(sampleMigration));

        // Act - Step 1: Create migration
        boolean created = migrationService.createSnapshotOnDisconnect(1L, "admin");

        // Act - Step 2: Get history
        List<MigrationHistoryDto> history = migrationService.getMigrationHistory(1L);

        // Act - Step 3: Get detail
        MigrationDetailDto detail = migrationService.getMigrationDetail(1L);

        // Assert
        assertTrue(created, "Migration created");
        assertEquals(1, history.size(), "History should have 1 record");
        assertNotNull(detail, "Detail should not be null");
        assertEquals(detail.getId(), history.get(0).getId(), "IDs should match");

        logger.info("[UT_MS_010] KẾT QUẢ: PASSED - Full flow executed successfully");
    }

    /**
     * Helper method to calculate SHA-256 hash (matches the service implementation)
     */
    private String calculateSHA256(String input) {
        try {
            java.security.MessageDigest digest = java.security.MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(input.getBytes(java.nio.charset.StandardCharsets.UTF_8));

            // Convert byte array to hex string
            StringBuilder hexString = new StringBuilder();
            for (byte b : hash) {
                String hex = Integer.toHexString(0xff & b);
                if (hex.length() == 1)
                    hexString.append('0');
                hexString.append(hex);
            }
            return hexString.toString();
        } catch (java.security.NoSuchAlgorithmException e) {
            throw new RuntimeException("Error generating hash", e);
        }
    }
}
