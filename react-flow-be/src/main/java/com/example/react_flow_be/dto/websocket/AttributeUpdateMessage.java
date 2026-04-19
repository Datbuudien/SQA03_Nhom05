package com.example.react_flow_be.dto.websocket;

import java.time.LocalDateTime;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class AttributeUpdateMessage extends BaseWebSocketMessage {
    private String attributeId;
    private String attributeName;
    private String attributeType;
    private String modelId;
    private String nodeId;
    private Boolean isNullable;
    private Boolean isPrimaryKey;
    private Boolean isForeignKey;

}