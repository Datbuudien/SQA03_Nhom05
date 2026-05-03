package com.example.react_flow_be.service;

import com.example.react_flow_be.entity.Attribute;
import com.example.react_flow_be.entity.Connection;
import com.example.react_flow_be.entity.Model;
import com.example.react_flow_be.repository.AttributeRepository;
import com.example.react_flow_be.repository.ConnectionRepository;
import com.example.react_flow_be.repository.ModelRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("ConnectionService Unit Tests - Branch Coverage Level 2")
class ConnectionServiceTest {

    @Mock
    private ConnectionRepository mockConnectionRepository;

    @Mock
    private AttributeRepository mockAttributeRepository;

    @Mock
    private ModelRepository mockModelRepository;

    @InjectMocks
    private ConnectionService serviceUnderTest;

    /**
     * TestCaseID: UN_CS_001
     * Mục tiêu:
     * - Xác minh createForeignKeyConnection trả false khi không tìm thấy source
     * attribute.
     * Given:
     * - attributeRepository.findById(attributeId) trả Optional.empty().
     * When:
     * - Gọi createForeignKeyConnection với attributeId không tồn tại.
     * Then (Expected theo đặc tả):
     * - Hệ thống không thể tạo kết nối FK vì thiếu nguồn ánh xạ.
     * - Trả về false.
     * CheckDB:
     * - Không được gọi save ở connectionRepository.
     * Rollback:
     * - Unit test dùng mock, không phát sinh giao dịch DB thật.
     */
    @Test
    void createForeignKeyConnection_shouldReturnFalse_whenSourceAttributeNotFound() {
        when(mockAttributeRepository.findById("attr-missing")).thenReturn(Optional.empty());

        boolean actualResult = serviceUnderTest.createForeignKeyConnection(
                "attr-missing", "model-target", "pk-id", "fk_order_customer");

        assertFalse(actualResult);
        verify(mockAttributeRepository, times(1)).findById("attr-missing");
        verify(mockModelRepository, never()).findById(anyString());
        verify(mockConnectionRepository, never()).save(any(Connection.class));
    }

    /**
     * TestCaseID: UN_CS_002
     * Mục tiêu:
     * - Xác minh createForeignKeyConnection trả false khi target model không tồn
     * tại.
     * Given:
     * - Source attribute hợp lệ.
     * - modelRepository.findById(targetModelId) trả Optional.empty().
     * Then (Expected theo đặc tả):
     * - Không thể tạo connection do không có bảng đích chứa khóa chính.
     * - Trả về false và không ghi DB connection.
     * CheckDB:
     * - verify không gọi connectionRepository.save.
     */
    @Test
    void createForeignKeyConnection_shouldReturnFalse_whenTargetModelNotFound() {
        Attribute existingSourceAttribute = new Attribute();
        existingSourceAttribute.setId("attr-fk");
        when(mockAttributeRepository.findById("attr-fk")).thenReturn(Optional.of(existingSourceAttribute));
        when(mockModelRepository.findById("model-missing")).thenReturn(Optional.empty());

        boolean actualResult = serviceUnderTest.createForeignKeyConnection(
                "attr-fk", "model-missing", "pk-id", "fk_order_customer");

        assertFalse(actualResult);
        verify(mockAttributeRepository, times(1)).findById("attr-fk");
        verify(mockModelRepository, times(1)).findById("model-missing");
        verify(mockConnectionRepository, never()).save(any(Connection.class));
    }

