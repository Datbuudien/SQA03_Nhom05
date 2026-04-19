package com.example.react_flow_be.dto.websocket;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ConnectionUpdateMessage {
    private Long connectionId;
    private String attributeId;
    private String sourceModelName;
    private String targetModelName;
    private String connectionType;
    private String sessionId;
}