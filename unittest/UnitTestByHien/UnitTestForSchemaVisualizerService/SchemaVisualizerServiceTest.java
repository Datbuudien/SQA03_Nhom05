package com.example.react_flow_be.service;

import com.example.react_flow_be.dto.DatabaseDiagramDto;
import com.example.react_flow_be.dto.ModelDto;
import com.example.react_flow_be.dto.collaboration.CollaborationDTO;
import com.example.react_flow_be.entity.Attribute;
import com.example.react_flow_be.entity.Collaboration;
import com.example.react_flow_be.entity.DatabaseDiagram;
import com.example.react_flow_be.entity.Model;
import com.example.react_flow_be.repository.AttributeRepository;
import com.example.react_flow_be.repository.ConnectionRepository;
import com.example.react_flow_be.repository.DatabaseDiagramRepository;
import com.example.react_flow_be.repository.ModelRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("SchemaVisualizerService Unit Tests - Branch Coverage Level 2")
class SchemaVisualizerServiceTest {

    @Mock
    private DatabaseDiagramService mockDatabaseDiagramService;
    @Mock
    private CollaborationService mockCollaborationService;
    @Mock
    private ModelService mockModelService;
    @Mock
    private AttributeService mockAttributeService;
    @Mock
    private ConnectionService mockConnectionService;
    @Mock
    private DatabaseDiagramRepository mockDatabaseDiagramRepository;
    @Mock
    private ModelRepository mockModelRepository;
    @Mock
    private AttributeRepository mockAttributeRepository;
    @Mock
    private ConnectionRepository mockConnectionRepository;

    @InjectMocks
    private SchemaVisualizerService serviceUnderTest;

    /**
     * TestCaseID: UN_SVS_001
     * Mục tiêu:
     * - Xác minh getSchemaData trả DTO hợp lệ khi diagram và quyền collaboration
     * tồn tại.
     * CheckDB:
     * - Verify luồng đọc dữ liệu qua service/repository phụ trợ.
     * Rollback:
     * - Unit test dùng mock, không thay đổi DB.
     */
    @Test
    void getSchemaData_shouldReturnDatabaseDiagramDto_whenDataExists() {
        DatabaseDiagram diagram = new DatabaseDiagram();
        diagram.setId(1L);
        diagram.setName("Diagram A");
        diagram.setDescription("Desc");
        diagram.setDatabaseType(DatabaseDiagram.DatabaseType.MYSQL);
        diagram.setVersion("8.0");
        diagram.setCharset("utf8mb4");
        diagram.setCollation("utf8mb4_unicode_ci");
        diagram.setIsPublic(false);
        diagram.setIsTemplate(false);
        diagram.setZoomLevel(1.0);
        diagram.setPanX(0.0);
        diagram.setPanY(0.0);

        Model model = new Model();
        model.setId("m1");
        diagram.setModels(List.of(model));

        ModelDto modelDto = new ModelDto("m1", "node1", "Model1", 10.0, 20.0, List.of());
        CollaborationDTO collaborationDTO = new CollaborationDTO(
                1L, "alice", Collaboration.CollaborationType.OWNER, Collaboration.Permission.EDIT, true, null, null);

        when(mockDatabaseDiagramService.getDatabaseDiagramById(1L)).thenReturn(diagram);
        when(mockModelService.convertToModelDto(model)).thenReturn(modelDto);
        when(mockCollaborationService.getUserCollaboration(1L, "alice")).thenReturn(collaborationDTO);

        DatabaseDiagramDto result = serviceUnderTest.getSchemaData(1L, "alice");

        assertEquals(1L, result.getId());
        assertEquals("Diagram A", result.getName());
        assertEquals(Collaboration.Permission.EDIT, result.getPermission());
        assertEquals(1, result.getModels().size());
    }

