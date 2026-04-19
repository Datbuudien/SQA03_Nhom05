package com.example.react_flow_be.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import com.example.react_flow_be.entity.Diagram;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface DiagramRepository extends JpaRepository<Diagram, Long>, JpaSpecificationExecutor<Diagram> {
    // List<Diagram> findByType(Diagram.DiagramType type);

    Optional<Diagram> findByName(String name);

    // Tìm diagrams không bị xóa
    List<Diagram> findByIsDeletedFalse();

    // Tìm diagrams đã xóa (trong trash)
    List<Diagram> findByIsDeletedTrue();

    // Count diagrams by deleted status
    @Query("SELECT COUNT(d) FROM Diagram d WHERE d.isDeleted = :isDeleted")
    Long countByIsDeleted(@Param("isDeleted") Boolean isDeleted);

    // ⭐ NEW: Tìm diagrams đã bị xóa mềm và quá thời hạn
    List<Diagram> findByIsDeletedTrueAndDeletedAtBefore(LocalDateTime deletedAt);

    // ⭐ NEW: Đếm diagrams sắp hết hạn
    Long countByIsDeletedTrueAndDeletedAtBefore(LocalDateTime deletedAt);

    // ⭐ NEW: Tìm diagrams của owner đang trong trash
    @Query("SELECT d FROM Diagram d " +
            "JOIN d.collaboration c " +
            "WHERE d.isDeleted = true " +
            "AND c.type = 'OWNER' " +
            "AND c.username = :username " +
            "ORDER BY d.deletedAt DESC")
    List<Diagram> findDeletedDiagramsByOwner(@Param("username") String username);
}