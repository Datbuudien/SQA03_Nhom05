package com.example.react_flow_be.service;

import org.junit.jupiter.api.AfterEach;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.argThat;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.reset;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import org.mockito.junit.jupiter.MockitoExtension;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.example.react_flow_be.entity.Attribute;
import com.example.react_flow_be.entity.DatabaseDiagram;
import com.example.react_flow_be.entity.Model;
import com.example.react_flow_be.repository.AttributeRepository;
import com.example.react_flow_be.repository.ConnectionRepository;
import com.example.react_flow_be.repository.DatabaseDiagramRepository;
import com.example.react_flow_be.repository.ModelRepository;
import com.fasterxml.jackson.databind.ObjectMapper;

/**
 * ============================================================================
 * Unit Test cho DiagramImportService - Tầng Service quản lý Import Sơ đồ
 * ============================================================================
 * Mô tả: Test các phương thức import JSON diagram, xử lý models, attributes,
 *        và connections từ file JSON vào hệ thống
 * Phương pháp: Sử dụng Mockito để mock các Repository và Service dependency
 * Rollback: Sử dụng Mockito (không tương tác DB thật) nên không cần rollback DB.
 *           Mỗi test được reset mock objects qua @BeforeEach/@AfterEach.
 * Database: Khi test lưu dữ liệu, phải verify:
 *           1. Status code: Phương thức save() được gọi đúng số lần
 *           2. Database persistence: Mock repository được verify với correct parameters
 *           3. Return value: Kiểm tra ID diagram được trả về đúng
 * ============================================================================
 */
@ExtendWith(MockitoExtension.class)
public class DiagramImportServiceTest {

    private static final Logger logger = LoggerFactory.getLogger(DiagramImportServiceTest.class);

    @Mock
    private DatabaseDiagramRepository databaseDiagramRepository;

    @Mock
    private ModelRepository modelRepository;

    @Mock
    private AttributeRepository attributeRepository;

    @Mock
    private ConnectionRepository connectionRepository;

    @Mock
    private CollaborationService collaborationService;

    @Mock
    private ObjectMapper objectMapper;

    @InjectMocks
    private DiagramImportService diagramImportService;

    private DatabaseDiagram sampleDatabaseDiagram;
    private Model sampleModel;
    private Attribute sampleAttribute;
    private String validJsonContent;

    @BeforeEach
    public void setUp() {
        logger.info("========================================");
        logger.info("[SETUP] Khởi tạo dữ liệu test cho DiagramImportService...");

        // Khởi tạo DatabaseDiagram mẫu với đầy đủ các thuộc tính
        // Được sử dụng làm giá trị mock trả về từ repository.save()
        sampleDatabaseDiagram = new DatabaseDiagram();
        sampleDatabaseDiagram.setId(1L);
        sampleDatabaseDiagram.setName("Sample Database Schema");
        sampleDatabaseDiagram.setDatabaseType(DatabaseDiagram.DatabaseType.MYSQL);
        sampleDatabaseDiagram.setVersion("8.0");
        sampleDatabaseDiagram.setCharset("utf8mb4");
        sampleDatabaseDiagram.setCollation("utf8mb4_unicode_ci");
        sampleDatabaseDiagram.setIsPublic(false);
        sampleDatabaseDiagram.setIsTemplate(false);
        sampleDatabaseDiagram.setZoomLevel(1.0);
        sampleDatabaseDiagram.setPanX(0.0);
        sampleDatabaseDiagram.setPanY(0.0);

        // Khởi tạo Model mẫu - đại diện cho một bảng trong schema
        // Model được liên kết với DatabaseDiagram
        sampleModel = new Model();
        sampleModel.setId("model-001");
        sampleModel.setName("Users");
        sampleModel.setNodeId("Users");
        sampleModel.setPositionX(100.0);
        sampleModel.setPositionY(100.0);
        sampleModel.setDatabaseDiagram(sampleDatabaseDiagram);

        // Khởi tạo Attribute mẫu - đại diện cho một cột trong bảng
        // Attribute được liên kết với Model
        sampleAttribute = new Attribute();
        sampleAttribute.setId("attr-001");
        sampleAttribute.setName("user_id");
        sampleAttribute.setDataType("INT");
        sampleAttribute.setLength(20);
        sampleAttribute.setIsNullable(false);
        sampleAttribute.setIsPrimaryKey(true);
        sampleAttribute.setAttributeOrder(0);
        sampleAttribute.setModel(sampleModel);
        sampleAttribute.setHasIndex(true);
        sampleAttribute.setIndexType(Attribute.IndexType.PRIMARY);

        // JSON hợp lệ mô phỏng dữ liệu import từ client
        // Cấu trúc: schema -> tables -> attributes
        validJsonContent = """
                {
                  "schema": {"description": "Database schema"},
                  "tables": [
                    {
                      "id": "table-001",
                      "name": "Users",
                      "position": {"x": 100, "y": 100},
                      "attributes": [
                        {
                          "id": "attr-001",
                          "name": "user_id",
                          "dataType": "INT",
                          "isNullable": false,
                          "isPrimaryKey": true
                        }
                      ]
                    }
                  ]
                }
                """;

        logger.info("[SETUP] Hoàn tất khởi tạo dữ liệu test.");
    }

