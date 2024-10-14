package org.codinjutsu.tools.jenkins.view.action;

import com.intellij.openapi.actionSystem.ActionUpdateThread;
import com.intellij.openapi.actionSystem.AnAction;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.project.DumbAware;
import org.codinjutsu.tools.jenkins.persistent.JenkinsAppSettings;
import org.codinjutsu.tools.jenkins.model.jenkins.Build;
import org.codinjutsu.tools.jenkins.model.jenkins.BuildParameter;
import org.codinjutsu.tools.jenkins.model.jenkins.Job;
import org.codinjutsu.tools.jenkins.task.RunBuild;
import org.codinjutsu.tools.jenkins.task.callback.impl.RunBuildCallbacker;
import org.codinjutsu.tools.jenkins.view.ui.BrowserPanel;
import org.jetbrains.annotations.NotNull;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * ==========================
 * 开发：wei.wang
 * 创建时间：2024-08-08 18:13
 * 版本：1.0
 * 描述：根据上次成功构建参数重建
 * ==========================
 */
public class RetryLastSuccessfulBuildAction extends AnAction implements DumbAware {

    public static final String ACTION_ID = "Jenkins.RetryLastSuccessfulBuild";

    @Override
    public void actionPerformed(@NotNull AnActionEvent event) {
        ActionUtil.getProject(event).ifPresent(project -> {
            final BrowserPanel browserPanel = BrowserPanel.getInstance(project);
            Optional.ofNullable(browserPanel.getSelectedJob()).ifPresent(job -> {
                new RunBuild(project, job, JenkinsAppSettings.getSafeInstance(project),
                        getLastParamValueMap(job.getLastBuild()), browserPanel.getJenkinsManager(),
                        new RunBuildCallbacker(browserPanel)).queue();
            });
        });
    }

    @Override
    public void update(@NotNull AnActionEvent event) {
        Optional<BrowserPanel> browserPanelOptl = ActionUtil.getBrowserPanel(event);
        browserPanelOptl.ifPresent(browserPanel -> {
            Job job = browserPanel.getSelectedJob();
            //job不显示这个菜单
            if (job == null) {
                event.getPresentation().setVisible(false);
            }
        });
    }

    @Override
    public @NotNull ActionUpdateThread getActionUpdateThread() {
        return super.getActionUpdateThread();
    }

    /**
     * 获取上次参数值的map映射
     *
     * @return
     */
    @NotNull
    private Map<String, ?> getLastParamValueMap(Build build) {
        final HashMap<String, Object> valueByNameMap = new HashMap<>();
        if (build == null) {
            return valueByNameMap;
        }
        List<BuildParameter> buildParameters = build.getBuildParameterList();
        for (BuildParameter buildParameter : buildParameters) {
            valueByNameMap.put(buildParameter.getName(), buildParameter.getValue());
        }
        return valueByNameMap;
    }
}
