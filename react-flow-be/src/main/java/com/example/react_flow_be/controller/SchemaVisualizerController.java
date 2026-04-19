package com.example.react_flow_be.controller;

import com.example.react_flow_be.dto.DatabaseDiagramDto;
import com.example.react_flow_be.dto.NewDiagramName;
import com.example.react_flow_be.service.CollaborationService;
import com.example.react_flow_be.service.DatabaseDiagramService;
import com.example.react_flow_be.service.SchemaVisualizerService;

import jakarta.servlet.http.HttpServletRequest;

import com.example.react_flow_be.repository.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/schema")
@RequiredArgsConstructor
@Slf4j
public class SchemaVisualizerController {

    private final SchemaVisualizerService schemaVisualizerService;
    private final CollaborationService collaborationService;
    private final ConnectionRepository connectionRepository;
    private final AttributeRepository attributeRepository;
    private final ModelRepository modelRepository;
    private final DatabaseDiagramService databaseDiagramService;
    private final DatabaseDiagramRepository databaseDiagramRepository;

    @GetMapping("/{diagramId}")
    public ResponseEntity<DatabaseDiagramDto> getSchemaData(@PathVariable Long diagramId, HttpServletRequest request) {
        String username = request.getHeader("X-Username");
        boolean hasAccess = collaborationService.hasAccess(diagramId, username);
        if (!hasAccess) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
        }
        log.info("Getting schema data");
        DatabaseDiagramDto response = schemaVisualizerService.getSchemaData(diagramId, username);
        log.info("Retrieved schema data: {} models", response.getModels().size());
        return ResponseEntity.ok(response);
    }

    @PostMapping("/initialize")
    public ResponseEntity<String> initializeSampleData() {
        try {
            log.info("Initializing sample data");
            schemaVisualizerService.initializeSampleData();
            return ResponseEntity.ok("Sample data initialized successfully");
        } catch (Exception e) {
            log.error("Error initializing sample data: {}", e.getMessage(), e);
            return ResponseEntity.internalServerError()
                    .body("Failed to initialize sample data: " + e.getMessage());
        }
    }

    @PostMapping("/{diagramId}/new-name")
    public ResponseEntity<Boolean> newName(@PathVariable Long diagramId, @RequestBody NewDiagramName newDiagramName) {
        Boolean success = databaseDiagramService.updateDiagramName(diagramId, newDiagramName.getNewName());
        return ResponseEntity.ok(success);
    }

    @PostMapping("/clear")
    @Transactional
    public ResponseEntity<String> clearAllData() {
        try {
            log.info("Clearing all data");

            // Delete in correct order to avoid foreign key constraints
            connectionRepository.deleteAll();
            log.info("Cleared all connections");

            attributeRepository.deleteAll();
            log.info("Cleared all attributes");

            modelRepository.deleteAll();
            log.info("Cleared all models");

            databaseDiagramRepository.deleteAll();
            log.info("Cleared all database diagrams");

            return ResponseEntity.ok("All data cleared successfully");
        } catch (Exception e) {
            log.error("Error clearing data: {}", e.getMessage(), e);
            return ResponseEntity.internalServerError()
                    .body("Failed to clear data: " + e.getMessage());
        }
    }
}