    @AfterEach
    public void tearDown() {
        logger.info("[TEARDOWN] Dọn dẹp dữ liệu test...");
        
        // Reset tất cả mock objects về trạng thái ban đầu
        // Đảm bảo test tiếp theo không bị ảnh hưởng từ test hiện tại
        // Mockito.reset() xóa các stubbing và verification history
        reset(databaseDiagramRepository, modelRepository, attributeRepository,
                connectionRepository, collaborationService);
        
        logger.info("[TEARDOWN] Hoàn tất dọn dẹp.");
        logger.info("========================================\n");
    }

    // ========================================================================================
    // TEST CASES: Kiểm tra import diagram từ JSON hợp lệ
    // ========================================================================================
    // Các test case trong phần này kiểm tra chức năng cốt lõi của DiagramImportService:
    // - Import JSON diagram và lưu vào database
    // - Tạo các relationship giữa DatabaseDiagram, Model, và Attribute
    // - Kiểm tra return value, database persistence, và collaboration creation
    // ========================================================================================

    /**
     * UT_DI_001: Import diagram từ JSON hợp lệ - tạo thành công
     * 
     * Mô tả: Kiểm tra import diagram từ JSON content hợp lệ với 1 table
     * Kịch bản: 
     *   - User cung cấp tên diagram, JSON content hợp lệ, và username
     *   - Service parse JSON, tạo DatabaseDiagram, Model, Attribute
     *   - Lưu tất cả vào database thông qua repository mock
     * 
     * Input: 
     *   - diagramName="Sales DB" (tên diagram từ request)
     *   - jsonContent=validJSON (JSON data chuẩn)
     *   - username="admin" (người tạo diagram)
     * 
     * Expected Output:
     *   - Return diagram ID = 1L
     *   - databaseDiagramRepository.save() được gọi đúng 1 lần
     *   - Repository verify nhận đúng diagram với name="Sales DB" và type=MYSQL
     *   - collaborationService.createOwner(1L, "admin") được gọi để tạo ownership
     * 
     * Coverage:
     *   - Kiểm tra method flow hoàn chỉnh
     *   - Kiểm tra return value match với saved diagram ID
     *   - Kiểm tra collaboration created correctly
     */
    @Test
    public void UT_DI_001_importDiagramFromJson_withValidData_shouldReturnDiagramId() throws Exception {
        logger.info("[UT_DI_001] BẮT ĐẦU: Import diagram từ JSON hợp lệ");
        logger.info("[UT_DI_001] Input: diagramName='Sales DB', jsonContent=valid, username='admin'");

        // Arrange: Setup mock behavior
        ObjectMapper realMapper = new ObjectMapper();
        when(objectMapper.readTree(validJsonContent))
                .thenReturn(realMapper.readTree(validJsonContent));
        when(databaseDiagramRepository.save(any(DatabaseDiagram.class)))
                .thenReturn(sampleDatabaseDiagram);
        when(modelRepository.save(any(Model.class)))
                .thenReturn(sampleModel);
        when(attributeRepository.save(any(Attribute.class)))
                .thenReturn(sampleAttribute);
        when(collaborationService.createOwner(1L, "admin")).thenReturn(null);

        // Act: Gọi service method
        Long result = diagramImportService.importDiagramFromJson("Sales DB", validJsonContent, "admin");

        // Assert: Verify kết quả
        // 1. Check return value không null
        assertNotNull(result, "Diagram ID không được null");
        
        // 2. Check return value bằng expected ID
        assertEquals(1L, result, "ID diagram phải bằng 1L");
        
        // 3. Verify repository được gọi với đúng parameters
        verify(databaseDiagramRepository, times(1)).save(argThat(diagram ->
                diagram.getName().equals("Sales DB") &&
                diagram.getDatabaseType() == DatabaseDiagram.DatabaseType.MYSQL));
        
        // 4. Verify collaboration service created ownership
        verify(collaborationService, times(1)).createOwner(1L, "admin");

        logger.info("[UT_DI_001] KẾT QUẢ: PASSED - Diagram được import thành công, ID={}, owner='admin'", result);
    }

