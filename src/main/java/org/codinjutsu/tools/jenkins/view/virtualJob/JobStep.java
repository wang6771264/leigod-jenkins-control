package org.codinjutsu.tools.jenkins.view.virtualJob;

import lombok.Data;
import org.codinjutsu.tools.jenkins.model.jenkins.Job;

import java.util.ArrayList;
import java.util.List;

/**
 * 任务步骤
 */
@Data
public class JobStep {
    private int order;
    private Job realJob;
    private JobStep dependsOn;
    private List<ParameterMapping> inputMappings = new ArrayList<>();
}