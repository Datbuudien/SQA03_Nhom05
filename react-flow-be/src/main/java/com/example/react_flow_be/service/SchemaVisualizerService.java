// src/main/java/com/example/react_flow_be/service/SchemaVisualizerService.java - Enhanced
package com.example.react_flow_be.service;

import com.example.react_flow_be.dto.*;
import com.example.react_flow_be.dto.collaboration.CollaborationDTO;
import com.example.react_flow_be.entity.*;
import com.example.react_flow_be.repository.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import org.springframework.cglib.core.Local;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class SchemaVisualizerService {

    private final DatabaseDiagramService databaseDiagramService;
    private final CollaborationService collaborationService;
    private final ModelService modelService;
    private final AttributeService attributeService;
    private final ConnectionService connectionService;
    private final DatabaseDiagramRepository databaseDiagramRepository;
    private final ModelRepository modelRepository;
    private final AttributeRepository attributeRepository;
    private final ConnectionRepository connectionRepository;

    @Transactional(readOnly = true)
    public DatabaseDiagramDto getSchemaData(Long diagramId, String username) {
        // List<DatabaseDiagram> databaseDiagrams =
        // databaseDiagramService.getAllDatabaseDiagrams();

        // if (!databaseDiagrams.isEmpty()) {
        DatabaseDiagram databaseDiagram = databaseDiagramService.getDatabaseDiagramById(diagramId);

        List<ModelDto> modelDtos = databaseDiagram.getModels().stream()
                .map(modelService::convertToModelDto)
                .collect(Collectors.toList());
        CollaborationDTO collaboration = collaborationService.getUserCollaboration(diagramId, username);

        return new DatabaseDiagramDto(
                databaseDiagram.getId(),
                collaboration.getPermission(),
                databaseDiagram.getName(),
                databaseDiagram.getDescription(),
                databaseDiagram.getDatabaseType().name(),
                databaseDiagram.getVersion(),
                databaseDiagram.getCharset(),
                databaseDiagram.getCollation(),
                databaseDiagram.getIsPublic(),
                databaseDiagram.getIsTemplate(),
                databaseDiagram.getZoomLevel(),
                databaseDiagram.getPanX(),
                databaseDiagram.getPanY(),
                modelDtos);
        // } else {
        // return new DatabaseDiagramDto(
        // null, "Empty Diagram", "No data available",
        // "MYSQL", "8.0", "utf8mb4", "utf8mb4_unicode_ci",
        // false, false, 1.0, 0.0, 0.0, List.of());
        // }
    }

    @Transactional
    public void initializeSampleData() {

        // Create sample data using other services
        // User sampleUser = databaseDiagramService.createSampleUser();
        DatabaseDiagram databaseDiagram = databaseDiagramService.createSampleDatabaseDiagram();

        // Create Models
        Model userModel = modelService.createModel("User", "User", 100.0, 300.0, true, databaseDiagram);
        Model postModel = modelService.createModel("Post", "Post", 500.0, 100.0, true, databaseDiagram);
        Model commentModel = modelService.createModel("Comment", "Comment", 500.0, 500.0, false, databaseDiagram);

        // Create Attributes for User model
        attributeService.createAttribute("1", userModel, "id", "BIGINT", false, 0, false, true);
        attributeService.createAttribute("2", userModel, "username", "VARCHAR", false, 1, false, false);
        attributeService.createAttribute("3", userModel, "email", "VARCHAR", false, 2, false, false);
        attributeService.createAttribute("4", userModel, "password", "VARCHAR", false, 3, false, false);
        attributeService.createAttribute("5", userModel, "full_name", "VARCHAR", false, 4, true, false);
        attributeService.createAttribute("6", userModel, "created_at", "TIMESTAMP", false, 5, false, false);
        attributeService.createAttribute("7", userModel, "updated_at", "TIMESTAMP", false, 6, false, false);

        // Create Attributes for Post model
        attributeService.createAttribute("8", postModel, "id", "BIGINT", false, 0, false, true);
        attributeService.createAttribute("9", postModel, "title", "VARCHAR", false, 1, false, false);
        attributeService.createAttribute("10", postModel, "content", "TEXT", false, 2, true, false);
        attributeService.createAttribute("11", postModel, "user_id", "BIGINT", true, 3, false, false);
        attributeService.createAttribute("12", postModel, "status", "VARCHAR", false, 4, false, false);
        attributeService.createAttribute("13", postModel, "published_at", "TIMESTAMP", false, 5, true, false);
        attributeService.createAttribute("14", postModel, "created_at", "TIMESTAMP", false, 6, false, false);
        attributeService.createAttribute("15", postModel, "updated_at", "TIMESTAMP", false, 7, false, false);

        // Create Attributes for Comment model
        attributeService.createAttribute("16", commentModel, "id", "BIGINT", false, 0, false, true);
        attributeService.createAttribute("17", commentModel, "content", "TEXT", false, 1, false, false);
        attributeService.createAttribute("18", commentModel, "post_id", "BIGINT", true, 2, false, false);
        attributeService.createAttribute("19", commentModel, "user_id", "BIGINT", true, 3, false, false);
        attributeService.createAttribute("20", commentModel, "parent_comment_id", "BIGINT", true, 4, true, false);
        attributeService.createAttribute("21", commentModel, "status", "VARCHAR", false, 5, false, false);
        attributeService.createAttribute("22", commentModel, "created_at", "TIMESTAMP", false, 6, false, false);
        attributeService.createAttribute("23", commentModel, "updated_at", "TIMESTAMP", false, 7, false, false);

        // Create Connections
        Attribute postUserIdAttribute = attributeService.getAttributeByModelAndName(postModel, "user_id");
        Attribute commentPostIdAttribute = attributeService.getAttributeByModelAndName(commentModel, "post_id");
        Attribute commentUserIdAttribute = attributeService.getAttributeByModelAndName(commentModel, "user_id");
        Attribute commentParentIdAttribute = attributeService.getAttributeByModelAndName(commentModel,
                "parent_comment_id");

        // Post -> User relationship
        if (postUserIdAttribute != null) {
            connectionService.createConnection(postUserIdAttribute, userModel, "1",
                    "fk_post_user", "#4A90E2");
        }

        // Comment -> Post relationship
        if (commentPostIdAttribute != null) {
            connectionService.createConnection(commentPostIdAttribute, postModel, "8",
                    "fk_comment_post", "#E53E3E");
        }

        // Comment -> User relationship
        if (commentUserIdAttribute != null) {
            connectionService.createConnection(commentUserIdAttribute, userModel, "1",
                    "fk_comment_user", "#38A169");
        }

        // Comment -> Comment (self-reference) relationship
        if (commentParentIdAttribute != null) {
            connectionService.createConnection(commentParentIdAttribute, commentModel, "16",
                    "fk_comment_parent", "#9CA3AF");
        }

        log.info("Sample data initialized successfully");
    }

    @Transactional
    public boolean updateModelPosition(String modelId, Double positionX, Double positionY, LocalDateTime timestamp) {
        return modelService.updateModelPosition(modelId, positionX, positionY, timestamp);
    }

    @Transactional
    public boolean updateAttributeName(String attributeId, String attributeName, LocalDateTime timestamp) {
        return attributeService.updateAttributeName(attributeId, attributeName, timestamp);
    }

    @Transactional
    public boolean updateAttributeType(String attributeId, String attributeType, LocalDateTime timestamp) {
        return attributeService.updateAttributeType(attributeId, attributeType, timestamp);
    }

    public boolean setAttributeAsPrimaryKey(String attributeId, LocalDateTime timestamp) {
        return attributeService.setAttributeAsPrimaryKey(attributeId, timestamp);
    }

    public boolean setAttributeAsForeignKey(String attributeId, LocalDateTime timestamp) {
        return attributeService.setAttributeAsForeignKey(attributeId, timestamp);
    }

    public boolean setAttributeAsNormal(String attributeId, LocalDateTime timestamp) {
        return attributeService.setAttributeAsNormal(attributeId, timestamp);
    }

    @Transactional
    public String addAttribute(String modelId, String attributeId, String attributeName, String dataType) {
        return attributeService.addAttribute(modelId, attributeId, attributeName, dataType);
    }

    @Transactional
    public boolean deleteAttribute(String attributeId) {
        return attributeService.deleteAttribute(attributeId);
    }

    @Transactional
    public boolean createForeignKeyConnection(
            String attributeId,
            String targetModelId,
            String targetAttributeId,
            String foreignKeyName) {

        return connectionService.createForeignKeyConnection(
                attributeId, targetModelId, targetAttributeId, foreignKeyName);
    }

    @Transactional
    public boolean removeForeignKeyConnection(String attributeId) {
        log.info("Removing FK connection for attributeId={}", attributeId);
        return connectionService.removeForeignKeyConnection(attributeId);
    }

    // Thêm vào SchemaVisualizerService.java
    @Transactional
    public String addModel(String name, Long databaseDiagramId, String modelId, Double positionX, Double positionY) {
        try {
            Optional<DatabaseDiagram> diagramOpt = databaseDiagramRepository.findById(databaseDiagramId);
            if (diagramOpt.isPresent()) {
                DatabaseDiagram diagram = diagramOpt.get();

                Model newModel = modelService.createModel(name, modelId, positionX, positionY, false, diagram);

                log.info("Added new model: {} at position ({}, {})", modelId, positionX, positionY);
                return newModel.getId();
            }
            return null;
        } catch (Exception e) {
            log.error("Error adding model: {}", e.getMessage(), e);
            return null;
        }
    }

    @Transactional
    public boolean updateModelName(String modelId, String newModelName, LocalDateTime timestamp) {
        try {
            Optional<Model> modelOpt = modelRepository.findByIdForUpdate(modelId);
            if (modelOpt.isPresent() && (modelOpt.get().getNameUpdatedAt() == null
                    || modelOpt.get().getNameUpdatedAt().isBefore(timestamp))) {
                Model model = modelOpt.get();
                String oldName = model.getName();

                model.setName(newModelName);
                model.setNodeId(newModelName);
                model.setNameUpdatedAt(timestamp);
                modelRepository.save(model);

                log.info("Updated model name from {} to {}", oldName, newModelName);
                return true;
            }
            return false;
        } catch (Exception e) {
            log.error("Error updating model name: {}", e.getMessage(), e);
            return false;
        }
    }

    @Transactional
    public boolean deleteModel(String modelId) {
        try {
            Optional<Model> modelOpt = modelRepository.findById(modelId);
            if (modelOpt.isPresent()) {
                Model model = modelOpt.get();
                String modelName = model.getName();

                log.info("Deleting model: {}", modelName);
                // List<Connection> incomingConnections =
                // connectionRepository.findByTargetModelId(modelId);
                // if (!incomingConnections.isEmpty()) {
                // System.out.println(incomingConnections.get(0).getId());
                // connectionRepository.deleteAll(incomingConnections);
                // log.info("Removed {} incoming connections", incomingConnections.size());
                // List<Connection> checkConnections =
                // connectionRepository.findByTargetModelId(modelId);
                // System.out.println(checkConnections.get(0).getId());
                // }
                modelRepository.deleteById(modelId);

                log.info("Successfully deleted model: {} and all related data", modelName);
                return true;
            }
            return false;
        } catch (Exception e) {
            log.error("Error deleting model {}: {}", modelId, e.getMessage(), e);
            return false;
        }
    }
}