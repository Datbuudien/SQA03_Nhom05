// src/main/java/com/example/react_flow_be/dto/websocket/WebSocketResponse.java
package com.example.react_flow_be.dto.websocket;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class WebSocketResponse<T> {
    private String type;
    private T data;
    private String sessionId;
    private long timestamp;
    private String messageId; // For client-side filtering
    
    public static <T> WebSocketResponse<T> success(String type, T data, String sessionId) {
        return new WebSocketResponse<>(type, data, sessionId, System.currentTimeMillis(), null);
    }
    
    public static <T> WebSocketResponse<T> successWithTracking(
            String type, T data, String sessionId, String messageId) {
        return new WebSocketResponse<>(type, data, sessionId, System.currentTimeMillis(), messageId);
    }
    
    public static <T> WebSocketResponse<T> error(String message, String sessionId) {
        return new WebSocketResponse<>("ERROR", (T) message, sessionId, System.currentTimeMillis(), null);
    }
}