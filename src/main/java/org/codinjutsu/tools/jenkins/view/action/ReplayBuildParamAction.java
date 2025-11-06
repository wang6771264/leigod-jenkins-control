package org.codinjutsu.tools.jenkins.view.action;

import com.intellij.openapi.actionSystem.AnAction;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.project.DumbAware;
import com.intellij.openapi.project.Project;
import java.util.Optional;
import org.codinjutsu.tools.jenkins.view.JenkinsTreeRenderer;
import org.codinjutsu.tools.jenkins.view.ui.BrowserPanel;
import org.jetbrains.annotations.NotNull;

/**
 * build历史记录重放
 */
public class ReplayBuildParamAction extends AnAction implements DumbAware {

    private static final Logger LOG = Logger.getInstance(ReplayBuildParamAction.class.getName());
    private final BrowserPanel browserPanel;
    
    public ReplayBuildParamAction(BrowserPanel browserPanel) {
        super("Replay Build History", "Replay Build History", JenkinsTreeRenderer.PROJECT_ICON);
        this.browserPanel = browserPanel;
    }
    
    @Override 
    public void actionPerformed(@NotNull AnActionEvent event) {
        ActionUtil.getProject(event).ifPresent(this::actionPerformed);
    }

    private void actionPerformed(@NotNull Project project) {
        final BrowserPanel browserPanel = BrowserPanel.getInstance(project);
        try {
            Optional.ofNullable(browserPanel.getSelectedBuildHistory())
                    .ifPresent(job -> RunBuildAction.queueRunBuild(project, browserPanel, job));
        } catch (Exception ex) {
            final String message = ex.getMessage() == null ? "Unknown error" : ex.getMessage();
            LOG.error(message, ex);
            browserPanel.notifyErrorJenkinsToolWindow("Reuse Build cannot be run: " + message);
        }
    }
}
