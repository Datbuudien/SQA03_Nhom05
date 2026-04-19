package com.example.react_flow_be.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import com.fasterxml.jackson.annotation.JsonBackReference;
import com.fasterxml.jackson.annotation.JsonManagedReference;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;
import java.util.List;

@Entity
@Inheritance(strategy = InheritanceType.JOINED)
@Data
@NoArgsConstructor
@AllArgsConstructor
public class Diagram {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String name;
    private String description;

    // @Enumerated(EnumType.STRING)
    // private DiagramType type; // Allow null, no default

    private Boolean isPublic;
    private Boolean isTemplate;
    private Boolean isDeleted = false;

    private LocalDateTime deletedAt;

    // Canvas settings
    private String canvasData;
    private Double zoomLevel;
    private Double panX;
    private Double panY;

    @CreationTimestamp
    private LocalDateTime createdAt;

    @UpdateTimestamp
    private LocalDateTime updatedAt;

    @OneToMany(mappedBy = "diagram", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<Collaboration> collaboration;

    // public enum DiagramType {
    // ER_DIAGRAM, FLOWCHART, UML_CLASS, NETWORK, ORGANIZATIONAL, MIND_MAP,
    // WIREFRAME, CUSTOM
    // }
}