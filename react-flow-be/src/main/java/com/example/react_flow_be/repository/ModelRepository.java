package com.example.react_flow_be.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import com.example.react_flow_be.entity.Model;

import jakarta.persistence.LockModeType;

import java.util.List;
import java.util.Optional;

@Repository
public interface ModelRepository extends JpaRepository<Model, String> {
    Optional<Model> findByName(String name);

    Optional<Model> findById(String nodeId);

    List<Model> findByDatabaseDiagramId(Long databaseDiagramId);

    Optional<Model> findByNodeIdAndDatabaseDiagram_Id(String nodeId, Long databaseDiagramId);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT m FROM Model m WHERE m.id = :id")
    Optional<Model> findByIdForUpdate(@Param("id") String id);

}