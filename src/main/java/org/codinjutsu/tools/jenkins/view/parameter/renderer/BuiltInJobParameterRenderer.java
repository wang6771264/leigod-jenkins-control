package org.codinjutsu.tools.jenkins.view.parameter.renderer;

import org.codinjutsu.tools.jenkins.model.jenkins.BuildInJobParameter;
import org.codinjutsu.tools.jenkins.model.jenkins.JobParameter;
import org.codinjutsu.tools.jenkins.model.jenkins.JobParameterType;
import org.codinjutsu.tools.jenkins.model.jenkins.ProjectJob;
import org.codinjutsu.tools.jenkins.view.extension.JobParameterRenderer;
import org.codinjutsu.tools.jenkins.view.extension.JobParameterRenderers;
import org.codinjutsu.tools.jenkins.view.parameter.JobParameterComponent;
import org.jetbrains.annotations.NotNull;

import javax.annotation.Nonnull;
import javax.swing.*;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.function.BiFunction;

public class BuiltInJobParameterRenderer extends AbstractParameterRenderer implements JobParameterRenderer {

    private final Map<JobParameterType, BiFunction<JobParameter, String, JobParameterComponent<?>>> converter = new HashMap<>();

    public BuiltInJobParameterRenderer() {
        converter.put(BuildInJobParameter.ChoiceParameterDefinition, JobParameterRenderers::createComboBox);
        converter.put(BuildInJobParameter.BooleanParameterDefinition, JobParameterRenderers::createCheckBox);
        converter.put(BuildInJobParameter.StringParameterDefinition, JobParameterRenderers::createTextField);
        converter.put(BuildInJobParameter.PasswordParameterDefinition, JobParameterRenderers::createPasswordField);
        converter.put(BuildInJobParameter.TextParameterDefinition, JobParameterRenderers::createTextArea);
        converter.put(BuildInJobParameter.FileParameterDefinition, JobParameterRenderers::createFileUpload);
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

    @Nonnull
    @Override
    public Optional<JLabel> createLabel(@NotNull JobParameter jobParameter) {
        final JobParameterType jobParameterType = jobParameter.getJobParameterType();
        final Optional<JLabel> label = super.createLabel(jobParameter);
        if (BuildInJobParameter.TextParameterDefinition.equals(jobParameterType)) {
            label.ifPresent(textAreaLabel -> textAreaLabel.setVerticalAlignment(SwingConstants.TOP));
        }
        return label;
    }
}
