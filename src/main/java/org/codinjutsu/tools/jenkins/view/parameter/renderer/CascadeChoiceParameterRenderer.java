package org.codinjutsu.tools.jenkins.view.parameter.renderer;

import com.intellij.openapi.project.Project;
import org.codinjutsu.tools.jenkins.example.SearchableComboBox;
import org.codinjutsu.tools.jenkins.model.jenkins.Job;
import org.codinjutsu.tools.jenkins.model.jenkins.JobParameter;
import org.codinjutsu.tools.jenkins.model.jenkins.JobParameterType;
import org.codinjutsu.tools.jenkins.model.jenkins.ProjectJob;
import org.codinjutsu.tools.jenkins.view.extension.JobParameterRenderer;
import org.codinjutsu.tools.jenkins.view.extension.JobParameterRenderers;
import org.codinjutsu.tools.jenkins.view.parameter.JobParameterComponent;
import org.codinjutsu.tools.jenkins.view.ui.BrowserPanel;
import org.jetbrains.annotations.NonNls;
import org.jetbrains.annotations.NotNull;

import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;


/**
 *
 */
public class CascadeChoiceParameterRenderer extends AbstractParameterRenderer implements JobParameterRenderer {

    @NonNls
    private static final String TYPE_CLASS = "org.biouno.unochoice.CascadeChoiceParameter";

    public static final String RELATIVE_PATH = "relativePath";
    public static final String BUILD_VER = "BUILD_VER";
    public static final String JOB_NAME = "JOB_NAME";

    public static final JobParameterType CASCADE_CHOICE_PARAMETER_DEFINITION =
            new JobParameterType("CascadeChoiceParameter", TYPE_CLASS);

    private final Set<JobParameterType> validTypes = new HashSet<>();

    /**
     * 存储级联的下拉列表
     */
    private final Map<String, SearchableComboBox> cascadeMap = new ConcurrentHashMap<>();

    public CascadeChoiceParameterRenderer() {
        validTypes.add(CASCADE_CHOICE_PARAMETER_DEFINITION);
    }

    public CascadeChoiceParameterRenderer(Set<JobParameterType> validTypes) {
        this.validTypes.addAll(validTypes);
    }

    @Override
    protected JobParameterComponent<String> getJobParameterComponent(JobParameter jobParameter, ProjectJob projectJob, String defaultValue) {
        //如果参数是JOB_NAME,则获取所有的job
        final Project project = projectJob.getProject();
        if (jobParameter.getName().equals(JOB_NAME)) {
            final BrowserPanel browserPanel = BrowserPanel.getInstance(project);
            List<Job> allJobs = browserPanel.getAllJobs();
            List<String> buildJobNames = allJobs.stream().map(Job::getName)
                    .filter(name -> name.endsWith("-build"))
                    .toList();
            //这里需要添加级联列表
            jobParameter.setChoices(buildJobNames);
            jobParameter.setDefaultValue(defaultValue);
            //如果参数是BUILD_VER和relativePath,则不处理写入一个空的列表
            SearchableComboBox artifactsCombo = new SearchableComboBox(RELATIVE_PATH,
                    List.of(), projectJob, null);
            SearchableComboBox buildVerCombo = new SearchableComboBox(BUILD_VER,
                    List.of(), projectJob, artifactsCombo);
            SearchableComboBox jobNameCombo = new SearchableComboBox(JOB_NAME,
                    buildJobNames, projectJob, buildVerCombo);
            //设置父级的下拉
            buildVerCombo.setParentComboBox(jobNameCombo);
            artifactsCombo.setParentComboBox(buildVerCombo);
            //设置当前参数的下拉
            jobParameter.setCascadeComboBox(jobNameCombo);
            // 为父级联下拉列表添加选项变化监听器
            cascadeMap.put(BUILD_VER, buildVerCombo);
            cascadeMap.put(RELATIVE_PATH, artifactsCombo);
        } else {
            SearchableComboBox cascadeCombo = cascadeMap.get(jobParameter.getName());
            //fixme 可能存在没有的情况,后续处理
            jobParameter.setCascadeComboBox(cascadeCombo);
        }
        return JobParameterRenderers.createCascadeComboBoxIfExists(jobParameter, defaultValue);
    }

    @Override
    public boolean isForJobParameter(@NotNull JobParameter jobParameter) {
        return validTypes.contains(jobParameter.getJobParameterType());
    }
}
