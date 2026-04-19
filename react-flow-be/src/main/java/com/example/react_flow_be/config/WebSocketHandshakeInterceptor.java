// WebSocketHandshakeInterceptor.java
package com.example.react_flow_be.config;

import lombok.extern.slf4j.Slf4j;
import org.springframework.http.server.ServerHttpRequest;
import org.springframework.http.server.ServerHttpResponse;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.WebSocketHandler;
import org.springframework.web.socket.server.HandshakeInterceptor;

import java.util.Map;

@Component
@Slf4j
public class WebSocketHandshakeInterceptor implements HandshakeInterceptor {

    @Override
    public boolean beforeHandshake(
            ServerHttpRequest request,
            ServerHttpResponse response,
            WebSocketHandler wsHandler,
            Map<String, Object> attributes) throws Exception {

        // ⭐ Lấy username từ header của HTTP handshake request
        String username = request.getHeaders().getFirst("X-Username");

        if (username == null || username.isEmpty()) {
            log.error("❌ No X-Username header in handshake request");
            return false; // Reject handshake
        }

        // ⭐ Lưu username vào WebSocket session attributes
        attributes.put("username", username);
        log.info("✅ Handshake successful for user: {}", username);

        return true; // Allow handshake
    }

    @Override
    public void afterHandshake(
            ServerHttpRequest request,
            ServerHttpResponse response,
            WebSocketHandler wsHandler,
            Exception exception) {
        // Nothing to do after handshake
    }
}