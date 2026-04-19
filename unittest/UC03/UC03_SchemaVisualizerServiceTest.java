package com.example.react_flow_be.unittest;

import com.example.react_flow_be.dto.DatabaseDiagramDto;
import com.example.react_flow_be.entity.Attribute;
import com.example.react_flow_be.entity.DatabaseDiagram;
import com.example.react_flow_be.entity.Model;
import com.example.react_flow_be.repository.AttributeRepository;
import com.example.react_flow_be.repository.ModelRepository;
import com.example.react_flow_be.service.AttributeService;
import com.example.react_flow_be.service.DatabaseDiagramService;
import com.example.react_flow_be.service.ModelService;
import com.example.react_flow_be.service.SchemaVisualizerService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@ActiveProfiles("test")
@Transactional
@DisplayName("UC03-SchemaVisualizerService Unit Tests")
class UC03_SchemaVisualizerServiceTest {

    @Autowired
    private SchemaVisualizerService schemaVisualizerService;

    @Autowired
    private DatabaseDiagramService databaseDiagramService;

    @Autowired
    private ModelService modelService;

    @Autowired
    private AttributeService attributeService;

    @Autowired
    private ModelRepository modelRepository;

    @Autowired
    private AttributeRepository attributeRepository;

    private DatabaseDiagram testDiagram;
    private String testUsername;

    @BeforeEach
    void setUp() {
        testUsername = "test_owner_schema_" + System.currentTimeMillis();
        testDiagram = databaseDiagramService.createBlankDiagram("Test Schema Diagram", testUsername);
    }

    // ============ TEST: getSchemaData() ============

    @Test
    @DisplayName("UC03-UT-601: getSchemaData - Retrieve schema with models")
    void getSchemaData_testChuan1() {
        // Arrange
        Model model1 = modelService.createModel("User", "m_schema_001", 100.0, 100.0, false, testDiagram);
        Model model2 = modelService.createModel("Post", "m_schema_002", 300.0, 100.0, false, testDiagram);

        attributeService.createAttribute("attr_001", model1, "id", "BIGINT", false, 0, false, true);
        attributeService.createAttribute("attr_002", model2, "id", "BIGINT", false, 0, false, true);

        // Act
        DatabaseDiagramDto schemaData = schemaVisualizerService.getSchemaData(testDiagram.getId(), testUsername);

        // Assert
        assertNotNull(schemaData);
        assertEquals(testDiagram.getId(), schemaData.getId());
        assertEquals("Test Schema Diagram", schemaData.getName());
        assertEquals(2, schemaData.getModels().size());
    }

    @Test
    @DisplayName("UC03-UT-602: getSchemaData - Empty diagram")
    void getSchemaData_testChuan2() {
        // Act
        DatabaseDiagramDto schemaData = schemaVisualizerService.getSchemaData(testDiagram.getId(), testUsername);

        // Assert
        assertNotNull(schemaData);
        assertEquals(0, schemaData.getModels().size());
    }

    @Test
    @DisplayName("UC03-UT-603: getSchemaData - Diagram not found")
    void getSchemaData_testNgoaiLe1() {
        // Act & Assert
        assertThrows(Exception.class,
            () -> schemaVisualizerService.getSchemaData(999999L, testUsername));
    }

    // ============ TEST: initializeSampleData() ============

    @Test
    @DisplayName("UC03-UT-604: initializeSampleData - Sample data created")
    void initializeSampleData_testChuan1() {
        // Act
        schemaVisualizerService.initializeSampleData();

        // Assert
        var diagrams = databaseDiagramService.getAllDatabaseDiagrams();
        assertTrue(diagrams.stream().anyMatch(d -> "Blog System".equals(d.getName())));
    }

