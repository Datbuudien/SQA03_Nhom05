package com.example.react_flow_be.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import com.fasterxml.jackson.annotation.JsonBackReference;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;

@Entity
@Data
@NoArgsConstructor
@AllArgsConstructor
public class Text {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    private String nodeId;
    private String content;
    
    // Position
    private Double positionX;
    private Double positionY;
    private Double width;
    private Double height;
    
    // Text styling
    private String fontFamily;
    private Integer fontSize;
    private String fontColor;
    private Boolean isBold;
    private Boolean isItalic;
    private Boolean isUnderline;
    private TextAlign textAlign;
    private VerticalAlign verticalAlign;
    
    private String backgroundColor;
    private Integer padding;
    private Integer zIndex;
    private Double rotation;
    
    @CreationTimestamp
    private LocalDateTime createdAt;
    
    @UpdateTimestamp
    private LocalDateTime updatedAt;
    
    @ManyToOne
    @JoinColumn(name = "shape_id")
    private Shape shape;
    
    public enum TextAlign {
        LEFT, CENTER, RIGHT, JUSTIFY
    }
    
    public enum VerticalAlign {
        TOP, MIDDLE, BOTTOM
    }
}