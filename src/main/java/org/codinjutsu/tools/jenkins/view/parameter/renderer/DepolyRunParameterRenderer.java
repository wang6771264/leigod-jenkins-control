package org.codinjutsu.tools.jenkins.view.parameter.renderer;

import com.github.cliftonlabs.json_simple.JsonObject;
import org.codinjutsu.tools.jenkins.cache.JobCache;
import org.codinjutsu.tools.jenkins.logic.JenkinsJsonParser;
import org.codinjutsu.tools.jenkins.logic.RequestManager;
import org.codinjutsu.tools.jenkins.model.jenkins.*;
import org.codinjutsu.tools.jenkins.view.extension.JobParameterRenderer;
import org.codinjutsu.tools.jenkins.view.extension.JobParameterRenderers;
import org.codinjutsu.tools.jenkins.view.parameter.JobParameterComponent;
import org.jetbrains.annotations.NonNls;
import org.jetbrains.annotations.NotNull;

import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;


/**
 *
 */
public class DepolyRunParameterRenderer extends AbstractParameterRenderer implements JobParameterRenderer {

    @NonNls
    private static final String TYPE_CLASS = "hudson.model.RunParameterDefinition";

    private static final String BUILD_NUMBER = "number";
    private static final String JOB_NAME = "jobName";

    public static final JobParameterType RUN_PARAMETER_DEFINITION =
            new JobParameterType("RunParameterDefinition", TYPE_CLASS);

    private final Set<JobParameterType> validTypes = new HashSet<>();

    public DepolyRunParameterRenderer() {
        validTypes.add(RUN_PARAMETER_DEFINITION);
    }

    public DepolyRunParameterRenderer(Set<JobParameterType> validTypes) {
        this.validTypes.addAll(validTypes);
    }

    @Override
    protected JobParameterComponent getJobParameterComponent(JobParameter jobParameter, ProjectJob projectJob, String defaultValue) {
        if (!validTypes.contains(jobParameter.getJobParameterType())) {
            return JobParameterRenderers.createTextField(jobParameter, defaultValue);
        }
        //这里从默认值里面取值
        JsonObject defaultParamObj = jobParameter.getDefaultParamObj();
        if (defaultParamObj != null) {
            //默认值是最新的一个，支持选择其他
            String projectName = defaultParamObj.getString(JenkinsJsonParser.createJsonKey(JOB_NAME));
            Job deployJob = JobCache.get(projectName);
            if (deployJob != null) {
                List<BuildHistory> builds = RequestManager.getInstance(projectJob.getProject())
                        .findRecently50SuccessBuilds(deployJob);
                jobParameter.setChoices(builds.stream().map(build -> projectName + "#" + build.getNumber())
                        .collect(Collectors.toList()));
                defaultValue = jobParameter.getChoices().get(0);
            } else {
                String number = defaultParamObj.getString(JenkinsJsonParser.createJsonKey(BUILD_NUMBER));
                defaultValue = projectName + "#" + number;
            }
        }
        if (projectJob == null) {
            return JobParameterRenderers.createComboBoxIfChoicesExists(jobParameter, defaultValue);
        } else {
            jobParameter.setDefaultValue(defaultValue);
            return JobParameterRenderers.createGitParameterChoices(projectJob).apply(jobParameter);
        }
    }

    @Override
    public boolean isForJobParameter(@NotNull JobParameter jobParameter) {
        return validTypes.contains(jobParameter.getJobParameterType());
    }
}