    @Test
    @DisplayName("UC03-UT-605: initializeSampleData - Models created with attributes")
    void initializeSampleData_testChuan2() {
        // Act
        schemaVisualizerService.initializeSampleData();

        // Assert
        var allModels = modelRepository.findAll();
        assertTrue(allModels.stream().anyMatch(m -> "User".equals(m.getName())));
        assertTrue(allModels.stream().anyMatch(m -> "Post".equals(m.getName())));
        assertTrue(allModels.stream().anyMatch(m -> "Comment".equals(m.getName())));
    }

    // ============ TEST: addModel() ============

    @Test
    @DisplayName("UC03-UT-606: addModel - Add model successfully")
    void addModel_testChuan1() {
        // Act
        String modelId = schemaVisualizerService.addModel(
                "Product", testDiagram.getId(), "m_product_001", 150.0, 250.0);

        // Assert
        assertNotNull(modelId);
        assertEquals("m_product_001", modelId);
        Optional<Model> saved = modelRepository.findById("m_product_001");
        assertTrue(saved.isPresent());
        assertEquals(150.0, saved.get().getPositionX());
        assertEquals(250.0, saved.get().getPositionY());
    }

    @Test
    @DisplayName("UC03-UT-607: addModel - Diagram not found")
    void addModel_testNgoaiLe1() {
        // Act
        String result = schemaVisualizerService.addModel(
                "Product", 999999L, "m_product_002", 100.0, 100.0);

        // Assert
        assertNull(result);
        assertTrue(modelRepository.findById("m_product_002").isEmpty());
    }

    @Test
    @DisplayName("UC03-UT-608: addModel - Multiple models in same diagram")
    void addModel_testChuan2() {
        // Act
        String modelId1 = schemaVisualizerService.addModel(
                "User", testDiagram.getId(), "m_user_003", 100.0, 100.0);
        String modelId2 = schemaVisualizerService.addModel(
                "Post", testDiagram.getId(), "m_post_003", 300.0, 100.0);
        String modelId3 = schemaVisualizerService.addModel(
                "Comment", testDiagram.getId(), "m_comment_003", 500.0, 100.0);

        // Assert
        assertNotNull(modelId1);
        assertNotNull(modelId2);
        assertNotNull(modelId3);
        assertEquals(3, modelRepository.findByDatabaseDiagramId(testDiagram.getId()).size());
    }

    // ============ TEST: updateModelName() ============

    @Test
    @DisplayName("UC03-UT-609: updateModelName - Successful update with newer timestamp")
    void updateModelName_testChuan1() {
        // Arrange
        String modelId = schemaVisualizerService.addModel(
                "Table1", testDiagram.getId(), "m_upd_001", 100.0, 100.0);
        LocalDateTime now = LocalDateTime.now();

        // Act
        boolean result = schemaVisualizerService.updateModelName(modelId, "UpdatedTable1", now);

        // Assert
        assertTrue(result);
        Optional<Model> updated = modelRepository.findById(modelId);
        assertTrue(updated.isPresent());
        assertEquals("UpdatedTable1", updated.get().getName());
        assertEquals(now, updated.get().getNameUpdatedAt());
    }

    @Test
    @DisplayName("UC03-UT-610: updateModelName - Old timestamp rejected")
    void updateModelName_testChuan2() {
        // Arrange
        String modelId = schemaVisualizerService.addModel(
                "Table2", testDiagram.getId(), "m_upd_002", 100.0, 100.0);
        LocalDateTime newTime = LocalDateTime.now();
        schemaVisualizerService.updateModelName(modelId, "FirstUpdate", newTime);

        // Act
        boolean result = schemaVisualizerService.updateModelName(
                modelId, "SecondUpdate", newTime.minusMinutes(5));

        // Assert
        assertFalse(result);
        Optional<Model> unchanged = modelRepository.findById(modelId);
        assertTrue(unchanged.isPresent());
        assertEquals("FirstUpdate", unchanged.get().getName());
    }

    @Test
    @DisplayName("UC03-UT-611: updateModelName - Model not found")
    void updateModelName_testNgoaiLe1() {
        // Act
        boolean result = schemaVisualizerService.updateModelName(
                "nonexistent", "NewName", LocalDateTime.now());

        // Assert
        assertFalse(result);
    }

