package com.example.react_flow_be.service;

import com.example.react_flow_be.dto.DiagramSnapshotDto;
import com.example.react_flow_be.dto.MigrationDetailDto;
import com.example.react_flow_be.dto.MigrationHistoryDto;
import com.example.react_flow_be.entity.Attribute;
import com.example.react_flow_be.entity.Connection;
import com.example.react_flow_be.entity.DatabaseDiagram;
import com.example.react_flow_be.entity.Migration;
import com.example.react_flow_be.entity.Model;
import com.example.react_flow_be.repository.DatabaseDiagramRepository;
import com.example.react_flow_be.repository.MigrationRepository;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class MigrationService {

    private final MigrationRepository migrationRepository;
    private final DatabaseDiagramRepository databaseDiagramRepository;
    private final ObjectMapper objectMapper;

    /**
     * Tạo snapshot khi user disconnect websocket
     */
    @Transactional
    public boolean createSnapshotOnDisconnect(Long diagramId, String username) {
        try {
            DatabaseDiagram diagram = databaseDiagramRepository.findById(diagramId)
                    .orElseThrow(() -> new RuntimeException("Diagram not found: " + diagramId));

            // Tạo snapshot JSON
            DiagramSnapshotDto snapshot = createSnapshot(diagram);
            String snapshotJson = objectMapper.writeValueAsString(snapshot);

            // Tạo hash từ JSON
            String currentHash = generateHash(snapshotJson);

            // So sánh với hash cuối cùng
            String lastHash = diagram.getLastSnapshotHash();

            if (lastHash == null || !lastHash.equals(currentHash)) {
                // Có thay đổi -> lưu migration
                Migration migration = new Migration();
                migration.setUsername(username);
                migration.setSnapshotJson(snapshotJson);
                migration.setSnapshotHash(currentHash);
                migration.setDatabaseDiagram(diagram);

                migrationRepository.save(migration);

                // Cập nhật lastSnapshotHash
                diagram.setLastSnapshotHash(currentHash);
                databaseDiagramRepository.save(diagram);

                log.info("Created migration snapshot for diagram {} by user {}", diagramId, username);
                return true;
            } else {
                log.info("No changes detected for diagram {}, skipping migration", diagramId);
                return false;
            }

        } catch (Exception e) {
            log.error("Error creating migration snapshot: {}", e.getMessage(), e);
            return false;
        }
    }

    /**
     * Tạo snapshot từ DatabaseDiagram
     */
    private DiagramSnapshotDto createSnapshot(DatabaseDiagram diagram) {
        DiagramSnapshotDto snapshot = new DiagramSnapshotDto();
        snapshot.setDiagramId(diagram.getId());
        snapshot.setDiagramName(diagram.getName());
        snapshot.setDatabaseType(diagram.getDatabaseType().name());
        snapshot.setVersion(diagram.getVersion());
        snapshot.setCharset(diagram.getCharset());
        snapshot.setCollation(diagram.getCollation());

        // Convert models
        List<DiagramSnapshotDto.ModelSnapshotDto> models = diagram.getModels().stream()
                .map(this::convertModelToSnapshot)
                .collect(Collectors.toList());
        snapshot.setModels(models);

        return snapshot;
    }

    /**
     * Convert Model entity to snapshot DTO
     */
    private DiagramSnapshotDto.ModelSnapshotDto convertModelToSnapshot(Model model) {
        DiagramSnapshotDto.ModelSnapshotDto modelDto = new DiagramSnapshotDto.ModelSnapshotDto();
        modelDto.setId(model.getId());
        modelDto.setName(model.getName());
        modelDto.setPositionX(model.getPositionX());
        modelDto.setPositionY(model.getPositionY());

        // Convert attributes
        List<DiagramSnapshotDto.AttributeSnapshotDto> attributes = model.getAttributes().stream()
                .sorted((a1, a2) -> a1.getAttributeOrder().compareTo(a2.getAttributeOrder()))
                .map(this::convertAttributeToSnapshot)
                .collect(Collectors.toList());
        modelDto.setAttributes(attributes);

        return modelDto;
    }

    /**
     * Convert Attribute entity to snapshot DTO
     */
    private DiagramSnapshotDto.AttributeSnapshotDto convertAttributeToSnapshot(Attribute attribute) {
        DiagramSnapshotDto.AttributeSnapshotDto attrDto = new DiagramSnapshotDto.AttributeSnapshotDto();
        attrDto.setId(attribute.getId());
        attrDto.setName(attribute.getName());
        attrDto.setDataType(attribute.getDataType());
        attrDto.setLength(attribute.getLength());
        attrDto.setIsNullable(attribute.getIsNullable());
        attrDto.setIsPrimaryKey(attribute.getIsPrimaryKey());
        attrDto.setIsForeignKey(attribute.getIsForeignKey());
        attrDto.setAttributeOrder(attribute.getAttributeOrder());

        // Convert connection if exists
        if (attribute.getConnection() != null && !attribute.getConnection().isEmpty()) {
            Connection conn = attribute.getConnection().get(0);
            DiagramSnapshotDto.ConnectionSnapshotDto connDto = new DiagramSnapshotDto.ConnectionSnapshotDto();
            connDto.setTargetModelId(conn.getTargetModel() != null ? conn.getTargetModel().getId() : null);
            connDto.setTargetAttributeId(conn.getTargetAttributeId());
            connDto.setForeignKeyName(conn.getForeignKeyName());
            attrDto.setConnection(connDto);
        }

        return attrDto;
    }

    /**
     * Tạo SHA-256 hash từ JSON string
     */
    private String generateHash(String input) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(input.getBytes(StandardCharsets.UTF_8));

            // Convert byte array to hex string
            StringBuilder hexString = new StringBuilder();
            for (byte b : hash) {
                String hex = Integer.toHexString(0xff & b);
                if (hex.length() == 1)
                    hexString.append('0');
                hexString.append(hex);
            }
            return hexString.toString();
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException("Error generating hash", e);
        }
    }

    /**
     * Lấy danh sách lịch sử migration của một diagram
     */
    @Transactional(readOnly = true)
    public List<MigrationHistoryDto> getMigrationHistory(Long diagramId) {
        List<Migration> migrations = migrationRepository.findByDatabaseDiagramIdOrderByCreatedAtDesc(diagramId);

        return migrations.stream()
                .map(m -> new MigrationHistoryDto(
                        m.getId(),
                        m.getUsername(),
                        m.getCreatedAt(),
                        m.getSnapshotHash(),
                        m.getDatabaseDiagram().getId(),
                        m.getDatabaseDiagram().getName()))
                .collect(Collectors.toList());
    }

    /**
     * Lấy chi tiết một migration snapshot
     */
    @Transactional(readOnly = true)
    public MigrationDetailDto getMigrationDetail(Long migrationId) {
        Migration migration = migrationRepository.findById(migrationId)
                .orElseThrow(() -> new RuntimeException("Migration not found: " + migrationId));

        return new MigrationDetailDto(
                migration.getId(),
                migration.getUsername(),
                migration.getCreatedAt(),
                migration.getSnapshotHash(),
                migration.getSnapshotJson(),
                migration.getDatabaseDiagram().getId(),
                migration.getDatabaseDiagram().getName());
    }
}