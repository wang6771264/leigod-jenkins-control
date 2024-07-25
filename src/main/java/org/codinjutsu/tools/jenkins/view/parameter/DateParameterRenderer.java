package org.codinjutsu.tools.jenkins.view.parameter;

import org.codinjutsu.tools.jenkins.model.jenkins.JobParameter;
import org.codinjutsu.tools.jenkins.model.jenkins.JobParameterType;
import org.codinjutsu.tools.jenkins.model.jenkins.ProjectJob;
import org.codinjutsu.tools.jenkins.view.extension.JobParameterRenderer;
import org.codinjutsu.tools.jenkins.view.extension.JobParameterRenderers;
import org.jetbrains.annotations.NonNls;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class DateParameterRenderer implements JobParameterRenderer {

    @NonNls
    private static final String TYPE_CLASS = "me.leejay.jenkins.dateparameter.DateParameterDefinition";

    static final JobParameterType DATE_PARAMETER = new JobParameterType("DateParameterDefinition", TYPE_CLASS);

    @NotNull
    @Override
    public JobParameterComponent<String> render(@NotNull JobParameter jobParameter, @Nullable ProjectJob projectJob) {
        return JobParameterRenderers.createTextField(jobParameter, jobParameter.getDefaultValue());
    }

    @Override
    public boolean isForJobParameter(@NotNull JobParameter jobParameter) {
        return DATE_PARAMETER.equals(jobParameter.getJobParameterType());
    }
}
