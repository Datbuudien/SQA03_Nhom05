// WebSocketSessionRegistry.java
package com.example.react_flow_be.config;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.WebSocketSession;

import java.io.IOException;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Registry để lưu trữ tất cả WebSocket sessions thực
 * Cho phép đóng kết nối từ server
 */
@Component
@Slf4j
public class WebSocketSessionTracker {

    private final Map<String, WebSocketSession> sessions = new ConcurrentHashMap<>();

    public void add(WebSocketSession session) {
        sessions.put(session.getId(), session);
        log.debug("📝 Added session: {}", session.getId());
    }

    public void remove(WebSocketSession session) {
        sessions.remove(session.getId());
        log.debug("🗑️ Removed session: {}", session.getId());
    }

    public WebSocketSession get(String sessionId) {
        return sessions.get(sessionId);
    }

    public void closeSession(String sessionId) {
        WebSocketSession session = sessions.get(sessionId);

        if (session != null && session.isOpen()) {
            try {
                session.close(CloseStatus.POLICY_VIOLATION);
                sessions.remove(sessionId);
                log.info("🔌 Force closed WebSocket session: {}", sessionId);
            } catch (IOException e) {
                log.error("❌ Error closing session {}: {}", sessionId, e.getMessage());
            }
        } else {
            log.warn("⚠️ Session {} not found or already closed", sessionId);
        }
    }

    /**
     * Đóng nhiều sessions
     */
    public void closeSessions(java.util.Set<String> sessionIds) {
        int closedCount = 0;
        for (String sessionId : sessionIds) {
            closeSession(sessionId);
            closedCount++;
        }
        log.info("🔌 Closed {} session(s)", closedCount);
    }

    public int getActiveSessionCount() {
        return sessions.size();
    }
}