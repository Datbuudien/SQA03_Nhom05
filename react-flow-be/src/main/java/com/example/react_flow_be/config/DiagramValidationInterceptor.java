// DiagramValidationInterceptor.java
package com.example.react_flow_be.config;

import com.example.react_flow_be.service.DatabaseDiagramService;
import com.example.react_flow_be.service.CollaborationService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Lazy;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.messaging.simp.stomp.StompCommand;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.messaging.support.ChannelInterceptor;
import org.springframework.messaging.support.MessageHeaderAccessor;
import org.springframework.stereotype.Component;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Component
@Slf4j
public class DiagramValidationInterceptor implements ChannelInterceptor {

    private final DatabaseDiagramService databaseDiagramService;
    private final CollaborationService collaborationService;
    private final DiagramSessionManager sessionManager;
    private SimpMessagingTemplate messagingTemplate;

    private static final Pattern DIAGRAM_TOPIC_PATTERN = Pattern.compile("/topic/diagram/(\\d+)");
    private static final Pattern DIAGRAM_DESTINATION_PATTERN = Pattern.compile("/app/diagram/(\\d+)/.*");

    public DiagramValidationInterceptor(
            DatabaseDiagramService databaseDiagramService,
            CollaborationService collaborationService,
            DiagramSessionManager sessionManager) {
        this.databaseDiagramService = databaseDiagramService;
        this.collaborationService = collaborationService;
        this.sessionManager = sessionManager;
    }

    @Autowired
    @Lazy
    public void setMessagingTemplate(SimpMessagingTemplate messagingTemplate) {
        this.messagingTemplate = messagingTemplate;
    }

    @Override
    public Message<?> preSend(Message<?> message, MessageChannel channel) {
        StompHeaderAccessor accessor = MessageHeaderAccessor.getAccessor(message, StompHeaderAccessor.class);

        if (accessor == null) {
            return message;
        }

        StompCommand command = accessor.getCommand();

        if (command == StompCommand.SUBSCRIBE) {
            return handleSubscribe(accessor, message);
        }

        if (command == StompCommand.SEND) {
            return handleSend(accessor, message);
        }

        return message;
    }

    /**
     * Handle SUBSCRIBE command
     */
    private Message<?> handleSubscribe(StompHeaderAccessor accessor, Message<?> message) {
        String destination = accessor.getDestination();
        String sessionId = accessor.getSessionId();

        if (destination == null) {
            return message;
        }

        Matcher topicMatcher = DIAGRAM_TOPIC_PATTERN.matcher(destination);
        if (topicMatcher.matches()) {
            Long diagramId = parseDiagramId(topicMatcher.group(1));
            if (diagramId == null) {
                return message;
            }

            // ⭐ Get username from session attributes (set during handshake)
            String username = (String) accessor.getSessionAttributes().get("username");

            if (username == null) {
                log.error("❌ No username in session attributes for session {}", sessionId);
                sendErrorMessage(diagramId, sessionId, "AUTHENTICATION_REQUIRED",
                        "Authentication required: No username in session");
                return null;
            }

            // Check if diagram exists
            if (!validateDiagram(diagramId, accessor)) {
                log.warn("❌ Diagram {} does not exist", diagramId);
                sendErrorMessage(diagramId, sessionId, "DIAGRAM_NOT_FOUND",
                        String.format("Diagram %d does not exist", diagramId));
                return null;
            }

            // Check if user has access to diagram
            if (!collaborationService.hasAccess(diagramId, username)) {
                log.error("❌ Access denied: user {} cannot access diagram {}", username, diagramId);
                sendErrorMessage(diagramId, sessionId, "ACCESS_DENIED",
                        "Access denied: You don't have permission to view this diagram");
                return null;
            }

            // All checks passed - track session with username
            sessionManager.joinDiagram(diagramId, sessionId, username);
            log.info("✅ Access granted: user {} joined diagram {} (session: {})",
                    username, diagramId, sessionId);
        }

        return message;
    }

    /**
     * Handle SEND command
     */
    private Message<?> handleSend(StompHeaderAccessor accessor, Message<?> message) {
        String destination = accessor.getDestination();
        String sessionId = accessor.getSessionId();

        if (destination == null) {
            return message;
        }

        Matcher destMatcher = DIAGRAM_DESTINATION_PATTERN.matcher(destination);
        if (destMatcher.matches()) {
            Long diagramId = parseDiagramId(destMatcher.group(1));
            if (diagramId == null) {
                return message;
            }

            // ⭐ Get username from session attributes
            String username = (String) accessor.getSessionAttributes().get("username");

            if (username == null) {
                log.error("❌ No username in session for SEND to {}", destination);
                return null;
            }

            // Verify user still has access
            if (!collaborationService.hasAccess(diagramId, username)) {
                log.error("❌ Access denied for SEND: user {} to diagram {}", username, diagramId);
                sendErrorMessage(diagramId, sessionId, "ACCESS_DENIED",
                        "Access denied: Your permission has been revoked");
                return null;
            }

            log.debug("✅ User {} authorized to send message to diagram {}", username, diagramId);
        }

        return message;
    }

    private Long parseDiagramId(String idString) {
        try {
            return Long.parseLong(idString);
        } catch (NumberFormatException e) {
            log.error("❌ Invalid diagram ID: {}", idString);
            return null;
        }
    }

    private boolean validateDiagram(Long diagramId, StompHeaderAccessor accessor) {
        try {
            databaseDiagramService.getDatabaseDiagramById(diagramId);
            log.debug("✅ Diagram {} validated for session {}", diagramId, accessor.getSessionId());
            return true;
        } catch (Exception e) {
            log.error("❌ Diagram {} does not exist: {}", diagramId, e.getMessage());
            return false;
        }
    }

    private void sendErrorMessage(Long diagramId, String sessionId, String errorCode, String errorMessage) {
        if (messagingTemplate == null) {
            log.warn("⚠️ MessagingTemplate not available, cannot send error");
            return;
        }

        try {
            ValidationError error = new ValidationError(
                    errorCode,
                    errorMessage,
                    diagramId,
                    sessionId,
                    System.currentTimeMillis());

            messagingTemplate.convertAndSend("/topic/validation-errors", error);
            log.info("📤 Broadcast validation error for diagram {} to /topic/validation-errors", diagramId);

        } catch (Exception e) {
            log.error("Failed to send validation error: {}", e.getMessage());
        }
    }

    public record ValidationError(
            String errorCode,
            String message,
            Long diagramId,
            String sessionId,
            Long timestamp) {
    }
}