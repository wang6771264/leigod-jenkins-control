package org.codinjutsu.tools.jenkins.view.parameter.renderer;

import org.codinjutsu.tools.jenkins.model.jenkins.JobParameter;
import org.codinjutsu.tools.jenkins.model.jenkins.JobParameterType;
import org.codinjutsu.tools.jenkins.model.jenkins.ProjectJob;
import org.codinjutsu.tools.jenkins.view.extension.JobParameterRenderer;
import org.codinjutsu.tools.jenkins.view.parameter.JobParameterComponent;
import org.jetbrains.annotations.NonNls;
import org.jetbrains.annotations.NotNull;

import java.util.HashSet;
import java.util.Set;

public class ListGitBranchesParameterRenderer extends AbstractParameterRenderer implements JobParameterRenderer {

    @NonNls
    private static final String TYPE_CLASS = "com.syhuang.hudson.plugins.listgitbranchesparameter.ListGitBranchesParameterDefinition";

    public static final JobParameterType PT_TAG = new JobParameterType("PT_TAG", TYPE_CLASS);

    public static final JobParameterType PT_BRANCH = new JobParameterType("PT_BRANCH", TYPE_CLASS);

    public static final JobParameterType PT_BRANCH_TAG = new JobParameterType("PT_BRANCH_TAG", TYPE_CLASS);

    private final GitParameterRenderer parameterRenderer;

    public ListGitBranchesParameterRenderer() {
        final Set<JobParameterType> validTypes = new HashSet<>();
        validTypes.add(PT_TAG);
        validTypes.add(PT_BRANCH);
        validTypes.add(PT_BRANCH_TAG);
        this.parameterRenderer = new GitParameterRenderer(validTypes);
    }

    @Override
    protected JobParameterComponent<String> getJobParameterComponent(JobParameter jobParameter, ProjectJob projectJob, String defaultValue) {
        return parameterRenderer.render(jobParameter, projectJob);
    }

    @Override
    public boolean isForJobParameter(@NotNull JobParameter jobParameter) {
        return parameterRenderer.isForJobParameter(jobParameter);
    }
}
