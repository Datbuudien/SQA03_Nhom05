package com.example.react_flow_be.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import com.fasterxml.jackson.annotation.JsonBackReference;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;
import java.util.List;

@Entity
@Data
@NoArgsConstructor
@AllArgsConstructor
public class Link {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    private String edgeId;
    private String label;
    private LinkType linkType;
    private ArrowType sourceArrowType;
    private ArrowType targetArrowType;
    
    // Style
    private String strokeColor;
    private Integer strokeWidth;
    private String strokeStyle;
    private Boolean isAnimated;
    
    private String sourceHandle;
    private String targetHandle;
    private String pathData;
    private Integer zIndex;
    
    @CreationTimestamp
    private LocalDateTime createdAt;
    
    @UpdateTimestamp
    private LocalDateTime updatedAt;
    
    @ManyToOne
    @JoinColumn(name = "source_shape_id")
    private Shape sourceShape;
    
    @ManyToOne
    @JoinColumn(name = "target_shape_id")
    private Shape targetShape;
    
    public enum LinkType {
        STRAIGHT, BEZIER, STEP, SMOOTH_STEP, CUSTOM
    }
    
    public enum ArrowType {
        NONE, ARROW, DIAMOND, CIRCLE, SQUARE, TRIANGLE, CROW_FOOT
    }
}