    /**
     * TestCaseID: UN_SVS_002
     * Mục tiêu:
     * - Xác minh initializeSampleData tạo sample và gọi tạo quan hệ FK khi
     * attribute tồn tại.
     * CheckDB:
     * - Verify đúng số lần gọi createModel/createAttribute/createConnection theo
     * flow.
     * Rollback:
     * - Unit test mock, không thay đổi DB thật.
     */
    @Test
    void initializeSampleData_shouldCreateSampleModelsAttributesAndConnections() {
        DatabaseDiagram diagram = new DatabaseDiagram();
        diagram.setId(10L);
        when(mockDatabaseDiagramService.createSampleDatabaseDiagram()).thenReturn(diagram);

        Model userModel = new Model();
        userModel.setId("u");
        userModel.setName("User");
        Model postModel = new Model();
        postModel.setId("p");
        postModel.setName("Post");
        Model commentModel = new Model();
        commentModel.setId("c");
        commentModel.setName("Comment");

        when(mockModelService.createModel(eq("User"), anyString(), anyDouble(), anyDouble(), anyBoolean(), eq(diagram)))
                .thenReturn(userModel);
        when(mockModelService.createModel(eq("Post"), anyString(), anyDouble(), anyDouble(), anyBoolean(), eq(diagram)))
                .thenReturn(postModel);
        when(mockModelService.createModel(eq("Comment"), anyString(), anyDouble(), anyDouble(), anyBoolean(),
                eq(diagram)))
                .thenReturn(commentModel);

        Attribute postUser = new Attribute();
        postUser.setId("11");
        Attribute commentPost = new Attribute();
        commentPost.setId("18");
        Attribute commentUser = new Attribute();
        commentUser.setId("19");
        Attribute commentParent = new Attribute();
        commentParent.setId("20");

        when(mockAttributeService.getAttributeByModelAndName(postModel, "user_id")).thenReturn(postUser);
        when(mockAttributeService.getAttributeByModelAndName(commentModel, "post_id")).thenReturn(commentPost);
        when(mockAttributeService.getAttributeByModelAndName(commentModel, "user_id")).thenReturn(commentUser);
        when(mockAttributeService.getAttributeByModelAndName(commentModel, "parent_comment_id"))
                .thenReturn(commentParent);

        serviceUnderTest.initializeSampleData();

        verify(mockModelService, times(3)).createModel(anyString(), anyString(), anyDouble(), anyDouble(), anyBoolean(),
                eq(diagram));
        verify(mockAttributeService, times(23)).createAttribute(anyString(), any(Model.class), anyString(), anyString(),
                anyBoolean(), anyInt(), anyBoolean(), anyBoolean());
        verify(mockConnectionService, times(4)).createConnection(any(Attribute.class), any(Model.class), anyString(),
                anyString(), anyString());
    }

    /**
     * TestCaseID: UN_SVS_003
     * Mục tiêu:
     * - Xác minh addModel trả id khi diagram tồn tại.
     * CheckDB:
     * - Verify truy vấn diagram và gọi createModel với tham số đúng.
     * Rollback:
     * - Unit test mock, không thay đổi DB.
     */
    @Test
    void addModel_shouldReturnModelId_whenDiagramExists() {
        DatabaseDiagram diagram = new DatabaseDiagram();
        diagram.setId(5L);
        Model createdModel = new Model();
        createdModel.setId("new-model-id");

        when(mockDatabaseDiagramRepository.findById(5L)).thenReturn(Optional.of(diagram));
        when(mockModelService.createModel("Order", "node-1", 100.0, 200.0, false, diagram)).thenReturn(createdModel);

        String result = serviceUnderTest.addModel("Order", 5L, "node-1", 100.0, 200.0);

        assertEquals("new-model-id", result);
    }

    /**
     * TestCaseID: UN_SVS_004
     * Mục tiêu:
     * - Xác minh addModel trả null khi diagram không tồn tại.
     * CheckDB:
     * - Verify không gọi createModel.
     * Rollback:
     * - Không có thay đổi DB.
     */
    @Test
    void addModel_shouldReturnNull_whenDiagramNotFound() {
        when(mockDatabaseDiagramRepository.findById(404L)).thenReturn(Optional.empty());

        String result = serviceUnderTest.addModel("Order", 404L, "node-1", 100.0, 200.0);

        assertNull(result);
        verify(mockModelService, never()).createModel(anyString(), anyString(), anyDouble(), anyDouble(), anyBoolean(),
                any());
    }

    /**
     * TestCaseID: UN_SVS_005
     * Mục tiêu:
     * - Xác minh updateModelName thành công khi timestamp mới hơn dữ liệu hiện tại.
     * CheckDB:
     * - Verify save(model) được gọi với dữ liệu name/nodeId/nameUpdatedAt mới.
     * Rollback:
     * - Unit test mock, không commit DB.
     */
    @Test
    void updateModelName_shouldReturnTrue_whenModelExistsAndTimestampIsNewer() {
        Model model = new Model();
        model.setId("m1");
        model.setName("Old Name");
        model.setNameUpdatedAt(LocalDateTime.of(2026, 5, 1, 10, 0));

        LocalDateTime newTimestamp = LocalDateTime.of(2026, 5, 2, 10, 0);

        when(mockModelRepository.findByIdForUpdate("m1")).thenReturn(Optional.of(model));

        boolean result = serviceUnderTest.updateModelName("m1", "New Name", newTimestamp);

        assertTrue(result);
        assertEquals("New Name", model.getName());
        assertEquals("New Name", model.getNodeId());
        assertEquals(newTimestamp, model.getNameUpdatedAt());
        verify(mockModelRepository, times(1)).save(model);
    }