    /**
     * TestCaseID: UN_CS_003
     * Mục tiêu:
     * - Xác minh nhánh update connection đã tồn tại.
     * Given:
     * - Source attribute và target model tồn tại.
     * - findByAttributeIdForUpdate trả về connection cũ.
     * Then (Expected theo đặc tả):
     * - Hệ thống cập nhật mapping FK mới trên kết nối hiện có.
     * - Trả true.
     * CheckDB:
     * - save được gọi đúng 1 lần với các field target/fkName đã cập nhật.
     */
    @Test
    void createForeignKeyConnection_shouldUpdateExistingConnection_whenConnectionAlreadyExists() {
        Attribute existingSourceAttribute = new Attribute();
        existingSourceAttribute.setId("attr-fk");

        Model targetModel = new Model();
        targetModel.setId("model-customer");

        Connection existingConnection = new Connection();
        existingConnection.setTargetAttributeId("old-pk");
        existingConnection.setForeignKeyName("old_fk_name");

        when(mockAttributeRepository.findById("attr-fk")).thenReturn(Optional.of(existingSourceAttribute));
        when(mockModelRepository.findById("model-customer")).thenReturn(Optional.of(targetModel));
        when(mockConnectionRepository.findByAttributeIdForUpdate("attr-fk"))
                .thenReturn(Optional.of(existingConnection));

        boolean actualResult = serviceUnderTest.createForeignKeyConnection(
                "attr-fk", "model-customer", "customer_id", "fk_order_customer");

        assertTrue(actualResult);
        ArgumentCaptor<Connection> savedConnectionCaptor = ArgumentCaptor.forClass(Connection.class);
        verify(mockConnectionRepository, times(1)).save(savedConnectionCaptor.capture());

        Connection savedConnection = savedConnectionCaptor.getValue();
        assertEquals(targetModel, savedConnection.getTargetModel());
        assertEquals("customer_id", savedConnection.getTargetAttributeId());
        assertEquals("fk_order_customer", savedConnection.getForeignKeyName());
    }

    /**
     * TestCaseID: UN_CS_004
     * Mục tiêu:
     * - Xác minh nhánh tạo mới connection khi chưa có connection theo attributeId.
     * Given:
     * - Source attribute và target model tồn tại.
     * - findByAttributeIdForUpdate trả Optional.empty().
     * Then (Expected theo đặc tả):
     * - Hệ thống tạo 1 connection mới từ FK -> PK đã chọn.
     * - Connection mới được enforce.
     * - Trả true.
     * CheckDB:
     * - save được gọi 1 lần với dữ liệu tạo mới chính xác.
     */
    @Test
    void createForeignKeyConnection_shouldCreateNewConnection_whenNoExistingConnection() {
        Attribute sourceAttribute = new Attribute();
        sourceAttribute.setId("attr-fk");

        Model targetModel = new Model();
        targetModel.setId("model-customer");

        when(mockAttributeRepository.findById("attr-fk")).thenReturn(Optional.of(sourceAttribute));
        when(mockModelRepository.findById("model-customer")).thenReturn(Optional.of(targetModel));
        when(mockConnectionRepository.findByAttributeIdForUpdate("attr-fk")).thenReturn(Optional.empty());

        boolean actualResult = serviceUnderTest.createForeignKeyConnection(
                "attr-fk", "model-customer", "customer_id", "fk_order_customer");

        assertTrue(actualResult);
        ArgumentCaptor<Connection> newConnectionCaptor = ArgumentCaptor.forClass(Connection.class);
        verify(mockConnectionRepository, times(1)).save(newConnectionCaptor.capture());

        Connection createdConnection = newConnectionCaptor.getValue();
        assertEquals(sourceAttribute, createdConnection.getAttribute());
        assertEquals(targetModel, createdConnection.getTargetModel());
        assertEquals("customer_id", createdConnection.getTargetAttributeId());
        assertEquals("fk_order_customer", createdConnection.getForeignKeyName());
        assertTrue(createdConnection.getIsEnforced());
    }

    /**
     * TestCaseID: UN_CS_005
     * Mục tiêu:
     * - Xác minh nhánh catch exception của createForeignKeyConnection.
     * Given:
     * - Tầng repository ném RuntimeException khi kiểm tra connection hiện có.
     * Then (Expected theo đặc tả):
     * - Hệ thống xử lý lỗi an toàn và trả false.
     */
    @Test
    void createForeignKeyConnection_shouldReturnFalse_whenUnexpectedExceptionOccurs() {
        Attribute sourceAttribute = new Attribute();
        sourceAttribute.setId("attr-fk");

        Model targetModel = new Model();
        targetModel.setId("model-customer");

        when(mockAttributeRepository.findById("attr-fk")).thenReturn(Optional.of(sourceAttribute));
        when(mockModelRepository.findById("model-customer")).thenReturn(Optional.of(targetModel));
        when(mockConnectionRepository.findByAttributeIdForUpdate("attr-fk"))
                .thenThrow(new RuntimeException("Simulated DB error"));

        boolean actualResult = serviceUnderTest.createForeignKeyConnection(
                "attr-fk", "model-customer", "customer_id", "fk_order_customer");

        assertFalse(actualResult);
    }

