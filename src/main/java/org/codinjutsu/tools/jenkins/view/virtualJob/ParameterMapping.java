package org.codinjutsu.tools.jenkins.view.virtualJob;

import lombok.Data;

@Data
public class ParameterMapping {
    private String targetParameter;
    private ParameterSource source;
    private String sourceKey;
}