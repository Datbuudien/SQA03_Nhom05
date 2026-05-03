package com.example.react_flow_be;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Optional;
import java.util.List;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.argThat;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import org.mockito.junit.jupiter.MockitoExtension;

import com.example.react_flow_be.dto.AttributeDto;
import com.example.react_flow_be.entity.Attribute;
import com.example.react_flow_be.entity.Connection;
import com.example.react_flow_be.entity.Model;
import com.example.react_flow_be.repository.AttributeRepository;
import com.example.react_flow_be.repository.ModelRepository;
import com.example.react_flow_be.service.AttributeService;
import com.example.react_flow_be.service.ConnectionService;

@ExtendWith(MockitoExtension.class)
class AttributeServiceTest {

    @Mock
    private AttributeRepository attributeRepository;

    @Mock
    private ModelRepository modelRepository;

    @Mock
    private ConnectionService connectionService;

    @InjectMocks
    private AttributeService attributeService;

    private Attribute testAttribute;
    private Model testModel;
    private final LocalDateTime now = LocalDateTime.now();

    @BeforeEach
    void setUp() {
        // Setup dữ liệu mẫu trước mỗi test case
        testModel = new Model();
        testModel.setId("model-1");
        testModel.setName("User");

        testAttribute = new Attribute();
        testAttribute.setId("attr-1");
        testAttribute.setName("oldName");
        testAttribute.setModel(testModel);
        testAttribute.setIsPrimaryKey(false);
        testAttribute.setIsForeignKey(false);
        testAttribute.setConnection(new ArrayList<>());
    }

    // ==========================================
    // TEST UPDATE ATTRIBUTE NAME
    // ==========================================
    @Test
    void updateAttributeName_Success() {
        // Given
        when(attributeRepository.findByIdForUpdate("attr-1")).thenReturn(Optional.of(testAttribute));
        when(attributeRepository.save(any(Attribute.class))).thenReturn(testAttribute);

        // When
        boolean result = attributeService.updateAttributeName("attr-1", "newName", now);

        // Then
        assertTrue(result);
        assertEquals("newName", testAttribute.getName());
        assertEquals(now, testAttribute.getNameUpdatedAt());
        verify(attributeRepository, times(1)).save(testAttribute);
    }

    @Test
    void updateAttributeName_Fails_WhenTimestampIsOlder() {
        // Given
        testAttribute.setNameUpdatedAt(now.plusDays(1)); // Thời gian trong DB mới hơn request
        when(attributeRepository.findByIdForUpdate("attr-1")).thenReturn(Optional.of(testAttribute));

        // When
        boolean result = attributeService.updateAttributeName("attr-1", "newName", now);

        // Then
        assertFalse(result);
        assertEquals("oldName", testAttribute.getName()); // Không bị đổi
        verify(attributeRepository, never()).save(any());
    }

    @Test
    void updateAttributeName_Fails_WhenExceptionOccurs() {
        // Given
        when(attributeRepository.findByIdForUpdate("attr-1")).thenThrow(new RuntimeException("DB down"));

        // When
        boolean result = attributeService.updateAttributeName("attr-1", "newName", now);

        // Then
        assertFalse(result);
    }

    // ==========================================
    // TEST SET PRIMARY KEY
    // ==========================================
    @Test
    void setAttributeAsPrimaryKey_Success_RemovesFKConnection() {
        // Given
        testAttribute.setIsForeignKey(true); // Giả sử nó đang là FK
        when(attributeRepository.findByIdForUpdate("attr-1")).thenReturn(Optional.of(testAttribute));

        // When
        boolean result = attributeService.setAttributeAsPrimaryKey("attr-1", now);

        // Then
        assertTrue(result);
        assertTrue(testAttribute.getIsPrimaryKey());
        assertFalse(testAttribute.getIsForeignKey());
        assertFalse(testAttribute.getIsNullable());
        assertTrue(testAttribute.getHasIndex());
        assertEquals("PRIMARY_oldName", testAttribute.getIndexName());
        
        // Xác minh rằng hàm xóa FK đã được gọi
        verify(connectionService, times(1)).removeConnectionsForAttribute("attr-1");
        verify(attributeRepository, times(1)).save(testAttribute);
    }

