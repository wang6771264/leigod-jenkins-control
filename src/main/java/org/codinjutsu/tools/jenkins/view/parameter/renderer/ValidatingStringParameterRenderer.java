package org.codinjutsu.tools.jenkins.view.parameter.renderer;

import org.codinjutsu.tools.jenkins.model.jenkins.JobParameter;
import org.codinjutsu.tools.jenkins.model.jenkins.JobParameterType;
import org.codinjutsu.tools.jenkins.model.jenkins.ProjectJob;
import org.codinjutsu.tools.jenkins.view.extension.JobParameterRenderer;
import org.codinjutsu.tools.jenkins.view.extension.JobParameterRenderers;
import org.codinjutsu.tools.jenkins.view.parameter.JobParameterComponent;
import org.jetbrains.annotations.NonNls;
import org.jetbrains.annotations.NotNull;

public class ValidatingStringParameterRenderer extends AbstractParameterRenderer implements JobParameterRenderer {

    @NonNls
    private static final String TYPE_CLASS = "hudson.plugins.validating_string_parameter.ValidatingStringParameterDefinition";

    static final JobParameterType TYPE = new JobParameterType("ValidatingStringParameterDefinition", TYPE_CLASS);

    @Override
    protected JobParameterComponent<String> getJobParameterComponent(JobParameter jobParameter, ProjectJob projectJob, String defaultValue) {
        return JobParameterRenderers.createTextField(jobParameter, defaultValue);
    }

    @Override
    public boolean isForJobParameter(@NotNull JobParameter jobParameter) {
        return TYPE.equals(jobParameter.getJobParameterType());
    }
}
