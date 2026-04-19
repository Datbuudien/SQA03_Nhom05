package com.example.react_flow_be.dto.websocket;

import java.time.LocalDateTime;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class AddModelMessage extends BaseWebSocketMessage {
    private String name;
    private String modelId;
    private Double positionX;
    private Double positionY;
    private Long databaseDiagramId;

}
