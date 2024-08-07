package org.codinjutsu.tools.jenkins.view.parameter.renderer;

import org.codinjutsu.tools.jenkins.model.jenkins.JobParameter;
import org.codinjutsu.tools.jenkins.model.jenkins.JobParameterType;
import org.codinjutsu.tools.jenkins.model.jenkins.ProjectJob;
import org.codinjutsu.tools.jenkins.view.extension.JobParameterRenderer;
import org.codinjutsu.tools.jenkins.view.extension.JobParameterRenderers;
import org.codinjutsu.tools.jenkins.view.parameter.JobParameterComponent;
import org.jetbrains.annotations.NotNull;

import java.util.HashMap;
import java.util.Map;
import java.util.function.BiFunction;

public class PersistentParameterRenderer extends AbstractParameterRenderer implements JobParameterRenderer {

    public static final JobParameterType BOOLEAN = new JobParameterType("PersistentBooleanParameterDefinition",
            "com.gem.persistentparameter.PersistentBooleanParameterDefinition");

    public static final JobParameterType STRING = new JobParameterType("PersistentStringParameterDefinition",
            "com.gem.persistentparameter.PersistentStringParameterDefinition");

    public static final JobParameterType CHOICE = new JobParameterType("PersistentChoiceParameterDefinition",
            "com.gem.persistentparameter.PersistentChoiceParameterDefinition");

    public static final JobParameterType TEXT = new JobParameterType("PersistentTextParameterDefinition",
            "com.gem.persistentparameter.PersistentTextParameterDefinition");

    private final Map<JobParameterType, BiFunction<JobParameter, String, JobParameterComponent<?>>> converter = new HashMap<>();

    public PersistentParameterRenderer() {
        converter.put(BOOLEAN, JobParameterRenderers::createCheckBox);
        converter.put(STRING, JobParameterRenderers::createTextField);
        converter.put(TEXT, JobParameterRenderers::createTextArea);
        converter.put(CHOICE, JobParameterRenderers::createComboBox);
    }

    @Override
    protected JobParameterComponent<?> getJobParameterComponent(JobParameter jobParameter, ProjectJob projectJob, String defaultValue) {
        return converter.getOrDefault(jobParameter.getJobParameterType(), JobParameterRenderers::createTextField)
                .apply(jobParameter, defaultValue);
    }

    @Override
    public boolean isForJobParameter(@NotNull JobParameter jobParameter) {
        return converter.containsKey(jobParameter.getJobParameterType());
    }
}
