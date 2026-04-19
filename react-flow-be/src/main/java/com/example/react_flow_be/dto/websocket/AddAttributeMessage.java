package com.example.react_flow_be.dto.websocket;

import java.time.LocalDateTime;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class AddAttributeMessage extends BaseWebSocketMessage {
    private String modelId;
    private String newAttributeId;
    private String attributeName;
    private String dataType;
}