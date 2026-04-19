package com.example.react_flow_be.dto.websocket;

import lombok.Data;

import java.time.LocalDateTime;

import lombok.AllArgsConstructor;
import lombok.NoArgsConstructor;

@Data
public abstract class BaseWebSocketMessage {
    private String sessionId;
    private String messageId;
    private LocalDateTime clientTimestamp;
    private Long diagramId; // Thêm field để xác định diagram
}