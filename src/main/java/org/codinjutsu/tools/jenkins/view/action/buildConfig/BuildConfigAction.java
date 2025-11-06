/*
 * Copyright (c) 2013 David Boissier
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.codinjutsu.tools.jenkins.view.action.buildConfig;

import static org.codinjutsu.tools.jenkins.view.ui.BrowserPanel.POPUP_PLACE;

import com.intellij.openapi.actionSystem.ActionUpdateThread;
import com.intellij.openapi.actionSystem.AnAction;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.project.DumbAware;
import com.intellij.openapi.project.Project;
import java.util.Optional;
import org.codinjutsu.tools.jenkins.logic.RequestManagerInterface;
import org.codinjutsu.tools.jenkins.model.jenkins.Build;
import org.codinjutsu.tools.jenkins.model.jenkins.Job;
import org.codinjutsu.tools.jenkins.persistent.JenkinsAppSettings;
import org.codinjutsu.tools.jenkins.persistent.JenkinsSettings;
import org.codinjutsu.tools.jenkins.view.action.ActionUtil;
import org.codinjutsu.tools.jenkins.view.ui.BrowserPanel;
import org.codinjutsu.tools.jenkins.view.ui.BuildConfigDialog;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * ==========================
 * 开发：maple
 * 创建时间：2024/7/25 23:18
 * 版本：1.0
 * 描述：构建参数保存操作
 * ==========================
 */
public class BuildConfigAction extends AnAction implements DumbAware {

    public static final String ACTION_ID = "Jenkins.BuildConfig";
    private static final Logger LOG = Logger.getInstance(BuildConfigAction.class.getName());

    public static boolean isBuildable(@Nullable Job job) {
        return job != null && job.isBuildable();
    }

    @Override
    public void actionPerformed(@NotNull AnActionEvent event) {
        ActionUtil.getProject(event).ifPresent(this::actionPerformed);
    }

    private void actionPerformed(@NotNull Project project) {
        final BrowserPanel browserPanel = BrowserPanel.getInstance(project);
        try {
            Optional.ofNullable(browserPanel.getSelectedJob())
                    .ifPresent(job -> queueRunBuild(project, browserPanel, job));
        } catch (Exception ex) {
            final String message = ex.getMessage() == null ? "Unknown error" : ex.getMessage();
            LOG.error(message, ex);
            browserPanel.notifyErrorJenkinsToolWindow("Build cannot be run: " + message);
        }
    }

    @Override
    public @NotNull ActionUpdateThread getActionUpdateThread() {
        return ActionUpdateThread.BGT;
    }

    @Override
    public void update(@NotNull AnActionEvent event) {
        final boolean isBuildable = ActionUtil.getBrowserPanel(event).map(BrowserPanel::getSelectedJob)
                .map(BuildConfigAction::isBuildable).orElse(Boolean.FALSE);
        if (event.getPlace().equals(POPUP_PLACE)) {
            event.getPresentation().setVisible(isBuildable);
        } else {
            event.getPresentation().setEnabled(isBuildable);
        }
    }

    private void queueRunBuild(@NotNull Project project, @NotNull BrowserPanel browserPanel, @NotNull Job job) {
        final Optional<Build> previousLastBuild = Optional.ofNullable(job.getLastBuild());
        //显示构建参数
        showBuildConfigDialog(project, job, browserPanel);
    }

    private void showBuildConfigDialog(@NotNull Project project, @NotNull Job job, @NotNull BrowserPanel browserPanel) {
        RequestManagerInterface requestManager = browserPanel.getJenkinsManager();
        BuildConfigDialog.showDialog(project, job, JenkinsAppSettings.getSafeInstance(project),
                JenkinsSettings.getSafeInstance(project),
                requestManager);
    }
}
