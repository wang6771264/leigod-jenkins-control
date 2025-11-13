package org.codinjutsu.tools.jenkins.view.virtualJob;

import com.intellij.openapi.project.Project;
import org.codinjutsu.tools.jenkins.model.jenkins.Build;
import org.codinjutsu.tools.jenkins.model.jenkins.BuildParameter;
import org.codinjutsu.tools.jenkins.model.jenkins.Job;
import org.codinjutsu.tools.jenkins.model.jenkins.JobParameter;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

/**
 * 虚拟任务管理器
 * 负责管理虚拟任务的创建、执行和参数映射
 */
public class VirtualJobManager {

    private final Project project;
    private final Map<String, VirtualJob> virtualJobs = new HashMap<>();

    public VirtualJobManager(Project project) {
        this.project = project;
    }

    /**
     * 创建虚拟任务
     */
    public VirtualJob createVirtualJob(@NotNull String name, @NotNull List<Job> jobs,
                                       @NotNull List<ParameterMapping> mappings) {
        VirtualJob virtualJob = new VirtualJob();
        virtualJob.setName(name);
        virtualJob.setDescription("Virtual job: " + name);

        // 创建任务步骤
        List<JobStep> steps = new ArrayList<>();
        for (int i = 0; i < jobs.size(); i++) {
            JobStep step = new JobStep();
            step.setOrder(i);
            step.setRealJob(jobs.get(i));
            if (i > 0) {
                step.setDependsOn(steps.get(i - 1));
            }
            steps.add(step);
        }
        virtualJob.setSteps(steps);
        virtualJob.setParameterMappings(mappings);
        virtualJobs.put(name, virtualJob);
        return virtualJob;
    }

    /**
     * 执行虚拟任务
     * @param virtualJob 虚拟任务
     * @param userInputs 用户输入的参数
     * @return 执行结果
     */
    public CompletableFuture<VirtualJobExecutionResult> executeVirtualJob(
            @NotNull VirtualJob virtualJob,
            @NotNull Map<String, String> userInputs) {

        return CompletableFuture.supplyAsync(() -> {
            VirtualJobExecutionResult result = new VirtualJobExecutionResult();
            result.setVirtualJobName(virtualJob.getName());

            // 上下文存储每一步的执行结果
            Map<Integer, Build> stepResults = new HashMap<>();

            try {
                for (JobStep step : virtualJob.getSteps()) {
                    // 准备当前步骤的参数
                    List<BuildParameter> parameters = prepareParameters(
                            step, userInputs
                    );

                    // 执行构建
                    Build build = this.executeBuild(step.getRealJob(), parameters);

                    if (build == null || !build.isSuccess()) {
                        result.setSuccess(false);
                        result.setFailedStep(step.getOrder());
                        result.setErrorMessage("Step " + step.getOrder() + " failed: " +
                                step.getRealJob().getName());
                        return result;
                    }

                    stepResults.put(step.getOrder(), build);
                }

                result.setSuccess(true);
                result.setStepResults(stepResults);
            } catch (Exception e) {
                result.setSuccess(false);
                result.setErrorMessage("Execution failed: " + e.getMessage());
            }
            return result;
        });
    }

    /**
     * 准备当前步骤的参数
     */
    private List<BuildParameter> prepareParameters(@NotNull JobStep step,
                                                   @NotNull Map<String, String> userInputs) {

        List<BuildParameter> parameters = new ArrayList<>();

        // 获取该步骤的参数映射
        for (ParameterMapping mapping : step.getInputMappings()) {
            BuildParameter parameter = new BuildParameter(
                    mapping.getTargetParameter(),
                    userInputs.get(mapping.getSourceKey()),
                    step.getRealJob().getUrl()
            );
            parameters.add(parameter);
        }
        return parameters;
    }

    /**
     * 执行单个Job的构建
     * 这里需要调用实际的Jenkins API
     */
    private Build executeBuild(@NotNull Job job, @NotNull List<BuildParameter> parameters) {
        // TODO: 调用Jenkins API执行构建
        // 需要等待构建完成并返回结果
        // 可以通过JenkinsRequestManager来实现
        return null;
    }

    /**
     * 收集虚拟任务所需的所有参数
     * 用于在Dialog中展示
     */
    public List<VirtualJobParameter> collectParameters(@NotNull VirtualJob virtualJob) {
        List<VirtualJobParameter> allParameters = new ArrayList<>();

        for (JobStep step : virtualJob.getSteps()) {
            Job realJob = step.getRealJob();

            // 获取该Job的所有参数
            List<JobParameter> jobParams = realJob.getParameters();

            for (JobParameter param : jobParams) {
                // 检查该参数是否需要用户输入
                boolean needUserInput = step.getInputMappings().stream()
                        .filter(m -> m.getTargetParameter().equals(param.getName()))
                        .anyMatch(m -> m.getSource() == ParameterSource.USER_INPUT);

                if (needUserInput) {
                    VirtualJobParameter vParam = new VirtualJobParameter();
                    vParam.setStepOrder(step.getOrder());
                    vParam.setStepName(realJob.getName());
                    vParam.setParameter(new BuildParameter(param.getName(), null, realJob.getUrl()));
                    vParam.setRequired(true);
                    allParameters.add(vParam);
                }
            }
        }

        return allParameters;
    }

}