    // ============ TEST: updateModelPosition() ============

    @Test
    @DisplayName("UC03-UT-612: updateModelPosition - Successful position update")
    void updateModelPosition_testChuan1() {
        // Arrange
        String modelId = schemaVisualizerService.addModel(
                "Box1", testDiagram.getId(), "m_pos_001", 100.0, 100.0);
        LocalDateTime now = LocalDateTime.now();

        // Act
        boolean result = schemaVisualizerService.updateModelPosition(
                modelId, 200.0, 300.0, now);

        // Assert
        assertTrue(result);
        Optional<Model> updated = modelRepository.findById(modelId);
        assertTrue(updated.isPresent());
        assertEquals(200.0, updated.get().getPositionX());
        assertEquals(300.0, updated.get().getPositionY());
    }

    // ============ TEST: deleteModel() ============

    @Test
    @DisplayName("UC03-UT-613: deleteModel - Successfully delete model")
    void deleteModel_testChuan1() {
        // Arrange
        String modelId = schemaVisualizerService.addModel(
                "ToDelete", testDiagram.getId(), "m_del_001", 100.0, 100.0);

        // Act
        boolean result = schemaVisualizerService.deleteModel(modelId);

        // Assert
        assertTrue(result);
        assertTrue(modelRepository.findById(modelId).isEmpty());
    }

    @Test
    @DisplayName("UC03-UT-614: deleteModel - Model with attributes")
    void deleteModel_testChuan2() {
        // Arrange
        String modelId = schemaVisualizerService.addModel(
                "WithAttrs", testDiagram.getId(), "m_del_002", 100.0, 100.0);
        Model model = modelRepository.findById(modelId).orElseThrow();
        attributeService.createAttribute("attr_del_001", model, "id", "BIGINT", false, 0, false, true);
        attributeService.createAttribute("attr_del_002", model, "name", "VARCHAR", false, 1, true, false);

        // Act
        boolean result = schemaVisualizerService.deleteModel(modelId);

        // Assert
        assertTrue(result);
        assertTrue(modelRepository.findById(modelId).isEmpty());
        assertTrue(attributeRepository.findByModelIdOrderByAttributeOrder(modelId).isEmpty());
    }

    @Test
    @DisplayName("UC03-UT-615: deleteModel - Model not found")
    void deleteModel_testNgoaiLe1() {
        // Act
        boolean result = schemaVisualizerService.deleteModel("nonexistent");

        // Assert
        assertFalse(result);
    }

    // ============ TEST: addAttribute() ============

    @Test
    @DisplayName("UC03-UT-616: addAttribute - Add attribute to model")
    void addAttribute_testChuan1() {
        // Arrange
        String modelId = schemaVisualizerService.addModel(
                "Article", testDiagram.getId(), "m_article_001", 100.0, 100.0);

        // Act
        String attrId = schemaVisualizerService.addAttribute(
                modelId, "attr_art_001", "title", "VARCHAR");

        // Assert
        assertNotNull(attrId);
        Optional<Attribute> saved = attributeRepository.findById(attrId);
        assertTrue(saved.isPresent());
        assertEquals("title", saved.get().getName());
    }

    @Test
    @DisplayName("UC03-UT-617: addAttribute - Model not found")
    void addAttribute_testNgoaiLe1() {
        // Act
        String result = schemaVisualizerService.addAttribute(
                "nonexistent", "attr_xxx", "field", "VARCHAR");

        // Assert
        assertNull(result);
    }

    // ============ TEST: updateAttributeName() ============

