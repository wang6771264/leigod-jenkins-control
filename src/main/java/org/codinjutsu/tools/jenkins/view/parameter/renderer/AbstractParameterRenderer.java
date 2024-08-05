package org.codinjutsu.tools.jenkins.view.parameter.renderer;

import org.codinjutsu.tools.jenkins.model.jenkins.Build;
import org.codinjutsu.tools.jenkins.model.jenkins.BuildParameter;
import org.codinjutsu.tools.jenkins.model.jenkins.JobParameter;
import org.codinjutsu.tools.jenkins.model.jenkins.ProjectJob;
import org.codinjutsu.tools.jenkins.view.extension.JobParameterRenderer;
import org.codinjutsu.tools.jenkins.view.parameter.JobParameterComponent;
import org.jetbrains.annotations.NonNls;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.Optional;

public abstract class AbstractParameterRenderer implements JobParameterRenderer {

    @NonNls
    protected static final String TYPE_CLASS_PREFIX = "org.biouno.unochoice.";

    @Override
    public @NotNull JobParameterComponent render(@NotNull JobParameter jobParameter, @Nullable ProjectJob projectJob) {
        Build build = projectJob.getLastBuild();
        List<BuildParameter> buildParameterList = build.getBuildParameterList();
        Optional<BuildParameter> first = buildParameterList.stream()
                .filter(buildParameter -> buildParameter.getName().equals(jobParameter.getName())).findFirst();

        return this.getJobParameterComponent(jobParameter, projectJob,
                first.map(BuildParameter::getValue).orElse(jobParameter.getDefaultValue()));
    }

    protected abstract JobParameterComponent getJobParameterComponent(JobParameter jobParameter,
                                                                      ProjectJob projectJob,
                                                                      String defaultValue);

}
