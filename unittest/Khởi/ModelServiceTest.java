package com.example.react_flow_be.service;

import com.example.react_flow_be.dto.AttributeDto;
import com.example.react_flow_be.dto.ModelDto;
import com.example.react_flow_be.entity.Attribute;
import com.example.react_flow_be.entity.DatabaseDiagram;
import com.example.react_flow_be.entity.Model;
import com.example.react_flow_be.repository.ModelRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ModelServiceTest {

    @Mock
    private ModelRepository modelRepository;

    @Mock
    private AttributeService attributeService;

    @InjectMocks
    private ModelService modelService;

    private DatabaseDiagram databaseDiagram;

    @BeforeEach
    void setUp() {
        databaseDiagram = new DatabaseDiagram();
        databaseDiagram.setId(10L);
        databaseDiagram.setName("Test Diagram");
    }

    // Test Case ID: UT_MS_001
    // Kiểm tra updateModelPosition() cập nhật thành công khi model chưa có positionUpdatedAt.
    @Test
    void testUpdateModelPositionSuccessWhenTimestampIsNull() {
        String modelId = "model-1";
        LocalDateTime timestamp = LocalDateTime.of(2026, 5, 2, 10, 0, 0);

        Model model = new Model();
        model.setId(modelId);
        model.setPositionX(1.0);
        model.setPositionY(2.0);
        model.setPositionUpdatedAt(null);

        when(modelRepository.findByIdForUpdate(modelId)).thenReturn(Optional.of(model));
        when(modelRepository.save(any(Model.class))).thenAnswer(invocation -> invocation.getArgument(0));

        boolean result = modelService.updateModelPosition(modelId, 11.5, 22.5, timestamp);

        assertTrue(result);

        ArgumentCaptor<Model> modelCaptor = ArgumentCaptor.forClass(Model.class);
        verify(modelRepository).save(modelCaptor.capture());
        Model savedModel = modelCaptor.getValue();
        assertEquals(11.5, savedModel.getPositionX());
        assertEquals(22.5, savedModel.getPositionY());
        assertEquals(timestamp, savedModel.getPositionUpdatedAt());
        verify(modelRepository).findByIdForUpdate(modelId);
        verifyNoMoreInteractions(modelRepository);
        verifyNoInteractions(attributeService);
    }

    // Test Case ID: UT_MS_002
    // Kiểm tra updateModelPosition() cập nhật thành công khi timestamp mới hơn timestamp hiện tại.
    @Test
    void testUpdateModelPositionSuccessWhenTimestampIsNewer() {
        String modelId = "model-2";
        LocalDateTime oldTimestamp = LocalDateTime.of(2026, 5, 2, 9, 0, 0);
        LocalDateTime newTimestamp = LocalDateTime.of(2026, 5, 2, 10, 0, 0);

        Model model = new Model();
        model.setId(modelId);
        model.setPositionX(3.0);
        model.setPositionY(4.0);
        model.setPositionUpdatedAt(oldTimestamp);

        when(modelRepository.findByIdForUpdate(modelId)).thenReturn(Optional.of(model));
        when(modelRepository.save(any(Model.class))).thenAnswer(invocation -> invocation.getArgument(0));

        boolean result = modelService.updateModelPosition(modelId, 33.3, 44.4, newTimestamp);

        assertTrue(result);

        ArgumentCaptor<Model> modelCaptor = ArgumentCaptor.forClass(Model.class);
        verify(modelRepository).save(modelCaptor.capture());
        Model savedModel = modelCaptor.getValue();
        assertEquals(33.3, savedModel.getPositionX());
        assertEquals(44.4, savedModel.getPositionY());
        assertEquals(newTimestamp, savedModel.getPositionUpdatedAt());
        verify(modelRepository).findByIdForUpdate(modelId);
        verifyNoMoreInteractions(modelRepository);
        verifyNoInteractions(attributeService);
    }

    // Test Case ID: UT_MS_003
    // Kiểm tra updateModelPosition() không cập nhật khi timestamp cũ hơn hoặc bằng timestamp hiện tại.
    @Test
    void testUpdateModelPositionRejectedWhenTimestampIsNotNewer() {
        String modelId = "model-3";
        LocalDateTime currentTimestamp = LocalDateTime.of(2026, 5, 2, 10, 0, 0);
        LocalDateTime incomingTimestamp = LocalDateTime.of(2026, 5, 2, 10, 0, 0);

        Model model = new Model();
        model.setId(modelId);
        model.setPositionX(5.0);
        model.setPositionY(6.0);
        model.setPositionUpdatedAt(currentTimestamp);

        when(modelRepository.findByIdForUpdate(modelId)).thenReturn(Optional.of(model));

        boolean result = modelService.updateModelPosition(modelId, 55.5, 66.6, incomingTimestamp);

        assertFalse(result);
        verify(modelRepository).findByIdForUpdate(modelId);
        verify(modelRepository, never()).save(any(Model.class));
        verifyNoMoreInteractions(modelRepository);
        verifyNoInteractions(attributeService);
    }

    // Test Case ID: UT_MS_004
    // Kiểm tra updateModelPosition() trả về false khi không tìm thấy model.
    @Test
    void testUpdateModelPositionRejectedWhenModelNotFound() {
        String modelId = "missing-model";
        LocalDateTime timestamp = LocalDateTime.of(2026, 5, 2, 10, 0, 0);

        when(modelRepository.findByIdForUpdate(modelId)).thenReturn(Optional.empty());

        boolean result = modelService.updateModelPosition(modelId, 77.7, 88.8, timestamp);

        assertFalse(result);
        verify(modelRepository).findByIdForUpdate(modelId);
        verify(modelRepository, never()).save(any(Model.class));
        verifyNoMoreInteractions(modelRepository);
        verifyNoInteractions(attributeService);
    }

    // Test Case ID: UT_MS_005
    // Kiểm tra createModel() tạo model mới với tên mặc định và lưu đúng dữ liệu xuống repository.
    @Test
    void testCreateModelShouldSaveDefaultModelName() {
        String modelId = "model-create-1";
        when(modelRepository.save(any(Model.class))).thenAnswer(invocation -> invocation.getArgument(0));

        Model createdModel = modelService.createModel("Ignored Name", modelId, 12.0, 24.0, Boolean.TRUE, databaseDiagram);

        assertNotNull(createdModel);
        assertEquals(modelId, createdModel.getId());
        assertEquals("Model", createdModel.getName());
        assertEquals(12.0, createdModel.getPositionX());
        assertEquals(24.0, createdModel.getPositionY());
        assertSame(databaseDiagram, createdModel.getDatabaseDiagram());

        ArgumentCaptor<Model> modelCaptor = ArgumentCaptor.forClass(Model.class);
        verify(modelRepository).save(modelCaptor.capture());
        Model savedModel = modelCaptor.getValue();
        assertEquals(modelId, savedModel.getId());
        assertEquals("Model", savedModel.getName());
        assertEquals(12.0, savedModel.getPositionX());
        assertEquals(24.0, savedModel.getPositionY());
        assertSame(databaseDiagram, savedModel.getDatabaseDiagram());
        verifyNoMoreInteractions(modelRepository);
        verifyNoInteractions(attributeService);
    }

    // Test Case ID: UT_MS_006
    // Kiểm tra convertToModelDto() map đúng field và gọi AttributeService để đổi danh sách attributes.
    @Test
    void testConvertToModelDtoShouldMapAllFields() {
        Model model = new Model();
        model.setId("model-dto-1");
        model.setNodeId("node-1");
        model.setName("Customer");
        model.setPositionX(100.0);
        model.setPositionY(200.0);

        Attribute attribute = new Attribute();
        attribute.setId("attr-1");
        attribute.setName("id");
        model.setAttributes(Collections.singletonList(attribute));

        List<AttributeDto> attributeDtos = List.of(new AttributeDto("attr-1", "id", "INT", 11, null, null,
                true, true, false, false, false, null, null, 0, false, null, null, null));
        when(attributeService.convertToDtoList(model.getAttributes())).thenReturn(attributeDtos);

        ModelDto dto = modelService.convertToModelDto(model);

        assertNotNull(dto);
        assertEquals("model-dto-1", dto.getId());
        assertEquals("node-1", dto.getNodeId());
        assertEquals("Customer", dto.getName());
        assertEquals(100.0, dto.getPositionX());
        assertEquals(200.0, dto.getPositionY());
        assertEquals(attributeDtos, dto.getAttributes());
        verify(attributeService).convertToDtoList(model.getAttributes());
        verifyNoMoreInteractions(attributeService);
        verifyNoInteractions(modelRepository);
    }

    // Test Case ID: UT_MS_007
    // Kiểm tra getModelNameByNodeId() trả về đúng tên khi tìm thấy nodeId phù hợp.
    @Test
    void testGetModelNameByNodeIdShouldReturnMatchedName() {
        Model firstModel = new Model();
        firstModel.setId("model-a");
        firstModel.setNodeId("node-a");
        firstModel.setName("Alpha");

        Model secondModel = new Model();
        secondModel.setId("model-b");
        secondModel.setNodeId("node-b");
        secondModel.setName("Beta");

        when(modelRepository.findAll()).thenReturn(List.of(firstModel, secondModel));

        String result = modelService.getModelNameByNodeId("node-b");

        assertEquals("Beta", result);
        verify(modelRepository).findAll();
        verifyNoMoreInteractions(modelRepository);
        verifyNoInteractions(attributeService);
    }

    // Test Case ID: UT_MS_008
    // Kiểm tra getModelNameByNodeId() trả về Unknown khi không có model nào khớp nodeId.
    @Test
    void testGetModelNameByNodeIdShouldReturnUnknownWhenNotFound() {
        Model firstModel = new Model();
        firstModel.setId("model-a");
        firstModel.setNodeId("node-a");
        firstModel.setName("Alpha");

        when(modelRepository.findAll()).thenReturn(List.of(firstModel));

        String result = modelService.getModelNameByNodeId("node-x");

        assertEquals("Unknown", result);
        verify(modelRepository).findAll();
        verifyNoMoreInteractions(modelRepository);
        verifyNoInteractions(attributeService);
    }
}
