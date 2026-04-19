package com.example.react_flow_be.entity;

import java.util.List;

import jakarta.persistence.Entity;
import jakarta.persistence.OneToMany;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

@Entity
@Data
@EqualsAndHashCode(callSuper = true)
@NoArgsConstructor
@AllArgsConstructor
public class CreativeDiagram extends Diagram {
    private String theme;
    private String templateId;
    
    @OneToMany(mappedBy = "creativeDiagram")
    private List<Shape> shapes;
    
    @OneToMany(mappedBy = "creativeDiagram")
    private List<History> histories;
}