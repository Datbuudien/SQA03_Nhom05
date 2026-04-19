package com.example.react_flow_be.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import com.example.react_flow_be.entity.DatabaseDiagram;

import java.util.List;
import java.util.Optional;

@Repository
public interface DatabaseDiagramRepository extends JpaRepository<DatabaseDiagram, Long> {

    Optional<DatabaseDiagram> findByName(String name);

    Optional<DatabaseDiagram> findById(Long id);

    @Query("SELECT d FROM DatabaseDiagram d WHERE d.isTemplate = true")
    List<DatabaseDiagram> findAllTemplates();

    @Modifying
    @Query("UPDATE DatabaseDiagram d SET d.name = :newName WHERE d.id = :id")
    int updateNameById(@Param("id") Long id, @Param("newName") String newName);
}