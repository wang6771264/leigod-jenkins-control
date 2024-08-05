package org.codinjutsu.tools.jenkins.view.parameter.renderer;

import org.codinjutsu.tools.jenkins.model.jenkins.JobParameter;
import org.codinjutsu.tools.jenkins.model.jenkins.JobParameterType;
import org.codinjutsu.tools.jenkins.model.jenkins.ProjectJob;
import org.codinjutsu.tools.jenkins.view.extension.JobParameterRenderer;
import org.codinjutsu.tools.jenkins.view.extension.JobParameterRenderers;
import org.codinjutsu.tools.jenkins.view.parameter.JobParameterComponent;
import org.jetbrains.annotations.NonNls;
import org.jetbrains.annotations.NotNull;

public class DateParameterRenderer extends AbstractParameterRenderer implements JobParameterRenderer {

    @NonNls
    private static final String TYPE_CLASS = "me.leejay.jenkins.dateparameter.DateParameterDefinition";

    static final JobParameterType DATE_PARAMETER = new JobParameterType("DateParameterDefinition", TYPE_CLASS);

    @Override
    protected JobParameterComponent getJobParameterComponent(JobParameter jobParameter, ProjectJob projectJob, String defaultValue) {
        return JobParameterRenderers.createTextField(jobParameter, defaultValue);
    }

    @Override
    public boolean isForJobParameter(@NotNull JobParameter jobParameter) {
        return DATE_PARAMETER.equals(jobParameter.getJobParameterType());
    }
}
