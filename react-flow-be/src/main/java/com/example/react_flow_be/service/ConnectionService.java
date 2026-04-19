package com.example.react_flow_be.service;

import com.example.react_flow_be.dto.ConnectionDto;
import com.example.react_flow_be.entity.*;
import com.example.react_flow_be.repository.ConnectionRepository;
import com.example.react_flow_be.repository.AttributeRepository;
import com.example.react_flow_be.repository.ModelRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class ConnectionService {

    private final ConnectionRepository connectionRepository;
    private final AttributeRepository attributeRepository;
    private final ModelRepository modelRepository;

    @Transactional(readOnly = true)
    public List<Connection> getConnectionsByDiagram(Long diagramId) {
        return connectionRepository.findAll().stream()
                .filter(conn -> conn.getAttribute() != null &&
                        conn.getAttribute().getModel().getDatabaseDiagram().getId().equals(diagramId))
                .collect(Collectors.toList());
    }

    @Transactional
    public Connection createConnection(Attribute sourceAttribute, Model targetModel, String targetAttributeId,
            String fkName,
            String color) {
        Connection connection = new Connection();

        connection.setTargetAttributeId(targetAttributeId);
        connection.setForeignKeyName(fkName);
        connection.setAttribute(sourceAttribute);
        connection.setTargetModel(targetModel);
        connection.setIsEnforced(true);

        return connectionRepository.save(connection);
    }

    @Transactional
    public boolean createForeignKeyConnection(
            String attributeId,
            String targetModelId,
            String targetAttributeId,
            String foreignKeyName) {

        try {

            // Find source attribute
            Optional<Attribute> sourceAttributeOpt = attributeRepository.findById(attributeId);
            if (!sourceAttributeOpt.isPresent()) {
                log.error("Source attribute not found: {}", attributeId);
                return false;
            }

            Attribute sourceAttribute = sourceAttributeOpt.get();

            // Find target model
            Optional<Model> targetModelOpt = modelRepository.findById(targetModelId);
            if (!targetModelOpt.isPresent()) {
                log.error("Target model not found: {}", targetModelId);
                return false;
            }

            Model targetModel = targetModelOpt.get();

            // Check if connection already exists
            Optional<Connection> existingConnection = connectionRepository.findByAttributeIdForUpdate(attributeId);
            if (existingConnection.isPresent()) {
                // Update existing connection
                Connection connection = existingConnection.get();
                connection.setTargetModel(targetModel);
                connection.setTargetAttributeId(targetAttributeId);
                connection.setForeignKeyName(foreignKeyName);
                connectionRepository.save(connection);
                log.info("Updated existing FK connection for attribute: {}", attributeId);
            } else {
                // Create new connection
                Connection connection = new Connection();
                connection.setAttribute(sourceAttribute);
                connection.setTargetModel(targetModel);
                connection.setTargetAttributeId(targetAttributeId);
                connection.setForeignKeyName(foreignKeyName);
                connection.setIsEnforced(true);
                connectionRepository.save(connection);
                log.info("Created new FK connection for attribute: {}", attributeId);
            }

            return true;
        } catch (Exception e) {
            log.error("Error creating FK connection: {}", e.getMessage(), e);
            return false;
        }
    }

    @Transactional
    public boolean removeForeignKeyConnection(String attributeId) {
        try {
            log.info("Removing FK connection for attribute: {}", attributeId);

            // Optional<Connection> connectionOpt =
            // connectionRepository.findByAttributeId(attributeId);
            Optional<Attribute> attributeOpt = attributeRepository.findById(attributeId);
            attributeOpt.get().getConnection().clear();
            return true;
            // System.out.println(connectionOpt.get().getId());
            // if (connectionOpt.isPresent()) {
            // // connectionRepository.delete(connectionOpt.get());
            // log.info("id: {}", connectionOpt.get().getId());
            // log.info("Successfully removed FK connection for attribute: {}",
            // attributeId);
            // return true;
            // } else {
            // log.warn("No FK connection found for attribute: {}", attributeId);
            // return false;
            // }
        } catch (Exception e) {
            log.error("Error removing FK connection for attribute {}: {}", attributeId, e.getMessage(), e);
            return false;
        }
    }

    @Transactional
    public void removeConnectionsForAttribute(String attributeId) {
        try {
            Optional<Connection> connectionOpt = connectionRepository.findByAttributeId(attributeId);
            if (connectionOpt.isPresent()) {
                connectionRepository.delete(connectionOpt.get());
                log.info("Removed connection for attribute: {}", attributeId);
            }
        } catch (Exception e) {
            log.error("Error removing connections for attribute {}: {}", attributeId, e.getMessage(), e);
        }
    }

    @Transactional
    public void removeConnectionsToAttribute(String targetModelId, String targetAttributeId) {
        try {
            List<Connection> connectionsToTarget = connectionRepository.findByTargetModelIdAndTargetAttributeId(
                    targetModelId, targetAttributeId);

            if (!connectionsToTarget.isEmpty()) {
                connectionRepository.deleteAll(connectionsToTarget);
                log.info("Removed {} connections to {}.{}",
                        connectionsToTarget.size(), targetModelId, targetAttributeId);
            }
        } catch (Exception e) {
            log.error("Error removing connections to {}.{}: {}",
                    targetModelId, targetAttributeId, e.getMessage(), e);
        }
    }

    public ConnectionDto convertToConnectionDto(Connection connection) {
        return new ConnectionDto(
                connection.getId(),
                connection.getTargetModel() != null ? connection.getTargetModel().getName() : "Unknown",
                connection.getTargetAttributeId(),
                connection.getForeignKeyName(),
                connection.getIsEnforced());
    }
}