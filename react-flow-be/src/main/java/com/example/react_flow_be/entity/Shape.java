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
@Data
@NoArgsConstructor
@AllArgsConstructor
public class Shape {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    private String nodeId;
    private String name;
    private ShapeType type;
    
    // Position & Size
    private Double positionX;
    private Double positionY;
    private Double width;
    private Double height;
    
    // Style
    private String backgroundColor;
    private String borderColor;
    private Integer borderWidth;
    private String borderStyle;
    private Integer borderRadius;
    
    private Integer zIndex;
    private Double rotation;
    private String customProperties;
    
    @CreationTimestamp
    private LocalDateTime createdAt;
    
    @UpdateTimestamp
    private LocalDateTime updatedAt;
    
    @ManyToOne
    @JoinColumn(name = "creative_diagram_id")
    private CreativeDiagram creativeDiagram;
    
    @OneToMany(mappedBy = "shape")
    private List<Text> texts;
    
    @OneToMany(mappedBy = "sourceShape")
    private List<Link> links; // Các link đi từ shape này
    
    public enum ShapeType {
        RECTANGLE, CIRCLE, ELLIPSE, TRIANGLE, DIAMOND, POLYGON,
        PROCESS, DECISION, START_END, CONNECTOR,
        TABLE, VIEW, STORED_PROCEDURE,
        CLASS, INTERFACE, PACKAGE,
        CUSTOM, GROUP
    }
}
