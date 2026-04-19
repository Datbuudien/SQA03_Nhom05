package com.example.react_flow_be.unittest;

import com.example.react_flow_be.entity.Attribute;
import com.example.react_flow_be.entity.Connection;
import com.example.react_flow_be.entity.DatabaseDiagram;
import com.example.react_flow_be.entity.Model;
import com.example.react_flow_be.repository.AttributeRepository;
import com.example.react_flow_be.repository.ConnectionRepository;
import com.example.react_flow_be.repository.ModelRepository;
import com.example.react_flow_be.service.AttributeService;
import com.example.react_flow_be.service.ConnectionService;
import com.example.react_flow_be.service.DatabaseDiagramService;
import com.example.react_flow_be.service.ModelService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@ActiveProfiles("test")
@Transactional
@DisplayName("UC03-ConnectionService Unit Tests")
class UC03_ConnectionServiceTest {

    @Autowired
    private ConnectionService connectionService;

    @Autowired
    private AttributeService attributeService;

    @Autowired
    private ModelService modelService;

    @Autowired
    private DatabaseDiagramService databaseDiagramService;

    @Autowired
    private ConnectionRepository connectionRepository;

    @Autowired
    private AttributeRepository attributeRepository;

    @Autowired
    private ModelRepository modelRepository;

    private DatabaseDiagram testDiagram;
    private Model sourceModel;
    private Model targetModel;
    private Attribute sourceAttr;
    private Attribute targetAttr;
    private String testUsername;

    @BeforeEach
    void setUp() {
        testUsername = "test_owner_conn_" + System.currentTimeMillis();
        testDiagram = databaseDiagramService.createBlankDiagram("Test Connection Diagram", testUsername);

        sourceModel = modelService.createModel("User", "m_source_" + testUsername, 100.0, 100.0, false, testDiagram);
        targetModel = modelService.createModel("Post", "m_target_" + testUsername, 300.0, 100.0, false, testDiagram);

        sourceAttr = attributeService.createAttribute(
                "attr_source_" + testUsername, sourceModel, "id", "BIGINT", false, 0, false, true);
        targetAttr = attributeService.createAttribute(
                "attr_target_" + testUsername, targetModel, "user_id", "BIGINT", true, 0, false, false);
    }

    // ============ TEST: createConnection() ============

    @Test
    @DisplayName("UC03-UT-501: createConnection - Standard FK connection")
    void createConnection_testChuan1() {
        // Act
        Connection connection = connectionService.createConnection(
                sourceAttr, targetModel, targetAttr.getId(), "fk_user_post", "#FF0000");

        // Assert
        assertNotNull(connection);
        assertNotNull(connection.getId());
        assertEquals(sourceAttr.getId(), connection.getAttribute().getId());
        assertEquals(targetModel.getId(), connection.getTargetModel().getId());
        assertEquals(targetAttr.getId(), connection.getTargetAttributeId());
        assertEquals("fk_user_post", connection.getForeignKeyName());
        assertTrue(connection.getIsEnforced());
    }

    @Test
    @DisplayName("UC03-UT-502: createConnection - Multiple connections from same attribute")
    void createConnection_testChuan2() {
        // Arrange
        Model targetModel2 = modelService.createModel("Comment", "m_target2_" + testUsername, 500.0, 100.0, false, testDiagram);
        Attribute targetAttr2 = attributeService.createAttribute(
                "attr_target2_" + testUsername, targetModel2, "user_id", "BIGINT", true, 0, false, false);

        // Act
        Connection conn1 = connectionService.createConnection(
                sourceAttr, targetModel, targetAttr.getId(), "fk_user_post", "#FF0000");
        Connection conn2 = connectionService.createConnection(
                sourceAttr, targetModel2, targetAttr2.getId(), "fk_user_comment", "#0000FF");

        // Assert
        assertNotNull(conn1);
        assertNotNull(conn2);
        assertNotEquals(conn1.getId(), conn2.getId());
    }

    // ============ TEST: createForeignKeyConnection() ============

