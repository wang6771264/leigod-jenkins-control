package org.codinjutsu.tools.jenkins.common;

import com.intellij.ide.actions.CopyAction;
import com.intellij.openapi.actionSystem.AnActionEvent;
import org.codinjutsu.tools.jenkins.example.CopyHyperLinkHelper;
import org.codinjutsu.tools.jenkins.util.SymbolPool;
import org.codinjutsu.tools.jenkins.view.ui.BrowserPanel;
import org.jetbrains.annotations.NotNull;

/**
 * ==========================
 * 开发：wei.wang
 * 创建时间：2024-08-08 18:13
 * 版本：1.0
 * 描述：
 * ==========================
 */
public class CopyBuildNumberAction extends CopyAction {

    final BrowserPanel browserPanel;

    public CopyBuildNumberAction(BrowserPanel browserPanel) {
        this.browserPanel = browserPanel;
    }

    @Override
    public void actionPerformed(@NotNull AnActionEvent e) {
        browserPanel.getSelectedBuild().ifPresent(build ->
                CopyHyperLinkHelper.copy(build.getUrl(), SymbolPool.HASH + build.getNumber()));
    }

}
