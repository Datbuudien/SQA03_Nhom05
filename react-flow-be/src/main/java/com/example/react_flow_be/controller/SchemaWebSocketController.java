// src/main/java/com/example/react_flow_be/controller/SchemaWebSocketController.java
package com.example.react_flow_be.controller;

import com.example.react_flow_be.config.DiagramSessionManager;
import com.example.react_flow_be.dto.collaboration.CollaborationDTO;
import com.example.react_flow_be.dto.websocket.*;
import com.example.react_flow_be.entity.Collaboration;
import com.example.react_flow_be.service.CollaborationService;
import com.example.react_flow_be.service.DatabaseDiagramService;
import com.example.react_flow_be.service.SchemaVisualizerService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import org.springframework.messaging.handler.annotation.DestinationVariable;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.simp.SimpMessageHeaderAccessor;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Controller;

import java.util.function.Supplier;

@Controller
@RequiredArgsConstructor
@Slf4j
public class SchemaWebSocketController {

    private final SchemaVisualizerService schemaVisualizerService;
    private final SimpMessagingTemplate messagingTemplate;
    private final DatabaseDiagramService databaseDiagramService;
    private final DiagramSessionManager sessionManager;
    private final CollaborationService collaborationService;

    @MessageMapping("/diagram/{diagramId}/updateNodePosition")
    public void updateNodePosition(@DestinationVariable Long diagramId, ModelUpdateMessage message,
            SimpMessageHeaderAccessor headerAccessor) {
        message.setDiagramId(diagramId);
        handleWebSocketMessage(
                "NODE_POSITION_UPDATE",
                diagramId,
                message,
                headerAccessor,
                () -> schemaVisualizerService.updateModelPosition(
                        message.getModelId(),
                        message.getPositionX(),
                        message.getPositionY(),
                        message.getClientTimestamp()));
    }

    @MessageMapping("/diagram/{diagramId}/updateAttributeName")
    public void updateAttributeName(@DestinationVariable Long diagramId, AttributeUpdateMessage message,
            SimpMessageHeaderAccessor headerAccessor) {
        message.setDiagramId(diagramId);
        handleWebSocketMessage(
                "FIELD_NAME_UPDATE",
                diagramId,
                message,
                headerAccessor,
                () -> schemaVisualizerService.updateAttributeName(
                        message.getAttributeId(),
                        message.getAttributeName(),
                        message.getClientTimestamp()));
    }

    @MessageMapping("/diagram/{diagramId}/updateAttributeType")
    public void updateAttributeType(@DestinationVariable Long diagramId, AttributeUpdateMessage message,
            SimpMessageHeaderAccessor headerAccessor) {
        message.setDiagramId(diagramId);
        handleWebSocketMessage(
                "FIELD_TYPE_UPDATE",
                diagramId,
                message,
                headerAccessor,
                () -> schemaVisualizerService.updateAttributeType(
                        message.getAttributeId(),
                        message.getAttributeType(),
                        message.getClientTimestamp()));
    }

    @MessageMapping("/diagram/{diagramId}/toggleKeyType")
    public void toggleKeyType(@DestinationVariable Long diagramId, ToggleKeyTypeMessage message,
            SimpMessageHeaderAccessor headerAccessor) {
        message.setDiagramId(diagramId);
        handleWebSocketMessage(
                "TOGGLE_KEY_TYPE",
                diagramId,
                message,
                headerAccessor,
                () -> {
                    boolean success = false;

                    switch (message.getKeyType()) {
                        case "PRIMARY":
                            success = schemaVisualizerService.setAttributeAsPrimaryKey(message.getAttributeId(),
                                    message.getClientTimestamp());
                            break;
                        case "FOREIGN":
                            success = schemaVisualizerService.setAttributeAsForeignKey(message.getAttributeId(),
                                    message.getClientTimestamp());
                            break;
                        case "NORMAL":
                            success = schemaVisualizerService.setAttributeAsNormal(message.getAttributeId(),
                                    message.getClientTimestamp());
                            break;
                        default:
                            log.warn("Unknown key type: {}", message.getKeyType());
                            return false;
                    }

                    if (success) {
                        // Return the message với keyType đã set
                        return message;
                    }
                    return null;
                });
    }

