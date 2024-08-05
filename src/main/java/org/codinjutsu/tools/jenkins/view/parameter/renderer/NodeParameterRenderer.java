package org.codinjutsu.tools.jenkins.view.parameter.renderer;

import org.codinjutsu.tools.jenkins.model.jenkins.JobParameter;
import org.codinjutsu.tools.jenkins.model.jenkins.JobParameterType;
import org.codinjutsu.tools.jenkins.model.jenkins.ProjectJob;
import org.codinjutsu.tools.jenkins.view.extension.JobParameterRenderer;
import org.codinjutsu.tools.jenkins.view.extension.JobParameterRenderers;
import org.codinjutsu.tools.jenkins.view.parameter.JobParameterComponent;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.HashMap;
import java.util.Map;
import java.util.function.BiFunction;

public class NodeParameterRenderer extends AbstractParameterRenderer implements JobParameterRenderer {

    public static final JobParameterType NODE_PARAMETER = new JobParameterType("NodeParameterDefinition",
            "org.jvnet.jenkins.plugins.nodelabelparameter.NodeParameterDefinition");

    private final Map<JobParameterType, BiFunction<JobParameter, String, JobParameterComponent<String>>> converter = new HashMap<>();

    public NodeParameterRenderer() {
        converter.put(new JobParameterType("LabelParameterDefinition",
                        "org.jvnet.jenkins.plugins.nodelabelparameter.LabelParameterDefinition"),
                JobParameterRenderers::createTextField);
        converter.put(new JobParameterType("NodeParameterDefinition",
                        "org.jvnet.jenkins.plugins.nodelabelparameter.NodeParameterDefinition"),
                JobParameterRenderers::createComboBox);
    }

    @Override
    protected JobParameterComponent<String>  getJobParameterComponent(JobParameter jobParameter, ProjectJob projectJob, String defaultValue) {
        return converter.getOrDefault(jobParameter.getJobParameterType(), JobParameterRenderers::createTextField)
                .apply(jobParameter, defaultValue);
    }

    @Override
    public boolean isForJobParameter(@NotNull JobParameter jobParameter) {
        return converter.containsKey(jobParameter.getJobParameterType());
    }
}
