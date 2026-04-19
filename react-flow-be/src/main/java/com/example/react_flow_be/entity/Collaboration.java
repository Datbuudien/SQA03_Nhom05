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
public class Collaboration {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Enumerated(EnumType.STRING)
    private CollaborationType type;

    @Enumerated(EnumType.STRING)
    private Permission permission;
    private Boolean isActive;
    private LocalDateTime expiresAt;

    @CreationTimestamp
    private LocalDateTime createdAt;

    @UpdateTimestamp
    private LocalDateTime updatedAt;

    private String username;

    @ManyToOne
    @JoinColumn(name = "diagram_id")
    private Diagram diagram;

    public enum CollaborationType {
        OWNER, PARTICIPANTS
    }

    public enum Permission {
        VIEW, COMMENT, EDIT, FULL_ACCESS
    }
}
