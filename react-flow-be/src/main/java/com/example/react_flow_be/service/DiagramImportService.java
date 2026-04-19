package com.example.react_flow_be.service;

import com.example.react_flow_be.entity.*;
import com.example.react_flow_be.repository.*;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;

@Service
@RequiredArgsConstructor
@Slf4j
public class DiagramImportService {

    private final DatabaseDiagramRepository databaseDiagramRepository;
    private final ModelRepository modelRepository;
    private final AttributeRepository attributeRepository;
    private final ConnectionRepository connectionRepository;
    private final CollaborationService collaborationService;
    private final ObjectMapper objectMapper;

    @Transactional
    public Long importDiagramFromJson(String diagramName, String jsonContent, String username) throws Exception {
        log.info("Starting diagram import for: {} by user: {}", diagramName, username);

        // Parse JSON
        JsonNode rootNode = objectMapper.readTree(jsonContent);
        JsonNode schemaNode = rootNode.path("schema");
        JsonNode tablesNode = rootNode.path("tables");

        // Create DatabaseDiagram
        DatabaseDiagram diagram = new DatabaseDiagram();
        diagram.setName(diagramName);
        diagram.setDescription(schemaNode.path("description").asText(""));
        // diagram.setType(Diagram.DiagramType.ER_DIAGRAM);
        diagram.setDatabaseType(DatabaseDiagram.DatabaseType.MYSQL);
        diagram.setVersion("8.0");
        diagram.setCharset("utf8mb4");
        diagram.setCollation("utf8mb4_unicode_ci");
        diagram.setIsPublic(false);
        diagram.setIsTemplate(false);
        diagram.setZoomLevel(1.0);
        diagram.setPanX(0.0);
        diagram.setPanY(0.0);

        diagram = databaseDiagramRepository.save(diagram);
        log.info("Created diagram with ID: {}", diagram.getId());

        // Create owner collaboration
        collaborationService.createOwner(diagram.getId(), username);
        log.info("Created owner collaboration for user: {}", username);

        // Maps to track old ID -> new ID
        Map<String, String> modelIdMap = new HashMap<>();
        Map<String, String> attributeIdMap = new HashMap<>();
        List<ConnectionMapping> connectionMappings = new ArrayList<>();

        // First pass: Create models and attributes
        for (JsonNode tableNode : tablesNode) {
            String oldModelId = tableNode.path("id").asText();
            String newModelId = UUID.randomUUID().toString();
            modelIdMap.put(oldModelId, newModelId);

            // Create Model
            Model model = new Model();
            model.setId(newModelId);
            model.setName(tableNode.path("name").asText());
            model.setNodeId(tableNode.path("name").asText());
            model.setPositionX(tableNode.path("position").path("x").asDouble());
            model.setPositionY(tableNode.path("position").path("y").asDouble());
            model.setDatabaseDiagram(diagram);

            model = modelRepository.save(model);
            log.info("Created model: {} with new ID: {}", model.getName(), newModelId);

            // Create Attributes
            JsonNode attributesNode = tableNode.path("attributes");
            int order = 0;
            for (JsonNode attrNode : attributesNode) {
                String oldAttrId = attrNode.path("id").asText();
                String newAttrId = UUID.randomUUID().toString();
                attributeIdMap.put(oldAttrId, newAttrId);

                Attribute attribute = new Attribute();
                attribute.setId(newAttrId);
                attribute.setName(attrNode.path("name").asText());
                attribute.setDataType(attrNode.path("dataType").asText());
                attribute.setIsNullable(attrNode.path("isNullable").asBoolean(true));
                attribute.setIsPrimaryKey(attrNode.path("isPrimaryKey").asBoolean(false));
                attribute.setIsForeignKey(attrNode.path("isForeignKey").asBoolean(false));
                attribute.setIsUnique(attrNode.path("isUnique").asBoolean(false));
                attribute.setIsAutoIncrement(attrNode.path("isAutoIncrement").asBoolean(false));
                attribute.setAttributeOrder(order++);
                attribute.setModel(model);

                // Set default length for VARCHAR
                if ("VARCHAR".equals(attribute.getDataType())) {
                    attribute.setLength(255);
                } else if ("BIGINT".equals(attribute.getDataType()) || "INT".equals(attribute.getDataType())) {
                    attribute.setLength(20);
                }

                // Handle default value
                if (attrNode.has("defaultValue") && !attrNode.path("defaultValue").isNull()) {
                    attribute.setDefaultValue(attrNode.path("defaultValue").asText());
                }

                // Handle comment
                if (attrNode.has("comment") && !attrNode.path("comment").isNull()) {
                    attribute.setComment(attrNode.path("comment").asText());
                }

                // Set index information for PK
                if (attribute.getIsPrimaryKey()) {
                    attribute.setHasIndex(true);
                    attribute.setIndexName("PRIMARY_" + attribute.getName());
                    attribute.setIndexType(Attribute.IndexType.PRIMARY);
                } else if (attribute.getIsForeignKey()) {
                    attribute.setHasIndex(true);
                    attribute.setIndexName("idx_" + model.getName().toLowerCase() + "_" + attribute.getName());
                    attribute.setIndexType(Attribute.IndexType.INDEX);
                }

                attribute = attributeRepository.save(attribute);
                log.debug("Created attribute: {} with new ID: {}", attribute.getName(), newAttrId);

                // Store connection info for second pass
                JsonNode connectionNode = attrNode.path("connection");
                if (!connectionNode.isMissingNode() && !connectionNode.isNull()) {
                    ConnectionMapping mapping = new ConnectionMapping();
                    mapping.sourceAttributeId = newAttrId;
                    mapping.targetModelId = connectionNode.path("targetModelId").asText();
                    mapping.targetAttributeId = connectionNode.path("targetAttributeId").asText();
                    mapping.foreignKeyName = connectionNode.path("foreignKeyName").asText(null);
                    mapping.isEnforced = connectionNode.path("isEnforced").asBoolean(true);
                    connectionMappings.add(mapping);
                }
            }
        }

        // Second pass: Create connections with new IDs
        for (ConnectionMapping mapping : connectionMappings) {
            try {
                String newTargetModelId = modelIdMap.get(mapping.targetModelId);
                String newTargetAttributeId = attributeIdMap.get(mapping.targetAttributeId);

                if (newTargetModelId == null || newTargetAttributeId == null) {
                    log.warn("Skipping connection - target not found. Old target model: {}, attribute: {}",
                            mapping.targetModelId, mapping.targetAttributeId);
                    continue;
                }

                Optional<Attribute> sourceAttrOpt = attributeRepository.findById(mapping.sourceAttributeId);
                Optional<Model> targetModelOpt = modelRepository.findById(newTargetModelId);

                if (sourceAttrOpt.isPresent() && targetModelOpt.isPresent()) {
                    Connection connection = new Connection();
                    connection.setAttribute(sourceAttrOpt.get());
                    connection.setTargetModel(targetModelOpt.get());
                    connection.setTargetAttributeId(newTargetAttributeId);
                    connection.setForeignKeyName(mapping.foreignKeyName);
                    connection.setIsEnforced(mapping.isEnforced);

                    connectionRepository.save(connection);
                    log.debug("Created connection from attribute {} to model {}",
                            mapping.sourceAttributeId, newTargetModelId);
                }
            } catch (Exception e) {
                log.error("Error creating connection: {}", e.getMessage());
            }
        }

        log.info("Successfully imported diagram: {} with {} models", diagramName, modelIdMap.size());
        return diagram.getId();
    }

    // Helper class to store connection mappings
    private static class ConnectionMapping {
        String sourceAttributeId;
        String targetModelId;
        String targetAttributeId;
        String foreignKeyName;
        boolean isEnforced;
    }
}