    /**
     * TestCaseID: UN_CS_006
     * Defect cần ghi nhận:
     * - Chưa chặn self-reference FK: source attribute và target model cùng 1 model.
     * Kỳ vọng nghiệp vụ:
     * - Trả false, không được save connection.
     * Hiện trạng:
     * - Test đang disabled vì code hiện tại vẫn cho phép và trả true.
     */
    @Test
    void createForeignKeyConnection_shouldReturnFalse_whenSourceAndTargetAreSameModel() {
        Model sameModel = new Model();
        sameModel.setId("model-order");

        Attribute sourceAttribute = new Attribute();
        sourceAttribute.setId("attr-order-customer-id");
        sourceAttribute.setModel(sameModel);
        sourceAttribute.setDataType("BIGINT");

        when(mockAttributeRepository.findById("attr-order-customer-id")).thenReturn(Optional.of(sourceAttribute));
        when(mockModelRepository.findById("model-order")).thenReturn(Optional.of(sameModel));
        when(mockConnectionRepository.findByAttributeIdForUpdate("attr-order-customer-id"))
                .thenReturn(Optional.empty());

        boolean actualResult = serviceUnderTest.createForeignKeyConnection(
                "attr-order-customer-id", "model-order", "id", "fk_order_customer_self");

        assertFalse(actualResult);
        verify(mockConnectionRepository, never()).save(any(Connection.class));
    }

    /**
     * TestCaseID: UN_CS_007
     * Defect cần ghi nhận:
     * - Chưa validate targetAttributeId rỗng/null.
     * Kỳ vọng nghiệp vụ:
     * - Trả false, không save dữ liệu FK không hợp lệ.
     * Hiện trạng:
     * - Test đang disabled vì code hiện tại không chặn trường hợp này.
     */
    @Test
    void createForeignKeyConnection_shouldReturnFalse_whenTargetAttributeIdIsBlank() {
        Attribute sourceAttribute = new Attribute();
        sourceAttribute.setId("attr-fk");

        Model targetModel = new Model();
        targetModel.setId("model-customer");

        when(mockAttributeRepository.findById("attr-fk")).thenReturn(Optional.of(sourceAttribute));
        when(mockModelRepository.findById("model-customer")).thenReturn(Optional.of(targetModel));
        when(mockConnectionRepository.findByAttributeIdForUpdate("attr-fk")).thenReturn(Optional.empty());

        boolean actualResult = serviceUnderTest.createForeignKeyConnection(
                "attr-fk", "model-customer", "   ", "fk_order_customer");

        assertFalse(actualResult);
        verify(mockConnectionRepository, never()).save(any(Connection.class));
    }

    /**
     * TestCaseID: UN_CS_008
     * Defect cần ghi nhận:
     * - Chưa validate foreignKeyName rỗng/null.
     * Kỳ vọng nghiệp vụ:
     * - Trả false nếu tên constraint không hợp lệ.
     * Hiện trạng:
     * - Test đang disabled vì code hiện tại vẫn save và trả true.
     */
    @Test
    void createForeignKeyConnection_shouldReturnFalse_whenForeignKeyNameIsBlank() {
        Attribute sourceAttribute = new Attribute();
        sourceAttribute.setId("attr-fk");

        Model targetModel = new Model();
        targetModel.setId("model-customer");

        when(mockAttributeRepository.findById("attr-fk")).thenReturn(Optional.of(sourceAttribute));
        when(mockModelRepository.findById("model-customer")).thenReturn(Optional.of(targetModel));
        when(mockConnectionRepository.findByAttributeIdForUpdate("attr-fk")).thenReturn(Optional.empty());

        boolean actualResult = serviceUnderTest.createForeignKeyConnection(
                "attr-fk", "model-customer", "id", "");

        assertFalse(actualResult);
        verify(mockConnectionRepository, never()).save(any(Connection.class));
    }

