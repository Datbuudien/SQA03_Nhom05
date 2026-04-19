package com.example.react_flow_be.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import com.example.react_flow_be.entity.Attribute;
import com.example.react_flow_be.entity.Model;

import jakarta.persistence.LockModeType;

@Repository
public interface AttributeRepository extends JpaRepository<Attribute, String> {
    List<Attribute> findByModelIdOrderByAttributeOrder(String modelId);

    List<Attribute> findByIsForeignKeyTrue();

    Optional<Attribute> findByModelIdAndName(String id, String attributeName);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT a FROM Attribute a WHERE a.id = :id")
    Optional<Attribute> findByIdForUpdate(@Param("id") String id);
}