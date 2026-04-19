// WebSocketEventListener.java
package com.example.react_flow_be.config;

import com.example.react_flow_be.config.DiagramSessionManager;
import com.example.react_flow_be.dto.websocket.WebSocketResponse;
import com.example.react_flow_be.service.MigrationService;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.event.EventListener;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.messaging.SessionConnectedEvent;
import org.springframework.web.socket.messaging.SessionDisconnectEvent;
import org.springframework.web.socket.messaging.SessionSubscribeEvent;
import org.springframework.web.socket.messaging.SessionUnsubscribeEvent;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Component
@RequiredArgsConstructor
@Slf4j
public class WebSocketEventListener {

    private final DiagramSessionManager sessionManager;
    private final SimpMessagingTemplate messagingTemplate;
    private final MigrationService migrationService;

    private static final Pattern DIAGRAM_TOPIC_PATTERN = Pattern.compile("/topic/diagram/(\\d+)");

    @EventListener
    public void handleWebSocketConnectListener(SessionConnectedEvent event) {
        StompHeaderAccessor headerAccessor = StompHeaderAccessor.wrap(event.getMessage());
        String sessionId = headerAccessor.getSessionId();
        log.info("WebSocket connected: sessionId={}", sessionId);
    }

    @EventListener
    public void handleWebSocketDisconnectListener(SessionDisconnectEvent event) {
        StompHeaderAccessor headerAccessor = StompHeaderAccessor.wrap(event.getMessage());
        String sessionId = headerAccessor.getSessionId();

        Long diagramId = sessionManager.getDiagramForSession(sessionId);
        String username = sessionManager.getUsernameForSession(sessionId);

        if (diagramId != null && username != null) {
            // 🔥 TẠO SNAPSHOT KHI USER DISCONNECT
            try {
                boolean snapshotCreated = migrationService.createSnapshotOnDisconnect(diagramId, username);
                if (snapshotCreated) {
                    log.info("✅ Created migration snapshot for diagram {} by user {}", diagramId, username);
                } else {
                    log.info("ℹ️ No changes detected, skipped snapshot for diagram {}", diagramId);
                }
            } catch (Exception e) {
                log.error("❌ Error creating snapshot on disconnect: {}", e.getMessage(), e);
            }

            sessionManager.leaveDiagram(sessionId);

            // Notify other users about user list change
            notifyUserListChange(diagramId);
        }

        log.info("WebSocket disconnected: sessionId={}, user={}, was viewing diagram={}",
                sessionId, username, diagramId);
    }

    @EventListener
    public void handleSubscribeEvent(SessionSubscribeEvent event) {
        StompHeaderAccessor headerAccessor = StompHeaderAccessor.wrap(event.getMessage());
        String sessionId = headerAccessor.getSessionId();
        String destination = headerAccessor.getDestination();
        String username = headerAccessor.getFirstNativeHeader("X-Username");

        if (destination != null) {
            Matcher matcher = DIAGRAM_TOPIC_PATTERN.matcher(destination);
            if (matcher.matches()) {
                Long diagramId = Long.parseLong(matcher.group(1));

                // joinDiagram is already called in WebSocketAuthInterceptor
                // Just notify about user list change
                notifyUserListChange(diagramId);

                log.info("Session {} (user: {}) subscribed to diagram {}",
                        sessionId, username, diagramId);
            }
        }
    }

    @EventListener
    public void handleUnsubscribeEvent(SessionUnsubscribeEvent event) {
        StompHeaderAccessor headerAccessor = StompHeaderAccessor.wrap(event.getMessage());
        String sessionId = headerAccessor.getSessionId();

        Long diagramId = sessionManager.getDiagramForSession(sessionId);
        String username = sessionManager.getUsernameForSession(sessionId);

        if (diagramId != null) {
            sessionManager.leaveDiagram(sessionId);
            notifyUserListChange(diagramId);

            log.info("Session {} (user: {}) unsubscribed from diagram {}",
                    sessionId, username, diagramId);
        }
    }

    private void notifyUserListChange(Long diagramId) {
        Set<String> activeUsernames = sessionManager.getActiveUsernames(diagramId);
        int userCount = activeUsernames.size();

        UserListMessage data = new UserListMessage(
                diagramId,
                activeUsernames,
                userCount,
                System.currentTimeMillis());

        WebSocketResponse<UserListMessage> response = WebSocketResponse.success("USER_LIST_UPDATE", data, null);

        messagingTemplate.convertAndSend(
                "/topic/diagram/" + diagramId,
                response);

        log.info("📢 Notified diagram {} about user list: {} users online",
                diagramId, userCount);
    }

    public record UserListMessage(
            Long diagramId,
            Set<String> activeUsernames,
            int activeUsers,
            long timestamp) {
    }
}