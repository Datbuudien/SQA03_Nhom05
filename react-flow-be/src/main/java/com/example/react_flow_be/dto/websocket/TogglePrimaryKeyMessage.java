package com.example.react_flow_be.dto.websocket;

import java.time.LocalDateTime;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class TogglePrimaryKeyMessage extends BaseWebSocketMessage {
    private String modelName;
    private String modelId;
    private String attributeId;

}