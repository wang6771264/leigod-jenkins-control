package org.codinjutsu.tools.jenkins.view.parameter.renderer;

import org.codinjutsu.tools.jenkins.model.jenkins.JobParameter;
import org.codinjutsu.tools.jenkins.model.jenkins.JobParameterType;
import org.codinjutsu.tools.jenkins.model.jenkins.ProjectJob;
import org.codinjutsu.tools.jenkins.view.extension.JobParameterRenderer;
import org.codinjutsu.tools.jenkins.view.extension.JobParameterRenderers;
import org.codinjutsu.tools.jenkins.view.parameter.JobParameterComponent;
import org.jetbrains.annotations.NonNls;
import org.jetbrains.annotations.NotNull;

import javax.swing.*;
import java.util.HashMap;
import java.util.Map;
import java.util.function.BiFunction;

public class ExtendedChoiceParameterRenderer extends AbstractParameterRenderer implements JobParameterRenderer {

    @NonNls
    private static final String TYPE_CLASS = "com.cwctravel.hudson.plugins.extended_choice_parameter.ExtendedChoiceParameterDefinition";

    static final JobParameterType PT_SINGLE_SELECT = new JobParameterType("PT_SINGLE_SELECT", TYPE_CLASS);

    static final JobParameterType PT_MULTI_SELECT = new JobParameterType("PT_MULTI_SELECT", TYPE_CLASS);

    static final JobParameterType PT_CHECKBOX = new JobParameterType("PT_CHECKBOX", TYPE_CLASS);

    static final JobParameterType PT_RADIO = new JobParameterType("PT_RADIO", TYPE_CLASS);

    static final JobParameterType PT_TEXTBOX = new JobParameterType("PT_TEXTBOX", TYPE_CLASS);

    static final JobParameterType PT_HIDDEN = new JobParameterType("PT_HIDDEN", TYPE_CLASS);

    public ExtendedChoiceParameterRenderer() {
        converter.put(PT_SINGLE_SELECT, JobParameterRenderers::createComboBoxIfChoicesExists);
        converter.put(PT_MULTI_SELECT, JobParameterRenderers::createComboBoxIfChoicesExists);
        converter.put(PT_CHECKBOX, JobParameterRenderers::createComboBoxIfChoicesExists);
        converter.put(PT_RADIO, JobParameterRenderers::createTextField);
        converter.put(PT_TEXTBOX, JobParameterRenderers::createTextField);
        converter.put(PT_HIDDEN, (jobParameter, name) -> new JobParameterComponent<>(jobParameter, new JLabel(), false));
    }

    private final Map<JobParameterType, BiFunction<JobParameter, String, JobParameterComponent<String>>> converter =
            new HashMap<>();

    @Override
    protected JobParameterComponent getJobParameterComponent(JobParameter jobParameter, ProjectJob projectJob, String defaultValue) {
        BiFunction<JobParameter, String, JobParameterComponent<String>> biFunction = converter.get(jobParameter.getJobParameterType());
        if (biFunction == null) {
            biFunction = JobParameterRenderers::createTextField;
        }
        return biFunction.apply(jobParameter, defaultValue);
    }

    @Override
    public boolean isForJobParameter(@NotNull JobParameter jobParameter) {
        return converter.containsKey(jobParameter.getJobParameterType());
    }
}
