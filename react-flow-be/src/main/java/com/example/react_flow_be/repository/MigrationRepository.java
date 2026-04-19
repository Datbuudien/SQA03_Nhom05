package com.example.react_flow_be.repository;

import com.example.react_flow_be.entity.Migration;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface MigrationRepository extends JpaRepository<Migration, Long> {

    /**
     * Lấy danh sách migration history theo diagram ID, sắp xếp mới nhất trước
     */
    @Query("SELECT m FROM Migration m WHERE m.databaseDiagram.id = :diagramId ORDER BY m.createdAt DESC")
    List<Migration> findByDatabaseDiagramIdOrderByCreatedAtDesc(@Param("diagramId") Long diagramId);

    /**
     * Lấy migration mới nhất của một diagram
     */
    @Query("SELECT m FROM Migration m WHERE m.databaseDiagram.id = :diagramId ORDER BY m.createdAt DESC")
    List<Migration> findTopByDatabaseDiagramIdOrderByCreatedAtDesc(@Param("diagramId") Long diagramId);
}