    // ==========================================
    // TEST SET FOREIGN KEY
    // ==========================================
    @Test
    void setAttributeAsForeignKey_Success_RemovesPKConnection() {
        // Given
        testAttribute.setIsPrimaryKey(true); // Giả sử nó đang là PK
        when(attributeRepository.findByIdForUpdate("attr-1")).thenReturn(Optional.of(testAttribute));

        // When
        boolean result = attributeService.setAttributeAsForeignKey("attr-1", now);

        // Then
        assertTrue(result);
        assertFalse(testAttribute.getIsPrimaryKey());
        assertTrue(testAttribute.getIsForeignKey());
        assertEquals("idx_user_oldName", testAttribute.getIndexName());
        
        // Xác minh rằng hàm xóa kết nối trỏ tới PK đã được gọi
        verify(connectionService, times(1)).removeConnectionsToAttribute("User", "oldName");
        verify(attributeRepository, times(1)).save(testAttribute);
    }

    // ==========================================
    // TEST ADD ATTRIBUTE
    // ==========================================
    @Test
    void addAttribute_Success() {
        // Given
        when(modelRepository.findById("model-1")).thenReturn(Optional.of(testModel));
        when(attributeRepository.findByModelIdOrderByAttributeOrder("model-1")).thenReturn(Arrays.asList(new Attribute(), new Attribute())); // Đã có 2 thuộc tính
        
        Attribute savedAttr = new Attribute();
        savedAttr.setId("attr-new");
        when(attributeRepository.save(any(Attribute.class))).thenReturn(savedAttr);

        // When
        String newId = attributeService.addAttribute("model-1", "attr-new", "email", "VARCHAR");

        // Then
        assertEquals("attr-new", newId);
        
        // Bắt lấy object được truyền vào hàm save để kiểm tra logic bên trong
        verify(attributeRepository).save(argThat(attr -> 
            attr.getName().equals("email") &&
            attr.getDataType().equals("VARCHAR") &&
            attr.getLength() == 255 &&
            attr.getAttributeOrder() == 2 // Size của list existingAttributes là 2
        ));
    }

    // ==========================================
    // TEST DELETE ATTRIBUTE
    // ==========================================
    @Test
    void deleteAttribute_Success_ReordersRemaining() {
        // Given
        when(attributeRepository.findByIdForUpdate("attr-1")).thenReturn(Optional.of(testAttribute));
        
        Attribute remainingAttr = new Attribute();
        remainingAttr.setId("attr-2");
        when(attributeRepository.findByModelIdOrderByAttributeOrder("model-1")).thenReturn(Arrays.asList(remainingAttr));

        // When
        boolean result = attributeService.deleteAttribute("attr-1");

        // Then
        assertTrue(result);
        verify(attributeRepository, times(1)).delete(testAttribute);
        verify(attributeRepository, times(1)).save(remainingAttr); // Check reorder
        assertEquals(0, remainingAttr.getAttributeOrder());
    }

    // ==========================================
    // TEST CONVERT TO DTO
    // ==========================================
    @Test
    void convertToAttributeDto_WithConnection() {
        // Given
        Model targetModel = new Model();
        targetModel.setId("model-target");

        Connection conn = new Connection();
        conn.setId(1L);
        conn.setTargetModel(targetModel);
        conn.setTargetAttributeId("attr-target");
        conn.setForeignKeyName("fk_name");
        conn.setIsEnforced(true);

        testAttribute.getConnection().add(conn);
        testAttribute.setDataType("INT");
        testAttribute.setAttributeOrder(1);

        // When
        AttributeDto dto = attributeService.convertToAttributeDto(testAttribute);

        // Then
        assertNotNull(dto);
        assertEquals("attr-1", dto.getId());
        assertNotNull(dto.getConnection());
        assertEquals(1L, dto.getConnection().getId());
        assertEquals("model-target", dto.getConnection().getTargetModelId());
    }
    // ==========================================
    // 1. CÁC HÀM CHƯA ĐƯỢC TEST (0% COVERAGE)
    // ==========================================

    @Test
    void updateAttributeType_Success() {
        when(attributeRepository.findByIdForUpdate("attr-1")).thenReturn(Optional.of(testAttribute));
        when(attributeRepository.save(any(Attribute.class))).thenReturn(testAttribute);
        
        boolean result = attributeService.updateAttributeType("attr-1", "VARCHAR", now);
        
        assertTrue(result);
        assertEquals("VARCHAR", testAttribute.getDataType());
        assertEquals(now, testAttribute.getTypeUpdatedAt());
    }

    @Test
    void updateAttributeType_Fails_WhenException() {
        when(attributeRepository.findByIdForUpdate("attr-1")).thenThrow(new RuntimeException("DB Error"));
        assertFalse(attributeService.updateAttributeType("attr-1", "VARCHAR", now));
    }

