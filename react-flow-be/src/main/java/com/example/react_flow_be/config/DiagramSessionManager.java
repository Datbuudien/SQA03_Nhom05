// DiagramSessionManager.java
package com.example.react_flow_be.config;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

@Service
@Slf4j
public class DiagramSessionManager {

    // Map: diagramId -> Set of sessionIds
    private final Map<Long, Set<String>> diagramSessions = new ConcurrentHashMap<>();

    // Map: sessionId -> diagramId
    private final Map<String, Long> sessionToDiagram = new ConcurrentHashMap<>();

    // ⭐ NEW: Map sessionId -> username
    private final Map<String, String> sessionToUsername = new ConcurrentHashMap<>();

    // ⭐ NEW: Map diagramId -> Map(username -> sessionId)
    private final Map<Long, Map<String, String>> diagramUserSessions = new ConcurrentHashMap<>();

    /**
     * User joins a diagram
     */
    public void joinDiagram(Long diagramId, String sessionId, String username) {
        // Remove from old diagram if exists
        leaveDiagram(sessionId);

        // Add to new diagram
        diagramSessions.computeIfAbsent(diagramId, k -> ConcurrentHashMap.newKeySet())
                .add(sessionId);
        sessionToDiagram.put(sessionId, diagramId);

        // ⭐ Track username
        sessionToUsername.put(sessionId, username);

        // ⭐ Track user session in diagram
        diagramUserSessions.computeIfAbsent(diagramId, k -> new ConcurrentHashMap<>())
                .put(username, sessionId);

        log.info("Session {} (user: {}) joined diagram {}. Total users: {}",
                sessionId, username, diagramId, getActiveUserCount(diagramId));
    }

    /**
     * User leaves current diagram
     */
    public void leaveDiagram(String sessionId) {
        Long diagramId = sessionToDiagram.remove(sessionId);
        String username = sessionToUsername.remove(sessionId); // ⭐ Remove username mapping

        if (diagramId != null) {
            Set<String> sessions = diagramSessions.get(diagramId);
            if (sessions != null) {
                sessions.remove(sessionId);
                if (sessions.isEmpty()) {
                    diagramSessions.remove(diagramId);
                }
                log.info("Session {} (user: {}) left diagram {}. Remaining users: {}",
                        sessionId, username, diagramId, sessions.size());
            }

            // ⭐ Remove from diagram user sessions
            if (username != null) {
                Map<String, String> userSessions = diagramUserSessions.get(diagramId);
                if (userSessions != null) {
                    userSessions.remove(username);
                    if (userSessions.isEmpty()) {
                        diagramUserSessions.remove(diagramId);
                    }
                }
            }
        }
    }

    /**
     * Get all active sessions for a diagram
     */
    public Set<String> getActiveSessions(Long diagramId) {
        return new HashSet<>(diagramSessions.getOrDefault(diagramId, Collections.emptySet()));
    }

    /**
     * Get diagram ID for a session
     */
    public Long getDiagramForSession(String sessionId) {
        return sessionToDiagram.get(sessionId);
    }

    /**
     * ⭐ NEW: Get username for a session
     */
    public String getUsernameForSession(String sessionId) {
        return sessionToUsername.get(sessionId);
    }

    /**
     * ⭐ NEW: Get all active usernames in a diagram
     */
    public Set<String> getActiveUsernames(Long diagramId) {
        Map<String, String> userSessions = diagramUserSessions.get(diagramId);
        return userSessions != null ? new HashSet<>(userSessions.keySet()) : Collections.emptySet();
    }

    /**
     * Get number of active users on a diagram
     */
    public int getActiveUserCount(Long diagramId) {
        Set<String> sessions = diagramSessions.get(diagramId);
        return sessions != null ? sessions.size() : 0;
    }

    /**
     * Get all active diagrams
     */
    public Set<Long> getActiveDiagrams() {
        return new HashSet<>(diagramSessions.keySet());
    }

    /**
     * ⭐ NEW: Check if user has active session in diagram
     */
    public boolean isUserActiveInDiagram(Long diagramId, String username) {
        Map<String, String> userSessions = diagramUserSessions.get(diagramId);
        return userSessions != null && userSessions.containsKey(username);
    }
}