    /**
     * TestCaseID: UN_SVS_006
     * Mục tiêu:
     * - Xác minh updateModelName trả false khi model không tồn tại.
     * CheckDB:
     * - Verify không gọi save.
     * Rollback:
     * - Không có thay đổi DB.
     */
    @Test
    void updateModelName_shouldReturnFalse_whenModelNotFound() {
        when(mockModelRepository.findByIdForUpdate("m-missing")).thenReturn(Optional.empty());

        boolean result = serviceUnderTest.updateModelName("m-missing", "Any Name", LocalDateTime.now());

        assertFalse(result);
        verify(mockModelRepository, never()).save(any(Model.class));
    }

    /**
     * TestCaseID: UN_SVS_007
     * Mục tiêu:
     * - Xác minh deleteModel trả true và gọi deleteById khi model tồn tại.
     * CheckDB:
     * - Verify findById + deleteById được gọi đúng modelId.
     * Rollback:
     * - Unit test mock, không thay đổi DB thật.
     */
    @Test
    void deleteModel_shouldReturnTrue_whenModelExists() {
        Model model = new Model();
        model.setId("m1");
        model.setName("User");
        when(mockModelRepository.findById("m1")).thenReturn(Optional.of(model));

        boolean result = serviceUnderTest.deleteModel("m1");

        assertTrue(result);
        verify(mockModelRepository, times(1)).deleteById("m1");
    }

    /**
     * TestCaseID: UN_SVS_008
     * Mục tiêu:
     * - Xác minh deleteModel trả false khi model không tồn tại.
     * CheckDB:
     * - Verify không gọi deleteById.
     * Rollback:
     * - Không có thay đổi DB.
     */
    @Test
    void deleteModel_shouldReturnFalse_whenModelNotFound() {
        when(mockModelRepository.findById("m-missing")).thenReturn(Optional.empty());

        boolean result = serviceUnderTest.deleteModel("m-missing");

        assertFalse(result);
        verify(mockModelRepository, never()).deleteById(anyString());
    }

    /**
     * TestCaseID: UN_SVS_009
     * Mục tiêu:
     * - Xác minh initializeSampleData không tạo connection khi các attribute FK
     * lookup trả null.
     * CheckDB:
     * - Verify createAttribute vẫn chạy đầy đủ nhưng createConnection không được
     * gọi.
     * Rollback:
     * - Unit test mock, không thay đổi DB.
     */
    @Test
    void initializeSampleData_shouldNotCreateConnections_whenForeignKeyAttributesAreNull() {
        DatabaseDiagram diagram = new DatabaseDiagram();
        diagram.setId(20L);
        when(mockDatabaseDiagramService.createSampleDatabaseDiagram()).thenReturn(diagram);

        Model userModel = new Model();
        userModel.setId("u2");
        userModel.setName("User");
        Model postModel = new Model();
        postModel.setId("p2");
        postModel.setName("Post");
        Model commentModel = new Model();
        commentModel.setId("c2");
        commentModel.setName("Comment");

        when(mockModelService.createModel(eq("User"), anyString(), anyDouble(), anyDouble(), anyBoolean(), eq(diagram)))
                .thenReturn(userModel);
        when(mockModelService.createModel(eq("Post"), anyString(), anyDouble(), anyDouble(), anyBoolean(), eq(diagram)))
                .thenReturn(postModel);
        when(mockModelService.createModel(eq("Comment"), anyString(), anyDouble(), anyDouble(), anyBoolean(),
                eq(diagram)))
                .thenReturn(commentModel);

        when(mockAttributeService.getAttributeByModelAndName(postModel, "user_id")).thenReturn(null);
        when(mockAttributeService.getAttributeByModelAndName(commentModel, "post_id")).thenReturn(null);
        when(mockAttributeService.getAttributeByModelAndName(commentModel, "user_id")).thenReturn(null);
        when(mockAttributeService.getAttributeByModelAndName(commentModel, "parent_comment_id")).thenReturn(null);

        serviceUnderTest.initializeSampleData();

        verify(mockAttributeService, times(23)).createAttribute(anyString(), any(Model.class), anyString(), anyString(),
                anyBoolean(), anyInt(), anyBoolean(), anyBoolean());
        verify(mockConnectionService, never()).createConnection(any(Attribute.class), any(Model.class), anyString(),
                anyString(),
                anyString());
    }