    @Test
    void setAttributeAsNormal_Success_FromPK() {
        testAttribute.setIsPrimaryKey(true); // Đang là PK
        when(attributeRepository.findByIdForUpdate("attr-1")).thenReturn(Optional.of(testAttribute));
        
        boolean result = attributeService.setAttributeAsNormal("attr-1", now);
        
        assertTrue(result);
        assertFalse(testAttribute.getIsPrimaryKey());
        assertFalse(testAttribute.getIsForeignKey());
        assertTrue(testAttribute.getIsNullable());
        assertFalse(testAttribute.getHasIndex());
        verify(connectionService, times(1)).removeConnectionsToAttribute("User", "oldName");
    }

    @Test
    void setAttributeAsNormal_Success_FromFK() {
        testAttribute.setIsForeignKey(true); // Đang là FK
        when(attributeRepository.findByIdForUpdate("attr-1")).thenReturn(Optional.of(testAttribute));
        
        boolean result = attributeService.setAttributeAsNormal("attr-1", now);
        
        assertTrue(result);
        verify(connectionService, times(1)).removeConnectionsForAttribute("attr-1");
    }

    @Test
    void createAttribute_Success_Varchar_PK() {
        // Giả lập save trả về chính object được truyền vào
        when(attributeRepository.save(any(Attribute.class))).thenAnswer(i -> i.getArguments()[0]);
        
        Attribute res = attributeService.createAttribute("attr-1", testModel, "email", "VARCHAR", false, 1, false, true);
        
        assertEquals(255, res.getLength()); // Tự động set 255 cho VARCHAR
        assertEquals(Attribute.IndexType.PRIMARY, res.getIndexType());
        assertTrue(res.getHasIndex());
    }

    @Test
    void createAttribute_Success_BigInt_FK() {
        when(attributeRepository.save(any(Attribute.class))).thenAnswer(i -> i.getArguments()[0]);
        
        Attribute res = attributeService.createAttribute("attr-1", testModel, "user_id", "BIGINT", true, 1, false, false);
        
        assertEquals(20, res.getLength()); // Tự động set 20 cho BIGINT
        assertEquals("idx_user_user_id", res.getIndexName());
    }

    @Test
    void getAttributeByModelAndName_Success() {
        when(attributeRepository.findByModelIdAndName("model-1", "oldName")).thenReturn(Optional.of(testAttribute));
        Attribute res = attributeService.getAttributeByModelAndName(testModel, "oldName");
        assertNotNull(res);
        assertEquals("oldName", res.getName());
    }

    @Test
    void convertToDtoList_Success_WithSorting() {
        Attribute attr2 = new Attribute();
        attr2.setId("attr-2");
        attr2.setAttributeOrder(2);
        attr2.setConnection(new ArrayList<>());
        
        testAttribute.setAttributeOrder(1);

        // Truyền list lộn xộn để test hàm sort
        List<AttributeDto> res = attributeService.convertToDtoList(Arrays.asList(attr2, testAttribute));
        
        assertEquals(2, res.size());
        assertEquals("attr-1", res.get(0).getId()); // attr-1 phải lên đầu vì order = 1
    }

    // ==========================================
    // 2. LẤP ĐẦY CÁC NHÁNH (BRANCHES) BỊ THIẾU
    // ==========================================

    @Test
    void convertToAttributeDto_WithoutConnection() {
        testAttribute.setConnection(new ArrayList<>()); // Empty list
        AttributeDto dto = attributeService.convertToAttributeDto(testAttribute);
        assertNull(dto.getConnection()); // Không lỗi NullPointer
    }

    @Test
    void addAttribute_Fails_ModelNotFound() {
        when(modelRepository.findById("model-1")).thenReturn(Optional.empty()); // Không tìm thấy Model
        String res = attributeService.addAttribute("model-1", "attr-2", "name", "INT");
        assertNull(res);
    }

    @Test
    void deleteAttribute_Fails_NotFound() {
        when(attributeRepository.findByIdForUpdate("attr-unknown")).thenReturn(Optional.empty());
        assertFalse(attributeService.deleteAttribute("attr-unknown"));
    }

    @Test
    void deleteAttribute_Success_NotPK() {
        testAttribute.setIsPrimaryKey(false); // Không phải PK
        when(attributeRepository.findByIdForUpdate("attr-1")).thenReturn(Optional.of(testAttribute));
        when(attributeRepository.findByModelIdOrderByAttributeOrder("model-1")).thenReturn(new ArrayList<>());
        
        assertTrue(attributeService.deleteAttribute("attr-1"));
        verify(connectionService, never()).removeConnectionsToAttribute(any(), any()); // Chắc chắn hàm này không bị gọi thừa
    }
}