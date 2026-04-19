// CollaborationRepository.java
package com.example.react_flow_be.repository;

import com.example.react_flow_be.entity.Collaboration;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface CollaborationRepository extends JpaRepository<Collaboration, Long> {

    // Tìm tất cả collaborations của một diagram
    List<Collaboration> findByDiagramId(Long diagramId);

    // Tìm collaboration theo diagram và username
    Optional<Collaboration> findByDiagramIdAndUsername(Long diagramId, String username);

    // Tìm collaboration của user với diagram cụ thể
    @Query("SELECT c FROM Collaboration c WHERE c.diagram.id = :diagramId AND c.username = :username AND c.isActive = true")
    Optional<Collaboration> findActiveCollaboration(@Param("diagramId") Long diagramId,
            @Param("username") String username);

    // Kiểm tra user có quyền truy cập diagram không
    @Query("SELECT CASE WHEN COUNT(c) > 0 THEN true ELSE false END FROM Collaboration c WHERE c.diagram.id = :diagramId AND c.username = :username AND c.isActive = true")
    boolean hasAccess(@Param("diagramId") Long diagramId, @Param("username") String username);

    // Lấy owner của diagram
    Optional<Collaboration> findByDiagramIdAndType(Long diagramId, Collaboration.CollaborationType type);

    // Đếm số participants của diagram
    @Query("SELECT COUNT(c) FROM Collaboration c WHERE c.diagram.id = :diagramId AND c.type = 'PARTICIPANTS' AND c.isActive = true")
    Integer countParticipants(@Param("diagramId") Long diagramId);

    // Xóa tất cả collaborations của diagram (khi xóa diagram)
    void deleteByDiagramId(Long diagramId);
}