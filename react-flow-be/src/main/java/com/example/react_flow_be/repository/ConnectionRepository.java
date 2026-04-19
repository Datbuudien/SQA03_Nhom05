package com.example.react_flow_be.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import com.example.react_flow_be.entity.Connection;

import jakarta.persistence.LockModeType;

@Repository
public interface ConnectionRepository extends JpaRepository<Connection, Long> {
    Optional<Connection> findByAttributeId(String attributeId);

    List<Connection> findByTargetModelId(String targetModelId);

    @Query("SELECT c FROM Connection c JOIN c.targetModel tm WHERE tm.id = :targetModelId AND c.targetAttributeId = :targetAttributeId")
    List<Connection> findByTargetModelIdAndTargetAttributeId(
            @Param("targetModelId") String targetModelId,
            @Param("targetAttributeId") String targetAttributeId);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT c FROM Connection c WHERE c.attribute.id = :attributeId")
    Optional<Connection> findByAttributeIdForUpdate(@Param("attributeId") String attributeId);

}