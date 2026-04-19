package com.example.react_flow_be.dto.websocket;

import java.time.LocalDateTime;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ForeignKeyConnectMessage extends BaseWebSocketMessage {
    private String attributeId;
    private String targetModelId;
    private String targetAttributeId;
    private String foreignKeyName;

}