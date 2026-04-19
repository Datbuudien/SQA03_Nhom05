package com.example.react_flow_be.dto.websocket;

import java.time.LocalDateTime;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class UpdateModelNameMessage extends BaseWebSocketMessage {
    private String modelId;
    private String oldModelName;
    private String newModelName;

}