    @Test
    @DisplayName("UC03-UT-503: createForeignKeyConnection - New FK connection")
    void createForeignKeyConnection_testChuan1() {
        // Act
        boolean result = connectionService.createForeignKeyConnection(
                sourceAttr.getId(),
                targetModel.getId(),
                targetAttr.getId(),
                "fk_test_001");

        // Assert
        assertTrue(result);
        Optional<Connection> conn = connectionRepository.findByAttributeId(sourceAttr.getId());
        assertTrue(conn.isPresent());
        assertEquals("fk_test_001", conn.get().getForeignKeyName());
    }

    @Test
    @DisplayName("UC03-UT-504: createForeignKeyConnection - Update existing FK connection")
    void createForeignKeyConnection_testChuan2() {
        // Arrange
        connectionService.createForeignKeyConnection(
                sourceAttr.getId(),
                targetModel.getId(),
                targetAttr.getId(),
                "fk_original");

        Model newTargetModel = modelService.createModel("Category", "m_new_target_" + testUsername, 400.0, 100.0, false, testDiagram);
        Attribute newTargetAttr = attributeService.createAttribute(
                "attr_new_target_" + testUsername, newTargetModel, "category_id", "BIGINT", false, 0, false, true);

        // Act
        boolean result = connectionService.createForeignKeyConnection(
                sourceAttr.getId(),
                newTargetModel.getId(),
                newTargetAttr.getId(),
                "fk_updated");

        // Assert
        assertTrue(result);
        Optional<Connection> updated = connectionRepository.findByAttributeId(sourceAttr.getId());
        assertTrue(updated.isPresent());
        assertEquals(newTargetModel.getId(), updated.get().getTargetModel().getId());
        assertEquals("fk_updated", updated.get().getForeignKeyName());
    }

    @Test
    @DisplayName("UC03-UT-505: createForeignKeyConnection - Source attribute not found")
    void createForeignKeyConnection_testNgoaiLe1() {
        // Act
        boolean result = connectionService.createForeignKeyConnection(
                "nonexistent_source",
                targetModel.getId(),
                targetAttr.getId(),
                "fk_test");

        // Assert
        assertFalse(result);
    }

    @Test
    @DisplayName("UC03-UT-506: createForeignKeyConnection - Target model not found")
    void createForeignKeyConnection_testNgoaiLe2() {
        // Act
        boolean result = connectionService.createForeignKeyConnection(
                sourceAttr.getId(),
                "nonexistent_model",
                targetAttr.getId(),
                "fk_test");

        // Assert
        assertFalse(result);
    }

    // ============ TEST: removeForeignKeyConnection() ============

    @Test
    @DisplayName("UC03-UT-507: removeForeignKeyConnection - Successfully remove")
    void removeForeignKeyConnection_testChuan1() {
        // Arrange
        connectionService.createForeignKeyConnection(
                sourceAttr.getId(),
                targetModel.getId(),
                targetAttr.getId(),
                "fk_to_remove");

        // Act
        boolean result = connectionService.removeForeignKeyConnection(sourceAttr.getId());

        // Assert
        assertTrue(result);
        // Verify connection is removed
        Optional<Attribute> attr = attributeRepository.findById(sourceAttr.getId());
        assertTrue(attr.isPresent());
        assertTrue(attr.get().getConnection().isEmpty());
    }

    @Test
    @DisplayName("UC03-UT-508: removeForeignKeyConnection - Attribute not found")
    void removeForeignKeyConnection_testNgoaiLe1() {
        // Act
        boolean result = connectionService.removeForeignKeyConnection("nonexistent");

        // Assert
        assertFalse(result);
    }

    // ============ TEST: removeConnectionsForAttribute() ============

    @Test
    @DisplayName("UC03-UT-509: removeConnectionsForAttribute - Remove connection")
    void removeConnectionsForAttribute_testChuan1() {
        // Arrange
        connectionService.createConnection(
                sourceAttr, targetModel, targetAttr.getId(), "fk_test", "#000000");

        // Act
        connectionService.removeConnectionsForAttribute(sourceAttr.getId());

        // Assert
        Optional<Connection> removed = connectionRepository.findByAttributeId(sourceAttr.getId());
        assertTrue(removed.isEmpty());
    }

