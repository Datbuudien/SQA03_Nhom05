package com.example.react_flow_be.controller;

import com.example.react_flow_be.dto.ImportDiagramResponse;
import com.example.react_flow_be.service.DiagramImportService;
import com.example.react_flow_be.service.DatabaseDiagramService;
import com.example.react_flow_be.entity.DatabaseDiagram;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.nio.charset.StandardCharsets;

@RestController
@RequestMapping("/api/diagrams")
@RequiredArgsConstructor
@Slf4j
public class DiagramImportController {

    private final DiagramImportService diagramImportService;
    private final DatabaseDiagramService databaseDiagramService;

    @PostMapping("/create")
    public ResponseEntity<ImportDiagramResponse> createDiagram(
            @RequestParam("name") String diagramName,
            @RequestParam(value = "jsonFile", required = false) MultipartFile jsonFile,
            HttpServletRequest request) {

        String username = request.getHeader("X-Username");
        if (username == null || username.isEmpty()) {
            username = "guest";
        }

        try {
            Long diagramId;

            // Case 1: Import from JSON
            if (jsonFile != null && !jsonFile.isEmpty()) {
                log.info("Received import request for diagram: {} from user: {}", diagramName, username);

                // Validate file
                if (!jsonFile.getOriginalFilename().endsWith(".json")) {
                    return ResponseEntity.badRequest()
                            .body(new ImportDiagramResponse(false, null, "File must be a JSON file"));
                }

                // Read file content
                String jsonContent = new String(jsonFile.getBytes(), StandardCharsets.UTF_8);

                // Import diagram
                diagramId = diagramImportService.importDiagramFromJson(diagramName, jsonContent, username);
                log.info("Successfully imported diagram with ID: {}", diagramId);
            }
            // Case 2: Create blank diagram
            else {
                log.info("Creating blank diagram: {} for user: {}", diagramName, username);
                DatabaseDiagram diagram = databaseDiagramService.createBlankDiagram(diagramName, username);
                diagramId = diagram.getId();
                log.info("Successfully created blank diagram with ID: {}", diagramId);
            }

            return ResponseEntity.ok(
                    new ImportDiagramResponse(true, diagramId, "Diagram created successfully"));

        } catch (Exception e) {
            log.error("Error creating diagram: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(new ImportDiagramResponse(false, null, "Error creating diagram: " + e.getMessage()));
        }
    }
}