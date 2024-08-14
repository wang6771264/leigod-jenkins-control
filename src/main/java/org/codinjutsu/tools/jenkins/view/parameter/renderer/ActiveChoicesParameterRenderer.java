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

import static org.codinjutsu.tools.jenkins.model.jenkins.JobParameterType.createTypeForClassPrefix;

public class ActiveChoicesParameterRenderer extends AbstractParameterRenderer implements JobParameterRenderer {

    @NonNls
    private static final String TYPE_CLASS_PREFIX = "org.biouno.unochoice.";

    public static final JobParameterType CHOICE_PARAMETER = createTypeForClassPrefix("ChoiceParameter", TYPE_CLASS_PREFIX);
    public static final JobParameterType DYNAMIC_REFERENCE_PARAMETER = createTypeForClassPrefix("DynamicReferenceParameter", TYPE_CLASS_PREFIX);
    private final Map<JobParameterType, BiFunction<JobParameter, String, JobParameterComponent<String>>> converter =
            new HashMap<>();

    public ActiveChoicesParameterRenderer() {
        converter.put(CHOICE_PARAMETER, JobParameterRenderers::createComboBoxIfChoicesExists);
        converter.put(DYNAMIC_REFERENCE_PARAMETER, JobParameterRenderers::createLabel);
    }

    @Override
    protected JobParameterComponent<String> getJobParameterComponent(JobParameter jobParameter, ProjectJob projectJob, String defaultValue) {
        return converter.getOrDefault(jobParameter.getJobParameterType(), JobParameterRenderers::createTextField)
                .apply(jobParameter, defaultValue);
    }

    @Override
    public boolean isForJobParameter(@NotNull JobParameter jobParameter) {
        return converter.containsKey(jobParameter.getJobParameterType());
    }
}