    @MessageMapping("/diagram/{diagramId}/addAttribute")
    public void addAttribute(@DestinationVariable Long diagramId, AddAttributeMessage message,
            SimpMessageHeaderAccessor headerAccessor) {
        message.setDiagramId(diagramId);
        handleWebSocketMessage(
                "ADD_ATTRIBUTE",
                diagramId,
                message,
                headerAccessor,
                () -> {
                    String attributeId = schemaVisualizerService.addAttribute(
                            message.getModelId(),
                            message.getNewAttributeId(),
                            message.getAttributeName(),
                            message.getDataType());

                    if (attributeId != null) {
                        // Tạo response với đầy đủ thông tin attribute
                        AddAttributeResponseMessage response = new AddAttributeResponseMessage();

                        response.setModelId(message.getModelId());
                        response.setAttributeId(message.getNewAttributeId());
                        response.setAttributeName(message.getAttributeName());
                        response.setDataType(message.getDataType());
                        response.setRealAttributeId(attributeId); // thay cho constructor
                        response.setSessionId(message.getSessionId());
                        response.setMessageId(message.getMessageId());
                        response.setClientTimestamp(message.getClientTimestamp());

                        return response;
                    }
                    return null;
                });
    }

    @MessageMapping("/diagram/{diagramId}/deleteAttribute")
    public void deleteAttribute(@DestinationVariable Long diagramId, DeleteAttributeMessage message,
            SimpMessageHeaderAccessor headerAccessor) {
        message.setDiagramId(diagramId);
        handleWebSocketMessage(
                "DELETE_ATTRIBUTE",
                diagramId,
                message,
                headerAccessor,
                () -> {
                    schemaVisualizerService.deleteAttribute(message.getAttributeId());
                    DeleteAttributeMessage msg = new DeleteAttributeMessage();
                    msg.setModelId(message.getModelId());
                    msg.setAttributeId(message.getAttributeId());
                    msg.setSessionId(message.getSessionId());
                    msg.setMessageId(message.getMessageId());
                    msg.setClientTimestamp(message.getClientTimestamp());

                    return msg;
                });
    }

    @MessageMapping("/diagram/{diagramId}/connectForeignKey")
    public void connectForeignKey(@DestinationVariable Long diagramId, ForeignKeyConnectMessage message,
            SimpMessageHeaderAccessor headerAccessor) {
        log.info("Received FK connect request: attributeId={}, target={}.{}",
                message.getAttributeId(), message.getTargetModelId(), message.getTargetAttributeId());
        message.setDiagramId(diagramId);
        handleWebSocketMessage(
                "FOREIGN_KEY_CONNECT",
                diagramId,
                message,
                headerAccessor,
                () -> schemaVisualizerService.createForeignKeyConnection(
                        message.getAttributeId(),
                        message.getTargetModelId(),
                        message.getTargetAttributeId(),
                        message.getForeignKeyName()));
    }

    @MessageMapping("/diagram/{diagramId}/disconnectForeignKey")
    public void disconnectForeignKey(@DestinationVariable Long diagramId, ForeignKeyDisconnectMessage message,
            SimpMessageHeaderAccessor headerAccessor) {
        log.info("Received FK disconnect request: attributeId={}", message.getAttributeId());
        message.setDiagramId(diagramId);
        handleWebSocketMessage(
                "FOREIGN_KEY_DISCONNECT",
                diagramId,
                message,
                headerAccessor,
                () -> schemaVisualizerService.removeForeignKeyConnection(message.getAttributeId()));
    }

    @MessageMapping("/diagram/{diagramId}/addModel")
    public void addModel(@DestinationVariable Long diagramId, AddModelMessage message,
            SimpMessageHeaderAccessor headerAccessor) {
        message.setDiagramId(diagramId);
        handleWebSocketMessage(
                "ADD_MODEL",
                diagramId,
                message,
                headerAccessor,
                () -> {
                    String modelId = schemaVisualizerService.addModel(
                            message.getName(),
                            message.getDatabaseDiagramId(),
                            message.getModelId(),
                            message.getPositionX(),
                            message.getPositionY());

                    if (modelId != null) {
                        // Tạo response với đầy đủ thông tin model
                        AddModelResponseMessage response = new AddModelResponseMessage();
                        response.setModelName("Model");
                        response.setRealModelId(modelId);
                        response.setPositionX(message.getPositionX());
                        response.setPositionY(message.getPositionY());
                        response.setSessionId(message.getSessionId());
                        response.setMessageId(message.getMessageId());
                        response.setClientTimestamp(message.getClientTimestamp());

                        return response;
                    }
                    return null;
                });
    }

