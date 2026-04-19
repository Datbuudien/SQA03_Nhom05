package com.example.react_flow_be.dto.websocket;

import java.time.LocalDateTime;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
public class AddAttributeResponseMessage extends BaseWebSocketMessage {
    private String modelId;
    private String attributeId;
    private String attributeName;
    private String dataType;
    private String realAttributeId;
}