    /**
     * UT_DI_002: Import diagram với tên diagram null
     * 
     * Mô tả: Kiểm tra hành vi khi tên diagram không được cung cấp (null)
     * Kịch bản: Service phải xử lý gracefully khi diagramName=null
     * 
     * Input:
     *   - diagramName=null (người dùng không cung cấp tên)
     *   - jsonContent=validJSON (JSON hợp lệ)
     * 
     * Expected Output:
     *   - Diagram vẫn được tạo (validation ở tầng controller)
     *   - databaseDiagramRepository.save() được gọi 1 lần
     *   - Return diagram ID khác null
     * 
     * Coverage:
     *   - Boundary case: null input handling
     *   - Đảm bảo service không crash khi receive null
     */
    @Test
    public void UT_DI_002_importDiagramFromJson_withNullDiagramName_shouldStillImport() throws Exception {
        logger.info("[UT_DI_002] BẮT ĐẦU: Import diagram với tên null");
        logger.info("[UT_DI_002] Input: diagramName=null, jsonContent=valid");

        // Arrange
        ObjectMapper realMapper = new ObjectMapper();
        when(objectMapper.readTree(validJsonContent))
                .thenReturn(realMapper.readTree(validJsonContent));
        when(databaseDiagramRepository.save(any(DatabaseDiagram.class)))
                .thenReturn(sampleDatabaseDiagram);
        when(modelRepository.save(any(Model.class)))
                .thenReturn(sampleModel);
        when(attributeRepository.save(any(Attribute.class)))
                .thenReturn(sampleAttribute);
        when(collaborationService.createOwner(anyLong(), anyString())).thenReturn(null);

        // Act
        Long result = diagramImportService.importDiagramFromJson(null, validJsonContent, "admin");

        // Assert
        assertNotNull(result, "Diagram vẫn phải được tạo dù name null");
        verify(databaseDiagramRepository, times(1)).save(any(DatabaseDiagram.class));

        logger.info("[UT_DI_002] KẾT QUẢ: PASSED - Diagram null name được import, ID={}", result);
    }

    /**
     * UT_DI_003: Import diagram - verify return value matches saved diagram ID
     * 
     * Mô tả: Kiểm tra return value trả về đúng ID của diagram vừa được save
     * Kịch bản: Verify rằng service trả về diagram ID từ repository
     * 
     * Input:
     *   - diagramName="Test" (tên diagram)
     *   - jsonContent=validJSON (JSON hợp lệ)
     * 
     * Expected Output:
     *   - Diagram được save với ID=12345L
     *   - Service trả về đúng ID=12345L (không phải ID khác)
     * 
     * Coverage:
     *   - Đảm bảo return value mapping đúng từ saved entity
     *   - Kiểm tra ID không bị mất/thay đổi trong quá trình xử lý
     */
    @Test
    public void UT_DI_003_importDiagramFromJson_returnValue_shouldMatch() throws Exception {
        logger.info("[UT_DI_003] BẮT ĐẦU: Return value verification");
        logger.info("[UT_DI_003] Input: diagramName='Test', jsonContent=valid");

        // Arrange - Setup với specific diagram ID
        ObjectMapper realMapper = new ObjectMapper();
        when(objectMapper.readTree(validJsonContent))
                .thenReturn(realMapper.readTree(validJsonContent));
        DatabaseDiagram savedDiagram = new DatabaseDiagram();
        savedDiagram.setId(12345L);  // ID không mặc định, verify mapping đúng
        when(databaseDiagramRepository.save(any(DatabaseDiagram.class)))
                .thenReturn(savedDiagram);
        when(modelRepository.save(any(Model.class)))
                .thenReturn(sampleModel);
        when(attributeRepository.save(any(Attribute.class)))
                .thenReturn(sampleAttribute);
        when(collaborationService.createOwner(anyLong(), anyString())).thenReturn(null);

        // Act
        Long result = diagramImportService.importDiagramFromJson("Test", validJsonContent, "admin");

        // Assert
        assertNotNull(result, "Result không được null");
        assertEquals(12345L, result, "Return value phải match với saved diagram ID");

        logger.info("[UT_DI_003] KẾT QUẢ: PASSED - Return value match with diagram ID={}",
                result);
    }

