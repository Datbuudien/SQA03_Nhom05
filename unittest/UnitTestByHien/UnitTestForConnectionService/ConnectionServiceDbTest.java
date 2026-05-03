package com.example.react_flow_be.service;

import com.example.react_flow_be.entity.Attribute;
import com.example.react_flow_be.entity.Connection;
import com.example.react_flow_be.entity.Model;
import com.example.react_flow_be.repository.AttributeRepository;
import com.example.react_flow_be.repository.ConnectionRepository;
import com.example.react_flow_be.repository.ModelRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.SpringBootConfiguration;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;
import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest(classes = ConnectionServiceDbTest.JpaTestApplication.class, properties = {
        "spring.datasource.url=jdbc:h2:mem:connection_service_db_test;MODE=MySQL;DB_CLOSE_DELAY=-1;DB_CLOSE_ON_EXIT=FALSE",
        "spring.datasource.driver-class-name=org.h2.Driver",
        "spring.datasource.username=sa",
        "spring.datasource.password=",
        "spring.jpa.hibernate.ddl-auto=create-drop",
        "spring.jpa.database-platform=org.hibernate.dialect.H2Dialect",
        "spring.jpa.properties.hibernate.dialect=org.hibernate.dialect.H2Dialect",
        "eureka.client.enabled=false"
}, webEnvironment = SpringBootTest.WebEnvironment.NONE)
@Import(ConnectionService.class)
@Transactional
@ActiveProfiles("test")
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.ANY)
@DisplayName("ConnectionService Integration Tests - H2 + Transaction Rollback")
class ConnectionServiceDbTest {

    @SpringBootConfiguration
    @EnableAutoConfiguration
    @EntityScan(basePackages = "com.example.react_flow_be.entity")
    @EnableJpaRepositories(basePackages = "com.example.react_flow_be.repository")
    static class JpaTestApplication {
    }

    @Autowired
    private ConnectionService serviceUnderTest;

    @Autowired
    private ConnectionRepository connectionRepository;

    @Autowired
    private AttributeRepository attributeRepository;

    @Autowired
    private ModelRepository modelRepository;

    /**
     * TestCaseID: UN_CS_DB_001
     * Mục tiêu:
     * - Kiểm chứng createForeignKeyConnection tạo bản ghi thật trong DB H2.
     * Given:
     * - Có source attribute và target model hợp lệ trong DB test.
     * When:
     * - Gọi createForeignKeyConnection.
     * Then:
     * - Trả true.
     * - DB xuất hiện connection mới với mapping FK -> PK đúng theo đầu vào.
     * CheckDB:
     * - Đọc lại DB qua repository để xác nhận bản ghi và dữ liệu cột.
     * Rollback:
     * - Do test được chạy trong transaction, dữ liệu sẽ tự rollback sau test.
     */
    @Test
    void createForeignKeyConnection_shouldPersistNewConnectionInH2() {
        Model sourceModel = createAndPersistModel("model-source", "Order");
        Model targetModel = createAndPersistModel("model-target", "Customer");
        Attribute sourceForeignKey = createAndPersistAttribute("attr-order-customer-id", "customer_id", sourceModel);

        boolean actualResult = serviceUnderTest.createForeignKeyConnection(
                sourceForeignKey.getId(), targetModel.getId(), "id", "fk_order_customer");

        assertTrue(actualResult);

        Optional<Connection> createdConnection = connectionRepository.findByAttributeId(sourceForeignKey.getId());
        assertTrue(createdConnection.isPresent());
        assertEquals(targetModel.getId(), createdConnection.get().getTargetModel().getId());
        assertEquals("id", createdConnection.get().getTargetAttributeId());
        assertEquals("fk_order_customer", createdConnection.get().getForeignKeyName());
        assertTrue(createdConnection.get().getIsEnforced());
    }

    /**
     * TestCaseID: UN_CS_DB_002
     * Mục tiêu:
     * - Kiểm chứng removeForeignKeyConnection xóa quan hệ connection khỏi attribute
     * ở DB context.
     * Given:
     * - Attribute có connection liên quan đã lưu trong H2.
     * When:
     * - Gọi removeForeignKeyConnection(attributeId).
     * Then:
     * - Trả true.
     * - Collection connection trong attribute trở thành rỗng.
     * CheckDB:
     * - Đọc lại attribute từ repository và kiểm tra collection.
     * Rollback:
     * - Toàn bộ thay đổi bị rollback khi test kết thúc.
     */
    @Test
    void removeForeignKeyConnection_shouldClearConnectionsCollectionInDbContext() {
        Model sourceModel = createAndPersistModel("model-source-2", "Invoice");
        Model targetModel = createAndPersistModel("model-target-2", "Customer");
        Attribute sourceForeignKey = createAndPersistAttribute("attr-invoice-customer-id", "customer_id", sourceModel);

        Connection connection = new Connection();
        connection.setAttribute(sourceForeignKey);
        connection.setTargetModel(targetModel);
        connection.setTargetAttributeId("id");
        connection.setForeignKeyName("fk_invoice_customer");
        connection.setIsEnforced(true);
        connectionRepository.save(connection);

        sourceForeignKey.setConnection(new ArrayList<>(List.of(connection)));
        attributeRepository.save(sourceForeignKey);

        boolean actualResult = serviceUnderTest.removeForeignKeyConnection(sourceForeignKey.getId());
        assertTrue(actualResult);

        Attribute reloadedAttribute = attributeRepository.findById(sourceForeignKey.getId()).orElseThrow();
        assertNotNull(reloadedAttribute.getConnection());
        assertTrue(reloadedAttribute.getConnection().isEmpty());
    }

