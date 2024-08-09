package org.codinjutsu.tools.jenkins.task.callback.impl;

import com.intellij.openapi.diagnostic.Logger;
import lombok.Data;
import org.codinjutsu.tools.jenkins.enums.BuildTypeEnum;
import org.codinjutsu.tools.jenkins.exception.AuthenticationException;
import org.codinjutsu.tools.jenkins.model.jenkins.Job;
import org.codinjutsu.tools.jenkins.task.callback.RunBuildCallback;
import org.codinjutsu.tools.jenkins.view.action.LogToolWindow;
import org.codinjutsu.tools.jenkins.view.action.RunBuildAction;
import org.codinjutsu.tools.jenkins.view.ui.BrowserPanel;
import org.jetbrains.annotations.NotNull;

@Data
public class RunBuildCallbacker implements RunBuildCallback {

    private static final Logger logger = Logger.getInstance(RunBuildAction.class.getName());

    @NotNull
    private BrowserPanel browserPanel;

    @Override
    public void notifyOnOk(Job job) {
        browserPanel.notifyInfoJenkinsToolWindow(job.getNameToRenderSingleJob() + " build is on going",
                job.getUrl());
        browserPanel.loadJob(job);
        final LogToolWindow logToolWindow = new LogToolWindow(browserPanel.getProject());
        logToolWindow.showLog(BuildTypeEnum.LAST, job);
    }

    @Override
    public void notifyOnError(Job job, Throwable ex) {
        if (ex instanceof AuthenticationException) {
            logger.debug(((AuthenticationException) ex).getResponseBody(), ex);
        }
        browserPanel.notifyErrorJenkinsToolWindow("Build '" + job.getNameToRenderSingleJob() + "' cannot be run: " + ex.getMessage());
        browserPanel.loadJob(job);
    }
}