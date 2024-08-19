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

package org.codinjutsu.tools.jenkins.view.vcs;

import com.intellij.openapi.project.Project;
import com.intellij.openapi.vcs.changes.ChangeListDecorator;
import com.intellij.openapi.vcs.changes.LocalChangeList;
import com.intellij.ui.ColoredTreeCellRenderer;
import com.intellij.ui.SimpleTextAttributes;
import org.codinjutsu.tools.jenkins.enums.BuildStatusEnum;
import org.codinjutsu.tools.jenkins.model.jenkins.Build;
import org.codinjutsu.tools.jenkins.model.jenkins.Job;
import org.codinjutsu.tools.jenkins.view.ui.BrowserPanel;
import org.jetbrains.annotations.NotNull;

import java.util.Map;
import java.util.Optional;

/**
 * ==========================
 * 开发：maple
 * 创建时间：2024/8/19 17:13
 * 版本：1.0
 * 描述：打开git的changelist后，正在监听的项目变化，显示在changelist上
 * ==========================
 */
public class WatchedJobsDecorator implements ChangeListDecorator {

    private final Project project;

    public WatchedJobsDecorator(Project project) {
        this.project = project;
    }

    @Override
    public void decorateChangeList(@NotNull LocalChangeList changeList,
                                   @NotNull ColoredTreeCellRenderer cellRenderer,
                                   boolean selected, boolean expanded, boolean hasFocus) {
        //添加查看项目变化的代码
        final BrowserPanel browserPanel = BrowserPanel.getInstance(project);
        final Map<String, Job> jobs = browserPanel.getWatched();
        Optional.ofNullable(jobs.get(changeList.getName()))
                .map(Job::getLastBuild)
                .ifPresent(build -> decorateChangeList(build, cellRenderer));
    }

    public void decorateChangeList(@NotNull Build build,
                                   @NotNull ColoredTreeCellRenderer cellRenderer) {
        final BuildStatusEnum status = build.isBuilding() ? BuildStatusEnum.RUNNING : build.getStatus();
        cellRenderer.append(String.format(" - last build %s: %s", build.getDisplayNumber(), status.getStatus()),
                SimpleTextAttributes.GRAYED_ATTRIBUTES);
        cellRenderer.repaint();
    }
}
