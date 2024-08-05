package org.codinjutsu.tools.jenkins.view.parameter.renderer;

import org.codinjutsu.tools.jenkins.model.jenkins.JobParameter;
import org.codinjutsu.tools.jenkins.model.jenkins.JobParameterType;
import org.codinjutsu.tools.jenkins.model.jenkins.ProjectJob;
import org.codinjutsu.tools.jenkins.view.extension.JobParameterRenderer;
import org.codinjutsu.tools.jenkins.view.parameter.JobParameterComponent;
import org.jetbrains.annotations.NotNull;

import javax.swing.*;

public class HiddenParameterRenderer extends AbstractParameterRenderer implements JobParameterRenderer {

    static final JobParameterType TYPE = new JobParameterType("WHideParameterDefinition",
            "com.wangyin.parameter.WHideParameterDefinition");


    @Override
    protected JobParameterComponent<Boolean> getJobParameterComponent(JobParameter jobParameter, ProjectJob projectJob, String defaultValue) {
        return new JobParameterComponent<>(jobParameter, new JLabel(), false);
    }

    @Override
    public boolean isForJobParameter(@NotNull JobParameter jobParameter) {
        return TYPE.equals(jobParameter.getJobParameterType());
    }
}
