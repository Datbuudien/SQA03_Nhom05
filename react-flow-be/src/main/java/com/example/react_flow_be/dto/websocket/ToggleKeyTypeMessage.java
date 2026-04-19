package com.example.react_flow_be.dto.websocket;

import java.time.LocalDateTime;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
public class ToggleKeyTypeMessage extends BaseWebSocketMessage {
    private String modelName;
    private String modelId;
    private String attributeId;
    private String keyType; // "NORMAL" | "PRIMARY" | "FOREIGN"

}