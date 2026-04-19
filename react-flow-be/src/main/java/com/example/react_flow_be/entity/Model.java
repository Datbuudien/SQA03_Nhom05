package com.example.react_flow_be.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import com.fasterxml.jackson.annotation.JsonManagedReference;

import java.time.LocalDateTime;
import java.util.List;

import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

@Entity
@Data
@NoArgsConstructor
@AllArgsConstructor
public class Model {
    @Id
    @Column(length = 36)
    private String id;

    private String nodeId;
    private String name;
    private Double positionX;
    private Double positionY;

    @CreationTimestamp
    private LocalDateTime createdAt;

    @UpdateTimestamp
    private LocalDateTime updatedAt;

    private LocalDateTime nameUpdatedAt;
    private LocalDateTime positionUpdatedAt;

    @ManyToOne
    @JoinColumn(name = "database_diagram_id")
    private DatabaseDiagram databaseDiagram;

    // Attributes sẽ được xóa tự động khi xóa Model
    @OneToMany(mappedBy = "model", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<Attribute> attributes;

    @OneToMany(mappedBy = "targetModel", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<Connection> incomingConnections;
}