    /**
     * TestCaseID: UN_CS_009
     * Defect cần ghi nhận:
     * - Chưa validate tương thích kiểu dữ liệu giữa FK và PK.
     * Kỳ vọng nghiệp vụ:
     * - Trả false khi source FK type khác với target PK type.
     * Hiện trạng:
     * - Test đang disabled vì API hiện tại chưa kiểm tra dữ liệu kiểu này.
     */
    @Test
    void createForeignKeyConnection_shouldReturnFalse_whenForeignKeyTypeMismatchesPrimaryKeyType() {
        Attribute sourceAttribute = new Attribute();
        sourceAttribute.setId("attr-fk");
        sourceAttribute.setDataType("VARCHAR");

        Model targetModel = new Model();
        targetModel.setId("model-customer");

        when(mockAttributeRepository.findById("attr-fk")).thenReturn(Optional.of(sourceAttribute));
        when(mockModelRepository.findById("model-customer")).thenReturn(Optional.of(targetModel));
        when(mockConnectionRepository.findByAttributeIdForUpdate("attr-fk")).thenReturn(Optional.empty());

        boolean actualResult = serviceUnderTest.createForeignKeyConnection(
                "attr-fk", "model-customer", "customer_id_bigint", "fk_order_customer");

        assertFalse(actualResult);
        verify(mockConnectionRepository, never()).save(any(Connection.class));
    }

    /**
     * TestCaseID: UN_CS_010
     * Mục tiêu:
     * - Xác minh removeForeignKeyConnection thành công khi attribute tồn tại và có
     * danh sách connection.
     * Given:
     * - Attribute có danh sách connection trước đó.
     * Then (Expected theo đặc tả):
     * - Danh sách connection liên quan bị xóa khỏi attribute.
     * - Trả true.
     * CheckDB:
     * - verify findById được gọi đúng id.
     */
    @Test
    void removeForeignKeyConnection_shouldReturnTrue_whenAttributeExistsAndConnectionsCleared() {
        Attribute attributeWithConnections = new Attribute();
        attributeWithConnections.setId("attr-fk");
        List<Connection> fkConnections = new ArrayList<>();
        fkConnections.add(new Connection());
        attributeWithConnections.setConnection(fkConnections);

        when(mockAttributeRepository.findById("attr-fk")).thenReturn(Optional.of(attributeWithConnections));

        boolean actualResult = serviceUnderTest.removeForeignKeyConnection("attr-fk");

        assertTrue(actualResult);
        assertTrue(attributeWithConnections.getConnection().isEmpty());
        verify(mockAttributeRepository, times(1)).findById("attr-fk");
    }

    /**
     * TestCaseID: UN_CS_011
     * Mục tiêu:
     * - Xác minh removeForeignKeyConnection trả false khi attribute không tồn tại.
     * Given:
     * - findById trả Optional.empty(), service gọi .get() và đi vào catch.
     * Then (Expected theo đặc tả):
     * - Không có kết nối để ngắt do FK không tồn tại.
     * - Trả false.
     */
    @Test
    void removeForeignKeyConnection_shouldReturnFalse_whenAttributeNotFound() {
        when(mockAttributeRepository.findById("attr-missing")).thenReturn(Optional.empty());

        boolean actualResult = serviceUnderTest.removeForeignKeyConnection("attr-missing");

        assertFalse(actualResult);
    }

    /**
     * TestCaseID: UN_CS_012
     * Mục tiêu:
     * - Xác minh removeForeignKeyConnection trả false nếu dữ liệu connection list
     * bị null gây lỗi.
     * Given:
     * - Attribute tồn tại nhưng connection list là null.
     * Then:
     * - Service bắt exception và trả false.
     */
    @Test
    void removeForeignKeyConnection_shouldReturnFalse_whenConnectionListIsNull() {
        Attribute attributeWithNullConnectionList = new Attribute();
        attributeWithNullConnectionList.setId("attr-fk");
        attributeWithNullConnectionList.setConnection(null);
        when(mockAttributeRepository.findById("attr-fk")).thenReturn(Optional.of(attributeWithNullConnectionList));

        boolean actualResult = serviceUnderTest.removeForeignKeyConnection("attr-fk");

        assertFalse(actualResult);
    }

