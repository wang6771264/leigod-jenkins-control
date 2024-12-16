package org.codinjutsu.tools.jenkins.common;

import com.intellij.openapi.actionSystem.ActionUpdateThread;
import com.intellij.openapi.actionSystem.AnAction;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.project.DumbAware;
import java.util.Optional;
import org.codinjutsu.tools.jenkins.model.jenkins.Build;
import org.codinjutsu.tools.jenkins.model.jenkins.Job;
import org.codinjutsu.tools.jenkins.util.HtmlClipboard;
import org.codinjutsu.tools.jenkins.util.SymbolPool;
import org.codinjutsu.tools.jenkins.view.action.ActionUtil;
import org.codinjutsu.tools.jenkins.view.ui.BrowserPanel;
import org.jetbrains.annotations.NotNull;

/**
 * ==========================
 * 开发：wei.wang
 * 创建时间：2024-08-08 18:13
 * 版本：1.0
 * 描述：复制超链接的action
 * ==========================
 */
public class CopyHyperlinkAction extends AnAction implements DumbAware {

    public static final String ACTION_ID = "Jenkins.CopyHyperlink";

    public CopyHyperlinkAction() {
    }

    @Override
    public void actionPerformed(@NotNull AnActionEvent event) {
        ActionUtil.getProject(event).ifPresent(project -> {
            final BrowserPanel browserPanel = BrowserPanel.getInstance(project);
            Optional<Build> selectedBuild = browserPanel.getSelectedBuild();
            if (selectedBuild.isPresent()) {
                HtmlClipboard.copyHtmlToClipboard(SymbolPool.HASH + selectedBuild.get().getNumber(), selectedBuild.get().getUrl());
            } else {
                Job selectedJob = browserPanel.getSelectedJob();
                if (selectedJob != null && selectedJob.getLastBuild() != null) {
                    Build lastBuild = selectedJob.getLastBuild();
                    HtmlClipboard.copyHtmlToClipboard(SymbolPool.HASH + lastBuild.getNumber(), lastBuild.getUrl());
                } else {
                    event.getPresentation().setVisible(false);
                }
            }
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
}
