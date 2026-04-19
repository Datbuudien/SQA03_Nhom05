package com.example.react_flow_be.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;

import com.fasterxml.jackson.annotation.JsonBackReference;

@Entity
@Data
@NoArgsConstructor
@AllArgsConstructor
public class Attribute {
    @Id
    @Column(length = 36)
    private String id;

    private String name;
    private String dataType;
    private Integer length;
    private Integer precisionValue;
    private Integer scaleValue;

    private Boolean isNullable;
    private Boolean isPrimaryKey;
    private Boolean isForeignKey;
    private Boolean isUnique;
    private Boolean isAutoIncrement;

    private String defaultValue;
    private String comment;
    private Integer attributeOrder;

    private Boolean hasIndex;
    private String indexName;
    private IndexType indexType;

    private LocalDateTime nameUpdatedAt;
    private LocalDateTime typeUpdatedAt;
    private LocalDateTime keyTypeUpdatedAt;

    @ManyToOne
    @JoinColumn(name = "model_id")
    private Model model;

    @OneToMany(mappedBy = "attribute", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<Connection> connection;

    public enum IndexType {
        PRIMARY, UNIQUE, INDEX, FULLTEXT, SPATIAL
    }
}