    /**
     * UT_DI_004: Import diagram - verify database diagram properties
     * Mô tả: Kiểm tra tất cả diagram properties được set đúng
     * Input: Valid JSON
     * Expected: Diagram được save với properties đúng
     */
    @Test
    public void UT_DI_004_importDiagramFromJson_diagramProperties_shouldSaveCorrectly() throws Exception {
        logger.info("[UT_DI_004] BẮT ĐẦU: Database diagram properties verification");

        ObjectMapper realMapper = new ObjectMapper();
        when(objectMapper.readTree(validJsonContent))
                .thenReturn(realMapper.readTree(validJsonContent));
        when(databaseDiagramRepository.save(any(DatabaseDiagram.class)))
                .thenReturn(sampleDatabaseDiagram);
        when(modelRepository.save(any(Model.class)))
                .thenReturn(sampleModel);
        when(attributeRepository.save(any(Attribute.class)))
                .thenReturn(sampleAttribute);
        when(collaborationService.createOwner(anyLong(), anyString())).thenReturn(null);

        Long result = diagramImportService.importDiagramFromJson("Sales DB", validJsonContent, "admin");

        assertNotNull(result);
        verify(databaseDiagramRepository).save(argThat(diagram ->
                diagram.getName().equals("Sales DB") &&
                diagram.getDatabaseType() == DatabaseDiagram.DatabaseType.MYSQL &&
                diagram.getVersion().equals("8.0") &&
                diagram.getCharset().equals("utf8mb4") &&
                diagram.getIsPublic() == false));

        logger.info("[UT_DI_004] KẾT QUẢ: PASSED - Diagram properties saved correctly");
    }

    /**
     * UT_DI_005: Import diagram - verify collaboration created
     * Mô tả: Kiểm tra owner collaboration được tạo
     * Input: Valid JSON, username="lecturer01"
     * Expected: collaborationService.createOwner() được gọi
     */
    @Test
    public void UT_DI_005_importDiagramFromJson_collaboration_shouldBeCreated() throws Exception {
        logger.info("[UT_DI_005] BẮT ĐẦU: Collaboration creation verification");

        ObjectMapper realMapper = new ObjectMapper();
        when(objectMapper.readTree(validJsonContent))
                .thenReturn(realMapper.readTree(validJsonContent));
        when(databaseDiagramRepository.save(any(DatabaseDiagram.class)))
                .thenReturn(sampleDatabaseDiagram);
        when(modelRepository.save(any(Model.class)))
                .thenReturn(sampleModel);
        when(attributeRepository.save(any(Attribute.class)))
                .thenReturn(sampleAttribute);
        when(collaborationService.createOwner(1L, "lecturer01")).thenReturn(null);

        Long result = diagramImportService.importDiagramFromJson("Test", validJsonContent, "lecturer01");

        assertNotNull(result);
        verify(collaborationService, times(1)).createOwner(1L, "lecturer01");

        logger.info("[UT_DI_005] KẾT QUẢ: PASSED - Collaboration created successfully");
    }

    /**
     * UT_DI_006: Import diagram - model association with diagram
     * Mô tả: Kiểm tra model được liên kết đúng với diagram
     * Input: 1 table trong JSON
     * Expected: model.databaseDiagram được set bằng saved diagram
     */
    @Test
    public void UT_DI_006_importDiagramFromJson_modelAssociation_shouldLink() throws Exception {
        logger.info("[UT_DI_006] BẮT ĐẦU: Model association verification");

        ObjectMapper realMapper = new ObjectMapper();
        when(objectMapper.readTree(validJsonContent))
                .thenReturn(realMapper.readTree(validJsonContent));
        when(databaseDiagramRepository.save(any(DatabaseDiagram.class)))
                .thenReturn(sampleDatabaseDiagram);
        when(modelRepository.save(any(Model.class)))
                .thenReturn(sampleModel);
        when(attributeRepository.save(any(Attribute.class)))
                .thenReturn(sampleAttribute);
        when(collaborationService.createOwner(anyLong(), anyString())).thenReturn(null);

        Long result = diagramImportService.importDiagramFromJson("Test", validJsonContent, "admin");

        assertNotNull(result);
        verify(modelRepository).save(argThat(model ->
                model.getDatabaseDiagram() != null &&
                model.getDatabaseDiagram().getId() == 1L));

        logger.info("[UT_DI_006] KẾT QUẢ: PASSED - Model linked correctly");
    }

