package com.example.react_flow_be.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ModelDto {
    private String id;
    private String nodeId;
    private String name;
    
    // Position & Size
    private Double positionX;
    private Double positionY;   
    
    // Attributes with their connections
    private List<AttributeDto> attributes;
}
