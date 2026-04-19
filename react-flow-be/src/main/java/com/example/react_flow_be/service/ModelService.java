package com.example.react_flow_be.service;

import com.example.react_flow_be.dto.ModelDto;
import com.example.react_flow_be.entity.*;
import com.example.react_flow_be.repository.ModelRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class ModelService {

    private final ModelRepository modelRepository;
    private final AttributeService attributeService;

    @Transactional
    public boolean updateModelPosition(String modelId, Double positionX, Double positionY, LocalDateTime timestamp) {
        // Use modelId for more accurate lookup
        Optional<Model> modelOpt = modelRepository.findByIdForUpdate(modelId);
        if (modelOpt.isPresent() && (modelOpt.get().getPositionUpdatedAt() == null ||
                modelOpt.get().getPositionUpdatedAt().isBefore(timestamp))) {
            Model model = modelOpt.get();
            model.setPositionX(positionX);
            model.setPositionY(positionY);
            model.setPositionUpdatedAt(timestamp);
            modelRepository.save(model);
            return true;

        }
        return false;
    }

    @Transactional
    public Model createModel(String name, String id, Double x, Double y, Boolean isChild,
            DatabaseDiagram databaseDiagram) {
        Model model = new Model();

        model.setId(id);
        model.setName("Model");
        model.setPositionX(x);
        model.setPositionY(y);
        model.setDatabaseDiagram(databaseDiagram);

        return modelRepository.save(model);
    }

    public ModelDto convertToModelDto(Model model) {
        return new ModelDto(
                model.getId(),
                model.getNodeId(),
                model.getName(),
                model.getPositionX(),
                model.getPositionY(),
                attributeService.convertToDtoList(model.getAttributes()));
    }

    public String getModelNameByNodeId(String nodeId) {
        return modelRepository.findAll().stream()
                .filter(model -> nodeId.equals(model.getNodeId()))
                .map(Model::getName)
                .findFirst()
                .orElse("Unknown");
    }
}
