package com.example.react_flow_be.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * DTO để serialize toàn bộ diagram snapshot thành JSON
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class DiagramSnapshotDto {
    private Long diagramId;
    private String diagramName;
    private String databaseType;
    private String version;
    private String charset;
    private String collation;
    private List<ModelSnapshotDto> models;

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ModelSnapshotDto {
        private String id;
        private String name;
        private Double positionX;
        private Double positionY;
        private List<AttributeSnapshotDto> attributes;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class AttributeSnapshotDto {
        private String id;
        private String name;
        private String dataType;
        private Integer length;
        private Boolean isNullable;
        private Boolean isPrimaryKey;
        private Boolean isForeignKey;
        private Integer attributeOrder;
        private ConnectionSnapshotDto connection;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ConnectionSnapshotDto {
        private String targetModelId;
        private String targetAttributeId;
        private String foreignKeyName;
    }
}