    /**
     * TestCaseID: UN_CS_DB_003
     * Mục tiêu:
     * - Kiểm chứng removeConnectionsForAttribute xóa đúng connection theo source
     * attribute.
     * Given:
     * - DB có 1 connection gắn với attribute nguồn.
     * When:
     * - Gọi removeConnectionsForAttribute(attributeId).
     * Then:
     * - Không còn connection theo attributeId đó.
     * CheckDB:
     * - findByAttributeId(attributeId) trả Optional.empty().
     * Rollback:
     * - Dữ liệu test tự rollback sau khi test hoàn tất.
     */
    @Test
    void removeConnectionsForAttribute_shouldDeleteConnectionBySourceAttribute() {
        Model sourceModel = createAndPersistModel("model-source-3", "Payment");
        Model targetModel = createAndPersistModel("model-target-3", "Order");
        Attribute sourceForeignKey = createAndPersistAttribute("attr-payment-order-id", "order_id", sourceModel);

        Connection connection = new Connection();
        connection.setAttribute(sourceForeignKey);
        connection.setTargetModel(targetModel);
        connection.setTargetAttributeId("id");
        connection.setForeignKeyName("fk_payment_order");
        connection.setIsEnforced(true);
        connectionRepository.save(connection);

        serviceUnderTest.removeConnectionsForAttribute(sourceForeignKey.getId());

        Optional<Connection> remainingConnection = connectionRepository.findByAttributeId(sourceForeignKey.getId());
        assertTrue(remainingConnection.isEmpty());
    }

    /**
     * TestCaseID: UN_CS_DB_004
     * Mục tiêu:
     * - Kiểm chứng removeConnectionsToAttribute xóa toàn bộ connection trỏ tới cùng
     * target.
     * Given:
     * - Có nhiều connection từ các attribute khác nhau cùng trỏ tới (targetModelId,
     * targetAttributeId).
     * When:
     * - Gọi removeConnectionsToAttribute(targetModelId, targetAttributeId).
     * Then:
     * - Danh sách connection tới target đó bằng 0.
     * CheckDB:
     * - Query findByTargetModelIdAndTargetAttributeId trả danh sách rỗng.
     * Rollback:
     * - Tự rollback sau test vì @Transactional.
     */
    @Test
    void removeConnectionsToAttribute_shouldDeleteAllConnectionsPointingToTargetAttribute() {
        Model sourceModelA = createAndPersistModel("model-source-4a", "Order");
        Model sourceModelB = createAndPersistModel("model-source-4b", "Invoice");
        Model targetModel = createAndPersistModel("model-target-4", "Customer");

        Attribute fkFromOrder = createAndPersistAttribute("attr-order-customer-id-2", "customer_id", sourceModelA);
        Attribute fkFromInvoice = createAndPersistAttribute("attr-invoice-customer-id-2", "customer_id", sourceModelB);

        Connection connectionA = new Connection();
        connectionA.setAttribute(fkFromOrder);
        connectionA.setTargetModel(targetModel);
        connectionA.setTargetAttributeId("id");
        connectionA.setForeignKeyName("fk_order_customer_2");
        connectionA.setIsEnforced(true);
        connectionRepository.save(connectionA);

        Connection connectionB = new Connection();
        connectionB.setAttribute(fkFromInvoice);
        connectionB.setTargetModel(targetModel);
        connectionB.setTargetAttributeId("id");
        connectionB.setForeignKeyName("fk_invoice_customer_2");
        connectionB.setIsEnforced(true);
        connectionRepository.save(connectionB);

        serviceUnderTest.removeConnectionsToAttribute(targetModel.getId(), "id");

        List<Connection> remainingTargetConnections = connectionRepository.findByTargetModelIdAndTargetAttributeId(
                targetModel.getId(), "id");
        assertTrue(remainingTargetConnections.isEmpty());
    }

    private Model createAndPersistModel(String modelId, String modelName) {
        Model model = new Model();
        model.setId(modelId);
        model.setName(modelName);
        return modelRepository.save(model);
    }

    private Attribute createAndPersistAttribute(String attributeId, String attributeName, Model ownerModel) {
        Attribute attribute = new Attribute();
        attribute.setId(attributeId);
        attribute.setName(attributeName);
        attribute.setModel(ownerModel);
        attribute.setConnection(new ArrayList<>());
        return attributeRepository.save(attribute);
    }
}
