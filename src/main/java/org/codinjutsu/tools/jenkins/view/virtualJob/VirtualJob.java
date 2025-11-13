package org.codinjutsu.tools.jenkins.view.virtualJob;

import lombok.Data;

import java.util.List;

@Data
public class VirtualJob {
    private String name;
    private String description;
    private List<JobStep> steps;
    private List<ParameterMapping> parameterMappings;
}