    /**
     * TestCaseID: UN_CS_013
     * Mục tiêu:
     * - Xác minh removeConnectionsForAttribute gọi delete khi tìm thấy connection.
     * Expected:
     * - Connection tương ứng attributeId bị xóa.
     * CheckDB:
     * - verify delete(connection) đúng 1 lần.
     */
    @Test
    void removeConnectionsForAttribute_shouldDeleteConnection_whenConnectionExists() {
        Connection existingConnection = new Connection();
        when(mockConnectionRepository.findByAttributeId("attr-fk")).thenReturn(Optional.of(existingConnection));

        serviceUnderTest.removeConnectionsForAttribute("attr-fk");

        verify(mockConnectionRepository, times(1)).delete(existingConnection);
    }

    /**
     * TestCaseID: UN_CS_014
     * Mục tiêu:
     * - Xác minh removeConnectionsForAttribute không delete khi không có dữ liệu.
     * Expected:
     * - Không gọi delete.
     */
    @Test
    void removeConnectionsForAttribute_shouldNotDelete_whenConnectionDoesNotExist() {
        when(mockConnectionRepository.findByAttributeId("attr-fk")).thenReturn(Optional.empty());

        serviceUnderTest.removeConnectionsForAttribute("attr-fk");

        verify(mockConnectionRepository, never()).delete(any(Connection.class));
    }

    /**
     * TestCaseID: UN_CS_015
     * Mục tiêu:
     * - Xác minh removeConnectionsForAttribute nuốt exception (không ném lỗi ra
     * ngoài).
     * Given:
     * - findByAttributeId ném RuntimeException.
     * Expected:
     * - Method kết thúc bình thường (assertDoesNotThrow).
     */
    @Test
    void removeConnectionsForAttribute_shouldHandleException_whenRepositoryThrowsError() {
        when(mockConnectionRepository.findByAttributeId("attr-fk"))
                .thenThrow(new RuntimeException("Simulated query error"));

        assertDoesNotThrow(() -> serviceUnderTest.removeConnectionsForAttribute("attr-fk"));
    }

    /**
     * TestCaseID: UN_CS_016
     * Mục tiêu:
     * - Xác minh removeConnectionsToAttribute gọi deleteAll khi có nhiều connection
     * tới cùng PK đích.
     * Expected:
     * - deleteAll được gọi với danh sách connection tìm thấy.
     */
    @Test
    void removeConnectionsToAttribute_shouldDeleteAll_whenTargetConnectionsExist() {
        List<Connection> existingTargetConnections = List.of(new Connection(), new Connection());
        when(mockConnectionRepository.findByTargetModelIdAndTargetAttributeId("model-customer", "customer_id"))
                .thenReturn(existingTargetConnections);

        serviceUnderTest.removeConnectionsToAttribute("model-customer", "customer_id");

        verify(mockConnectionRepository, times(1)).deleteAll(existingTargetConnections);
    }

    /**
     * TestCaseID: UN_CS_017
     * Mục tiêu:
     * - Xác minh removeConnectionsToAttribute không deleteAll khi danh sách rỗng.
     * Expected:
     * - deleteAll không được gọi.
     */
    @Test
    void removeConnectionsToAttribute_shouldNotDeleteAll_whenNoTargetConnectionsFound() {
        when(mockConnectionRepository.findByTargetModelIdAndTargetAttributeId("model-customer", "customer_id"))
                .thenReturn(List.of());

        serviceUnderTest.removeConnectionsToAttribute("model-customer", "customer_id");

        verify(mockConnectionRepository, never()).deleteAll(any());
    }

    /**
     * TestCaseID: UN_CS_018
     * Mục tiêu:
     * - Xác minh removeConnectionsToAttribute xử lý exception an toàn.
     * Given:
     * - findByTargetModelIdAndTargetAttributeId ném lỗi.
     * Expected:
     * - Method không ném exception ra ngoài.
     */
    @Test
    void removeConnectionsToAttribute_shouldHandleException_whenRepositoryThrowsError() {
        when(mockConnectionRepository.findByTargetModelIdAndTargetAttributeId("model-customer", "customer_id"))
                .thenThrow(new RuntimeException("Simulated query failure"));

        assertDoesNotThrow(() -> serviceUnderTest.removeConnectionsToAttribute("model-customer", "customer_id"));
    }
}
