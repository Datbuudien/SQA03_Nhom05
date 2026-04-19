// CollaborationDTO.java
package com.example.react_flow_be.dto.collaboration;

import com.example.react_flow_be.entity.Collaboration;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class CollaborationDTO {
    private Long id;
    private String username;
    private Collaboration.CollaborationType type;
    private Collaboration.Permission permission;
    private Boolean isActive;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    public static CollaborationDTO fromEntity(Collaboration collaboration) {
        return new CollaborationDTO(
                collaboration.getId(),
                collaboration.getUsername(),
                collaboration.getType(),
                collaboration.getPermission(),
                collaboration.getIsActive(),
                collaboration.getCreatedAt(),
                collaboration.getUpdatedAt());
    }
}