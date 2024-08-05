package org.codinjutsu.tools.jenkins.view.parameter.renderer;

import org.codinjutsu.tools.jenkins.model.jenkins.JobParameter;
import org.codinjutsu.tools.jenkins.model.jenkins.JobParameterType;
import org.codinjutsu.tools.jenkins.model.jenkins.ProjectJob;
import org.codinjutsu.tools.jenkins.view.extension.JobParameterRenderer;
import org.codinjutsu.tools.jenkins.view.extension.JobParameterRenderers;
import org.codinjutsu.tools.jenkins.view.parameter.JobParameterComponent;
import org.jetbrains.annotations.NonNls;
import org.jetbrains.annotations.NotNull;

import java.util.HashMap;
import java.util.Map;
import java.util.function.BiFunction;

public class ExtensibleChoiceParameterRenderer extends AbstractParameterRenderer implements JobParameterRenderer {

    @NonNls
    private static final String TYPE_CLASS = "jp.ikedam.jenkins.plugins.extensible_choice_parameter.ExtensibleChoiceParameterDefinition";

    static final JobParameterType TYPE = new JobParameterType("ExtensibleChoiceParameterDefinition", TYPE_CLASS);

    private final Map<JobParameterType, BiFunction<JobParameter, String, JobParameterComponent<String>>> converter =
            new HashMap<>();

    public ExtensibleChoiceParameterRenderer() {
        converter.put(TYPE, JobParameterRenderers::createComboBoxIfChoicesExists);
    }

    @Override
    protected JobParameterComponent getJobParameterComponent(JobParameter jobParameter, ProjectJob projectJob, String defaultValue) {
        return converter.getOrDefault(jobParameter.getJobParameterType(), JobParameterRenderers::createTextField)
                .apply(jobParameter, defaultValue);
    }

    @Override
    public boolean isForJobParameter(@NotNull JobParameter jobParameter) {
        return converter.containsKey(jobParameter.getJobParameterType());
    }
}
