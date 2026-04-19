package com.example.react_flow_be.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import com.fasterxml.jackson.annotation.JsonBackReference;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;

@Entity
@Data
@NoArgsConstructor
@AllArgsConstructor
public class History {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    private ActionType actionType;
    private String description;
    private String targetId;
    private TargetType targetType;
    private String oldValue;
    private String newValue;
    private String userId;
    private String sessionId;
    
    @CreationTimestamp
    private LocalDateTime createdAt;
    
    @ManyToOne
    @JoinColumn(name = "creative_diagram_id")
    private CreativeDiagram creativeDiagram;
    
    public enum ActionType {
        SHAPE_CREATED, SHAPE_UPDATED, SHAPE_DELETED, SHAPE_MOVED, SHAPE_RESIZED, SHAPE_STYLED,
        TEXT_CREATED, TEXT_UPDATED, TEXT_DELETED, TEXT_MOVED,
        LINK_CREATED, LINK_UPDATED, LINK_DELETED,
        DIAGRAM_CREATED, DIAGRAM_UPDATED, DIAGRAM_RENAMED,
        USER_JOINED, USER_LEFT, PERMISSION_CHANGED,
        BULK_UPDATE, IMPORT_DATA, EXPORT_DATA
    }
    
    public enum TargetType {
        SHAPE, TEXT, LINK, DIAGRAM, USER
    }
}