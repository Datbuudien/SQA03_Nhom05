// UpdateDiagramNameMessage.java
package com.example.react_flow_be.dto.websocket;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class UpdateDiagramNameMessage extends BaseWebSocketMessage {
    private String newName;
}