package org.codinjutsu.tools.jenkins.view.parameter.renderer;

import org.codinjutsu.tools.jenkins.model.jenkins.JobParameter;
import org.codinjutsu.tools.jenkins.model.jenkins.ProjectJob;
import org.codinjutsu.tools.jenkins.view.extension.JobParameterRenderer;
import org.codinjutsu.tools.jenkins.view.parameter.JobParameterComponent;
import org.jetbrains.annotations.NonNls;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public abstract class AbstractParameterRenderer implements JobParameterRenderer {

    @NonNls
    protected static final String TYPE_CLASS_PREFIX = "org.biouno.unochoice.";

    @Override
    public @NotNull JobParameterComponent render(@NotNull JobParameter jobParameter,
                                                 @Nullable ProjectJob projectJob) {
        //这里获取上一次的构建参数
//        Build build = projectJob.getLastBuild();
//        List<BuildParameter> buildParameterList = build.getBuildParameterList();
//        Optional<BuildParameter> first = buildParameterList.stream()
//                .filter(buildParameter -> buildParameter.getName().equals(jobParameter.getName())).findFirst();

        return this.getJobParameterComponent(jobParameter, projectJob, jobParameter.getDefaultValue());
    }

    protected abstract JobParameterComponent getJobParameterComponent(JobParameter jobParameter,
                                                                      ProjectJob projectJob,
                                                                      String defaultValue);

}
