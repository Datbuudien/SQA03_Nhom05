package com.example.react_flow_be.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;

@Entity
@Data
@NoArgsConstructor
@AllArgsConstructor
@Table(name = "migration")
public class Migration {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String username; // Người thực hiện thay đổi

    @Column(columnDefinition = "TEXT")
    private String snapshotJson; // JSON chứa toàn bộ diagram data tại thời điểm đó

    @Column(length = 64)
    private String snapshotHash; // Hash để so sánh thay đổi

    @CreationTimestamp
    private LocalDateTime createdAt;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "database_diagram_id")
    private DatabaseDiagram databaseDiagram;
}