    @Test
    @DisplayName("UC03-UT-618: updateAttributeName - Update attribute name")
    void updateAttributeName_testChuan1() {
        // Arrange
        String modelId = schemaVisualizerService.addModel(
                "Table", testDiagram.getId(), "m_tbl_001", 100.0, 100.0);
        Model model = modelRepository.findById(modelId).orElseThrow();
        Attribute attr = attributeService.createAttribute(
                "attr_name_001", model, "oldName", "VARCHAR", false, 0, true, false);
        LocalDateTime now = LocalDateTime.now();

        // Act
        boolean result = schemaVisualizerService.updateAttributeName(
                attr.getId(), "newName", now);

        // Assert
        assertTrue(result);
        Optional<Attribute> updated = attributeRepository.findById(attr.getId());
        assertTrue(updated.isPresent());
        assertEquals("newName", updated.get().getName());
    }

    // ============ TEST: updateAttributeType() ============

    @Test
    @DisplayName("UC03-UT-619: updateAttributeType - Update data type")
    void updateAttributeType_testChuan1() {
        // Arrange
        String modelId = schemaVisualizerService.addModel(
                "Data", testDiagram.getId(), "m_data_001", 100.0, 100.0);
        Model model = modelRepository.findById(modelId).orElseThrow();
        Attribute attr = attributeService.createAttribute(
                "attr_type_001", model, "amount", "INT", false, 0, true, false);
        LocalDateTime now = LocalDateTime.now();

        // Act
        boolean result = schemaVisualizerService.updateAttributeType(
                attr.getId(), "DECIMAL", now);

        // Assert
        assertTrue(result);
        Optional<Attribute> updated = attributeRepository.findById(attr.getId());
        assertTrue(updated.isPresent());
        assertEquals("DECIMAL", updated.get().getDataType());
    }

    // ============ TEST: setAttributeAsPrimaryKey() ============

    @Test
    @DisplayName("UC03-UT-620: setAttributeAsPrimaryKey - Mark as PK")
    void setAttributeAsPrimaryKey_testChuan1() {
        // Arrange
        String modelId = schemaVisualizerService.addModel(
                "PK_Test", testDiagram.getId(), "m_pk_001", 100.0, 100.0);
        Model model = modelRepository.findById(modelId).orElseThrow();
        Attribute attr = attributeService.createAttribute(
                "attr_pk_mark", model, "id", "BIGINT", false, 0, true, false);

        // Act
        boolean result = schemaVisualizerService.setAttributeAsPrimaryKey(
                attr.getId(), LocalDateTime.now());

        // Assert
        assertTrue(result);
        Optional<Attribute> updated = attributeRepository.findById(attr.getId());
        assertTrue(updated.isPresent());
        assertTrue(updated.get().getIsPrimaryKey());
        assertFalse(updated.get().getIsNullable());
    }

    // ============ TEST: setAttributeAsForeignKey() ============

    @Test
    @DisplayName("UC03-UT-621: setAttributeAsForeignKey - Mark as FK")
    void setAttributeAsForeignKey_testChuan1() {
        // Arrange
        String modelId = schemaVisualizerService.addModel(
                "FK_Test", testDiagram.getId(), "m_fk_001", 100.0, 100.0);
        Model model = modelRepository.findById(modelId).orElseThrow();
        Attribute attr = attributeService.createAttribute(
                "attr_fk_mark", model, "user_id", "BIGINT", false, 0, true, false);

        // Act
        boolean result = schemaVisualizerService.setAttributeAsForeignKey(
                attr.getId(), LocalDateTime.now());

        // Assert
        assertTrue(result);
        Optional<Attribute> updated = attributeRepository.findById(attr.getId());
        assertTrue(updated.isPresent());
        assertTrue(updated.get().getIsForeignKey());
    }

    // ============ TEST: setAttributeAsNormal() ============

    @Test
    @DisplayName("UC03-UT-622: setAttributeAsNormal - Mark as normal")
    void setAttributeAsNormal_testChuan1() {
        // Arrange
        String modelId = schemaVisualizerService.addModel(
                "Normal_Test", testDiagram.getId(), "m_normal_001", 100.0, 100.0);
        Model model = modelRepository.findById(modelId).orElseThrow();
        Attribute attr = attributeService.createAttribute(
                "attr_normal_mark", model, "status", "VARCHAR", false, 0, false, true);

        // Act
        boolean result = schemaVisualizerService.setAttributeAsNormal(
                attr.getId(), LocalDateTime.now());

        // Assert
        assertTrue(result);
        Optional<Attribute> updated = attributeRepository.findById(attr.getId());
        assertTrue(updated.isPresent());
        assertFalse(updated.get().getIsPrimaryKey());
        assertFalse(updated.get().getIsForeignKey());
        assertTrue(updated.get().getIsNullable());
    }