    /**
     * TestCaseID: UN_SVS_010
     * Mục tiêu:
     * - Xác minh addModel trả null khi xảy ra exception trong quá trình xử lý.
     * CheckDB:
     * - Verify exception được bắt và không ném ra ngoài.
     * Rollback:
     * - Không có thay đổi DB.
     */
    @Test
    void addModel_shouldReturnNull_whenUnexpectedExceptionOccurs() {
        when(mockDatabaseDiagramRepository.findById(7L)).thenThrow(new RuntimeException("Simulated repository error"));

        String result = serviceUnderTest.addModel("Order", 7L, "node-err", 1.0, 2.0);

        assertNull(result);
    }

    /**
     * TestCaseID: UN_SVS_011
     * Mục tiêu:
     * - Xác minh updateModelName trả false khi timestamp cũ hơn nameUpdatedAt hiện
     * tại.
     * CheckDB:
     * - Verify không gọi save khi dữ liệu cũ hơn.
     * Rollback:
     * - Không có thay đổi DB.
     */
    @Test
    void updateModelName_shouldReturnFalse_whenTimestampIsOlderThanCurrent() {
        Model model = new Model();
        model.setId("m2");
        model.setName("Current Name");
        model.setNameUpdatedAt(LocalDateTime.of(2026, 5, 3, 10, 0));

        LocalDateTime olderTimestamp = LocalDateTime.of(2026, 5, 2, 10, 0);
        when(mockModelRepository.findByIdForUpdate("m2")).thenReturn(Optional.of(model));

        boolean result = serviceUnderTest.updateModelName("m2", "Should Not Apply", olderTimestamp);

        assertFalse(result);
        verify(mockModelRepository, never()).save(any(Model.class));
    }

    /**
     * TestCaseID: UN_SVS_014
     * Mục tiêu:
     * - Xác minh updateModelName vẫn update thành công khi nameUpdatedAt hiện tại
     * là null.
     * CheckDB:
     * - Verify save được gọi với name/nodeId/timestamp mới.
     * Rollback:
     * - Unit test mock, không thay đổi DB.
     */
    @Test
    void updateModelName_shouldReturnTrue_whenCurrentNameUpdatedAtIsNull() {
        Model model = new Model();
        model.setId("m-null-ts");
        model.setName("Old");
        model.setNameUpdatedAt(null);

        LocalDateTime timestamp = LocalDateTime.of(2026, 5, 3, 12, 0);
        when(mockModelRepository.findByIdForUpdate("m-null-ts")).thenReturn(Optional.of(model));

        boolean result = serviceUnderTest.updateModelName("m-null-ts", "New", timestamp);

        assertTrue(result);
        assertEquals("New", model.getName());
        assertEquals("New", model.getNodeId());
        assertEquals(timestamp, model.getNameUpdatedAt());
        verify(mockModelRepository, times(1)).save(model);
    }

    /**
     * TestCaseID: UN_SVS_012
     * Mục tiêu:
     * - Xác minh updateModelName trả false khi phát sinh exception.
     * CheckDB:
     * - Verify exception bị bắt trong service và method trả false.
     * Rollback:
     * - Không có thay đổi DB.
     */
    @Test
    void updateModelName_shouldReturnFalse_whenUnexpectedExceptionOccurs() {
        when(mockModelRepository.findByIdForUpdate("m3")).thenThrow(new RuntimeException("Simulated lock failure"));

        boolean result = serviceUnderTest.updateModelName("m3", "Any", LocalDateTime.now());

        assertFalse(result);
    }

    /**
     * TestCaseID: UN_SVS_013
     * Mục tiêu:
     * - Xác minh deleteModel trả false khi xảy ra exception trong repository.
     * CheckDB:
     * - Verify exception được xử lý nội bộ và không ném ra ngoài.
     * Rollback:
     * - Không có thay đổi DB.
     */
    @Test
    void deleteModel_shouldReturnFalse_whenUnexpectedExceptionOccurs() {
        when(mockModelRepository.findById("m-error")).thenThrow(new RuntimeException("Simulated read failure"));

        boolean result = serviceUnderTest.deleteModel("m-error");

        assertFalse(result);
    }
}
