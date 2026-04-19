package com.example.react_flow_be.dto.websocket;

import java.time.LocalDateTime;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class AddModelResponseMessage extends BaseWebSocketMessage {
    private String modelName;
    private String realModelId;
    private Double positionX;
    private Double positionY;

}