// AddCollaboratorRequest.java
package com.example.react_flow_be.dto.collaboration;

import com.example.react_flow_be.entity.Collaboration;
import lombok.Data;

@Data
public class AddCollaboratorRequest {
    private String username;
    private Collaboration.Permission permission;
}