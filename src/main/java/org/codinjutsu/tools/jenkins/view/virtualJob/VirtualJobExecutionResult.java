package org.codinjutsu.tools.jenkins.view.virtualJob;

import lombok.Data;
import org.codinjutsu.tools.jenkins.model.jenkins.Build;

import java.util.Map;

@Data
public class VirtualJobExecutionResult {
    private String virtualJobName;
    private boolean success;
    private int failedStep = -1;
    private String errorMessage;
    private Map<Integer, Build> stepResults;
}