    /**
     * UT_DI_007: Import diagram - attribute association with model
     * Mô tả: Kiểm tra attribute được liên kết đúng với model
     * Input: 1 attribute trong 1 table
     * Expected: attribute.model được set bằng created model
     */
    @Test
    public void UT_DI_007_importDiagramFromJson_attributeAssociation_shouldLink() throws Exception {
        logger.info("[UT_DI_007] BẮT ĐẦU: Attribute association verification");

        ObjectMapper realMapper = new ObjectMapper();
        when(objectMapper.readTree(validJsonContent))
                .thenReturn(realMapper.readTree(validJsonContent));
        when(databaseDiagramRepository.save(any(DatabaseDiagram.class)))
                .thenReturn(sampleDatabaseDiagram);
        when(modelRepository.save(any(Model.class)))
                .thenReturn(sampleModel);
        when(attributeRepository.save(any(Attribute.class)))
                .thenReturn(sampleAttribute);
        when(collaborationService.createOwner(anyLong(), anyString())).thenReturn(null);

        Long result = diagramImportService.importDiagramFromJson("Test", validJsonContent, "admin");

        assertNotNull(result);
        verify(attributeRepository).save(argThat(attr ->
                attr.getModel() != null &&
                attr.getModel().getName().equals("Users")));

        logger.info("[UT_DI_007] KẾT QUẢ: PASSED - Attribute linked correctly");
    }

    /**
     * UT_DI_008: Import diagram - malformed JSON should throw exception
     * Mô tả: Kiểm tra xử lý khi JSON không hợp lệ
     * Input: malformed JSON
     * Expected: Exception thrown
     */
    @Test
    public void UT_DI_008_importDiagramFromJson_invalidJson_shouldThrow() throws Exception {
        logger.info("[UT_DI_008] BẮT ĐẦU: Invalid JSON handling");

        String invalidJson = "{ invalid }";
        when(objectMapper.readTree(invalidJson))
                .thenThrow(new RuntimeException("Invalid JSON"));

        assertThrows(Exception.class, () -> {
            diagramImportService.importDiagramFromJson("Test", invalidJson, "admin");
        });

        logger.info("[UT_DI_008] KẾT QUẢ: PASSED - Exception thrown for invalid JSON");
    }

    /**
     * UT_DI_009: Import diagram - empty JSON should handle gracefully
     * Mô tả: Kiểm tra xử lý khi JSON rỗng
     * Input: empty JSON object
     * Expected: Diagram created without errors
     */
    @Test
    public void UT_DI_009_importDiagramFromJson_emptyJson_shouldHandle() throws Exception {
        logger.info("[UT_DI_009] BẮT ĐẦU: Empty JSON handling");

        String emptyJson = "{}";
        ObjectMapper realMapper = new ObjectMapper();
        when(objectMapper.readTree(emptyJson))
                .thenReturn(realMapper.readTree(emptyJson));
        when(databaseDiagramRepository.save(any(DatabaseDiagram.class)))
                .thenReturn(sampleDatabaseDiagram);
        when(collaborationService.createOwner(anyLong(), anyString())).thenReturn(null);

        Long result = diagramImportService.importDiagramFromJson("Test", emptyJson, "admin");

        assertNotNull(result);
        verify(databaseDiagramRepository, times(1)).save(any(DatabaseDiagram.class));

        logger.info("[UT_DI_009] KẾT QUẢ: PASSED - Empty JSON handled gracefully");
    }

    /**
     * UT_DI_010: Import diagram - verify model ID is UUID (not from JSON)
     * Mô tả: Kiểm tra model được tạo với UUID mới
     * Input: JSON chứa model với ID="table-001"
     * Expected: Model được save với UUID mới
     */
    @Test
    public void UT_DI_010_importDiagramFromJson_modelUUID_shouldGenerate() throws Exception {
        logger.info("[UT_DI_010] BẮT ĐẦU: Model UUID generation verification");

        ObjectMapper realMapper = new ObjectMapper();
        when(objectMapper.readTree(validJsonContent))
                .thenReturn(realMapper.readTree(validJsonContent));
        when(databaseDiagramRepository.save(any(DatabaseDiagram.class)))
                .thenReturn(sampleDatabaseDiagram);
        when(modelRepository.save(any(Model.class)))
                .thenAnswer(invocation -> {
                    Model m = invocation.getArgument(0);
                    assertNotNull(m.getId());
                    assertTrue(m.getId().matches("^[0-9a-f]{8}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{12}$"));
                    return m;
                });
        when(attributeRepository.save(any(Attribute.class)))
                .thenReturn(sampleAttribute);
        when(collaborationService.createOwner(anyLong(), anyString())).thenReturn(null);

        Long result = diagramImportService.importDiagramFromJson("Test", validJsonContent, "admin");

        assertNotNull(result);
        verify(modelRepository).save(argThat(model -> !model.getId().equals("table-001")));

        logger.info("[UT_DI_010] KẾT QUẢ: PASSED - Model created with new UUID");
    }
}
