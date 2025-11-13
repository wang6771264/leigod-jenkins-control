package org.codinjutsu.tools.jenkins.view.virtualJob;

import lombok.Data;
import org.codinjutsu.tools.jenkins.model.jenkins.BuildParameter;

@Data
public class VirtualJobParameter {
    private int stepOrder;
    private String stepName;
    private BuildParameter parameter;
    private boolean required;
}