    @MessageMapping("/diagram/{diagramId}/updateModelName")
    public void updateModelName(@DestinationVariable Long diagramId, UpdateModelNameMessage message,
            SimpMessageHeaderAccessor headerAccessor) {
        message.setDiagramId(diagramId);
        handleWebSocketMessage(
                "UPDATE_MODEL_NAME",
                diagramId,
                message,
                headerAccessor,
                () -> schemaVisualizerService.updateModelName(
                        message.getModelId(),
                        message.getNewModelName(),
                        message.getClientTimestamp()));
    }

    @MessageMapping("/diagram/{diagramId}/deleteModel")
    public void deleteModel(@DestinationVariable Long diagramId, DeleteModelMessage message,
            SimpMessageHeaderAccessor headerAccessor) {
        message.setDiagramId(diagramId);
        handleWebSocketMessage(
                "DELETE_MODEL",
                diagramId,
                message,
                headerAccessor,
                () -> {
                    boolean success = schemaVisualizerService.deleteModel(message.getModelId());
                    if (success) {
                        DeleteModelMessage msg = new DeleteModelMessage();
                        msg.setModelId(message.getModelId());
                        msg.setSessionId(message.getSessionId());
                        msg.setMessageId(message.getMessageId());
                        msg.setClientTimestamp(message.getClientTimestamp());

                        // Trả về message với cả modelId và modelName để frontend xử lý
                        return msg;
                    }
                    return null;
                });
    }

    @MessageMapping("/diagram/{diagramId}/updateDiagramName")
    public void updateDiagramName(@DestinationVariable Long diagramId, UpdateDiagramNameMessage message,
            SimpMessageHeaderAccessor headerAccessor) {
        message.setDiagramId(diagramId);
        handleWebSocketMessage(
                "UPDATE_DIAGRAM_NAME",
                diagramId,
                message,
                headerAccessor,
                () -> databaseDiagramService.updateDiagramName(diagramId, message.getNewName()));
    }

    /**
     * Generic method to handle WebSocket messages with diagram-specific
     * broadcasting
     */
    private <T, R> void handleWebSocketMessage(
            String messageType,
            Long diagramId,
            T message,
            SimpMessageHeaderAccessor headerAccessor,
            Supplier<R> serviceCall) {
        String sessionId = headerAccessor.getSessionId();
        String username = sessionManager.getUsernameForSession(sessionId);
        CollaborationDTO cDto = collaborationService.getUserCollaboration(diagramId, username);
        if (cDto.getPermission().equals(Collaboration.Permission.VIEW)) {
            sendErrorToUser(sessionId,
                    "You dont have permission to edit this diagram " + messageType.toLowerCase().replace("_", " "));
            return;
        }

        try {
            if (message instanceof BaseWebSocketMessage) {
                ((BaseWebSocketMessage) message).setSessionId(sessionId);
            }

            log.info("Processing {} for diagram {}: {}", messageType, diagramId, message);

            R result = serviceCall.get();
            boolean success = isSuccessfulResult(result);

            if (success) {
                Object responseData = (result != null && !(result instanceof Boolean)) ? result : message;
                WebSocketResponse<Object> response = WebSocketResponse.success(messageType, responseData, sessionId);

                // Broadcast ONLY to subscribers of this specific diagram
                String topicPath = "/topic/diagram/" + diagramId;
                messagingTemplate.convertAndSend(topicPath, response);

                log.info("Successfully broadcast {} to {}", messageType, topicPath);
            } else {
                sendErrorToUser(sessionId, "Failed to process " + messageType.toLowerCase().replace("_", " "));
            }

        } catch (Exception e) {
            log.error("Error processing {} for diagram {}: {}", messageType, diagramId, e.getMessage(), e);
            sendErrorToUser(sessionId, "Internal server error: " + e.getMessage());
        }
    }

    /**
     * Extract messageId from message if available (for client-side filtering)
     */
    private String extractMessageId(Object message) {
        if (message instanceof BaseWebSocketMessage) {
            return ((BaseWebSocketMessage) message).getMessageId();
        }
        return null;
    }

    /**
     * Check if the service call result indicates success
     */
    private boolean isSuccessfulResult(Object result) {
        if (result == null)
            return false;
        if (result instanceof Boolean)
            return (Boolean) result;
        return true;
    }

    /**
     * Send error message to specific user
     */
    private void sendErrorToUser(String sessionId, String errorMessage) {
        log.info("error: sessionId - " + sessionId + " " + errorMessage);
        WebSocketResponse<String> errorResponse = WebSocketResponse.error(errorMessage, sessionId);
        messagingTemplate.convertAndSend("/queue/errors-" + sessionId, errorResponse);
    }
}