    @Test
    @DisplayName("UC03-UT-510: removeConnectionsForAttribute - No connection for attribute")
    void removeConnectionsForAttribute_testChuan2() {
        // Act - Should not throw exception
        assertDoesNotThrow(() ->
            connectionService.removeConnectionsForAttribute("nonexistent"));
    }

    // ============ TEST: removeConnectionsToAttribute() ============

    @Test
    @DisplayName("UC03-UT-511: removeConnectionsToAttribute - Remove incoming connections")
    void removeConnectionsToAttribute_testChuan1() {
        // Arrange
        connectionService.createConnection(
                sourceAttr, targetModel, targetAttr.getId(), "fk_test", "#000000");

        // Act
        connectionService.removeConnectionsToAttribute(targetModel.getId(), targetAttr.getId());

        // Assert
        Optional<Connection> removed = connectionRepository.findByAttributeId(sourceAttr.getId());
        assertTrue(removed.isEmpty());
    }

    @Test
    @DisplayName("UC03-UT-512: removeConnectionsToAttribute - No connections to target")
    void removeConnectionsToAttribute_testChuan2() {
        // Act - Should not throw exception
        assertDoesNotThrow(() ->
            connectionService.removeConnectionsToAttribute(targetModel.getId(), "nonexistent"));
    }


    @Test
    @DisplayName("UC03-UT-513: getConnectionsByDiagram - Multiple connections")
    void getConnectionsByDiagram_testChuan1() {
        // Arrange
        Model model3 = modelService.createModel("Tag", "m_tag_" + testUsername, 200.0, 300.0, false, testDiagram);
        Attribute tagAttr = attributeService.createAttribute(
                "attr_tag_" + testUsername, model3, "post_id", "BIGINT", true, 0, false, false);

        connectionService.createConnection(sourceAttr, targetModel, targetAttr.getId(), "fk1", "#FF0000");
        connectionService.createConnection(tagAttr, sourceModel, sourceAttr.getId(), "fk2", "#00FF00");

        // Act
        List<Connection> connections = connectionService.getConnectionsByDiagram(testDiagram.getId());

        // Assert
        assertNotNull(connections);
        assertTrue(connections.size() >= 2);
    }

    @Test
    @DisplayName("UC03-UT-514: getConnectionsByDiagram - Empty diagram")
    void getConnectionsByDiagram_testChuan2() {
        // Arrange
        DatabaseDiagram emptyDiagram = databaseDiagramService.createBlankDiagram("Empty Diagram", testUsername + "_empty");

        // Act
        List<Connection> connections = connectionService.getConnectionsByDiagram(emptyDiagram.getId());

        // Assert
        assertNotNull(connections);
        assertEquals(0, connections.size());
    }

    // ============ TEST: convertToConnectionDto() ============

    @Test
    @DisplayName("UC03-UT-515: convertToConnectionDto - Standard conversion")
    void convertToConnectionDto_testChuan1() {
        // Arrange
        Connection connection = connectionService.createConnection(
                sourceAttr, targetModel, targetAttr.getId(), "fk_test_dto", "#AABBCC");

        // Act
        var dto = connectionService.convertToConnectionDto(connection);

        // Assert
        assertNotNull(dto);
        assertEquals(targetModel.getId(), dto.getTargetModelId());
        assertEquals(targetAttr.getId(), dto.getTargetAttributeId());
        assertEquals("fk_test_dto", dto.getForeignKeyName());
    }

    @Test
    @DisplayName("UC03-UT-516: convertToConnectionDto - Null target model")
    void convertToConnectionDto_testChuan2() {
        // Arrange
        Connection connection = new Connection();
        connection.setTargetModel(null);
        connection.setTargetAttributeId("attr_test");
        connection.setForeignKeyName("fk_null");
        connection.setIsEnforced(true);

        // Act
        var dto = connectionService.convertToConnectionDto(connection);

        // Assert
        assertNotNull(dto);
        assertEquals("Unknown", dto.getTargetModelId());
    }
}