    // ============ TEST: deleteAttribute() ============

    @Test
    @DisplayName("UC03-UT-623: deleteAttribute - Delete existing attribute")
    void deleteAttribute_testChuan1() {
        // Arrange
        String modelId = schemaVisualizerService.addModel(
                "Del_Test", testDiagram.getId(), "m_del_attr", 100.0, 100.0);
        Model model = modelRepository.findById(modelId).orElseThrow();
        Attribute attr = attributeService.createAttribute(
                "attr_to_del", model, "temp", "VARCHAR", false, 0, true, false);

        // Act
        boolean result = schemaVisualizerService.deleteAttribute(attr.getId());

        // Assert
        assertTrue(result);
        assertTrue(attributeRepository.findById(attr.getId()).isEmpty());
    }

    @Test
    @DisplayName("UC03-UT-624: deleteAttribute - Attribute not found")
    void deleteAttribute_testNgoaiLe1() {
        // Act
        boolean result = schemaVisualizerService.deleteAttribute("nonexistent");

        // Assert
        assertFalse(result);
    }

    // ============ TEST: createForeignKeyConnection() ============

    @Test
    @DisplayName("UC03-UT-625: createForeignKeyConnection - Create FK connection")
    void createForeignKeyConnection_testChuan1() {
        // Arrange
        String userId = schemaVisualizerService.addModel(
                "User", testDiagram.getId(), "m_user_fk", 100.0, 100.0);
        String postId = schemaVisualizerService.addModel(
                "Post", testDiagram.getId(), "m_post_fk", 300.0, 100.0);

        Model userModel = modelRepository.findById(userId).orElseThrow();
        Model postModel = modelRepository.findById(postId).orElseThrow();

        Attribute userIdAttr = attributeService.createAttribute(
                "attr_user_id", userModel, "id", "BIGINT", false, 0, false, true);
        Attribute postUserIdAttr = attributeService.createAttribute(
                "attr_post_user_id", postModel, "user_id", "BIGINT", true, 0, false, false);

        // Act
        boolean result = schemaVisualizerService.createForeignKeyConnection(
                postUserIdAttr.getId(), userId, userIdAttr.getId(), "fk_post_user");

        // Assert
        assertTrue(result);
    }

    // ============ TEST: removeForeignKeyConnection() ============

    @Test
    @DisplayName("UC03-UT-626: removeForeignKeyConnection - Remove FK")
    void removeForeignKeyConnection_testChuan1() {
        // Arrange
        String userId = schemaVisualizerService.addModel(
                "User", testDiagram.getId(), "m_user_rmfk", 100.0, 100.0);
        String postId = schemaVisualizerService.addModel(
                "Post", testDiagram.getId(), "m_post_rmfk", 300.0, 100.0);

        Model userModel = modelRepository.findById(userId).orElseThrow();
        Model postModel = modelRepository.findById(postId).orElseThrow();

        Attribute userIdAttr = attributeService.createAttribute(
                "attr_user_rmfk", userModel, "id", "BIGINT", false, 0, false, true);
        Attribute postUserIdAttr = attributeService.createAttribute(
                "attr_post_rmfk", postModel, "user_id", "BIGINT", true, 0, false, false);

        schemaVisualizerService.createForeignKeyConnection(
                postUserIdAttr.getId(), userId, userIdAttr.getId(), "fk_rm");

        // Act
        boolean result = schemaVisualizerService.removeForeignKeyConnection(postUserIdAttr.getId());

        // Assert
        assertTrue(result);
    }
}

