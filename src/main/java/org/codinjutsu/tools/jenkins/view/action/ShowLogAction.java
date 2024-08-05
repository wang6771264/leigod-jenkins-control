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

package org.codinjutsu.tools.jenkins.view.action;

import com.intellij.icons.AllIcons;
import com.intellij.openapi.actionSystem.ActionUpdateThread;
import com.intellij.openapi.actionSystem.AnAction;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.project.DumbAware;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.NlsActions;
import lombok.Value;
import org.codinjutsu.tools.jenkins.enums.BuildTypeEnum;
import org.codinjutsu.tools.jenkins.model.jenkins.Job;
import org.codinjutsu.tools.jenkins.view.ui.BrowserPanel;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.swing.*;

public class ShowLogAction extends AnAction implements DumbAware {

    private static final Icon ICON = AllIcons.Actions.Show;//AllIcons.Nodes.Console

    private final BuildTypeEnum buildTypeEnum;

    public ShowLogAction(BuildTypeEnum buildTypeEnum) {
        super(ICON);
        final ShowLogActionText actionText = getActionText(buildTypeEnum);
        getTemplatePresentation().setText(actionText.getText());
        getTemplatePresentation().setDescription(actionText.getDescription());
        this.buildTypeEnum = buildTypeEnum;
    }

    public static boolean isAvailable(@Nullable Job job, @NotNull BuildTypeEnum buildTypeEnum) {
        return job != null
                && job.isBuildable()
                && isLogAvailable(job, buildTypeEnum)
                && !job.isInQueue();
    }

    @NotNull
    static ShowLogActionText getActionText(BuildTypeEnum buildTypeEnum) {
        final ShowLogActionText logActionText;
        switch (buildTypeEnum) {
            case LAST_SUCCESSFUL:
                logActionText = new ShowLogActionText("Show last successful log", "Show last successful build's log");
                break;
            case LAST_FAILED:
                logActionText = new ShowLogActionText("Show last failed log", "Show last failed build's log");
                break;
            case LAST://Fallthrough
            default:
                logActionText = new ShowLogActionText("Show last log", "Show last build's log");
        }
        return logActionText;
    }

    private static boolean isLogAvailable(@NotNull Job buildableJob, @NotNull BuildTypeEnum buildTypeEnum) {
        return buildableJob.getAvailableBuildTypeEnums().contains(buildTypeEnum);
    }

    @Override
    public void actionPerformed(AnActionEvent event) {
        ActionUtil.getProject(event).ifPresent(this::actionPerformed);
    }

    private void actionPerformed(Project project) {
        final BrowserPanel browserPanel = BrowserPanel.getInstance(project);
        final Job job = browserPanel.getSelectedJob();
        if (job != null) {
            final LogToolWindow logToolWindow = new LogToolWindow(project);
            logToolWindow.showLog(buildTypeEnum, job);
        }
    }

    public boolean isAvailable(@Nullable Job job) {
        return isAvailable(job, buildTypeEnum);
    }

    @Override
    public void update(AnActionEvent event) {
        final boolean canShowLogForLastBuild = ActionUtil.getBrowserPanel(event).map(BrowserPanel::getSelectedJob)
                .map(this::isAvailable).orElse(Boolean.FALSE);
        event.getPresentation().setVisible(canShowLogForLastBuild);
    }

    @Override
    public @NotNull ActionUpdateThread getActionUpdateThread() {
        return ActionUpdateThread.BGT;
    }

    @Value
    static class ShowLogActionText {

        @Nullable @NlsActions.ActionText String text;
        @Nullable @NlsActions.ActionDescription String description;

    }
}
