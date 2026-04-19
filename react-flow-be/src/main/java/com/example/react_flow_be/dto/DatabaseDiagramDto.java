package com.example.react_flow_be.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.util.List;

import com.example.react_flow_be.entity.Collaboration;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class DatabaseDiagramDto {
    private Long id;
    private Collaboration.Permission permission;
    private String name;
    private String description;
    private String databaseType;
    private String version;
    private String charset;
    private String collation;
    private Boolean isPublic;
    private Boolean isTemplate;
    private Double zoomLevel;
    private Double panX;
    private Double panY;
    private List<ModelDto> models;
}
