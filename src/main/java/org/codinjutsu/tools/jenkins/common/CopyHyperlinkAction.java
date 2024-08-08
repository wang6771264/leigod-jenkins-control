package org.codinjutsu.tools.jenkins.common;

import com.intellij.openapi.actionSystem.AnAction;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.actionSystem.KeyboardShortcut;
import com.intellij.openapi.actionSystem.Shortcut;
import com.intellij.openapi.keymap.Keymap;
import com.intellij.openapi.keymap.KeymapManager;
import com.intellij.openapi.project.DumbAware;
import org.codinjutsu.tools.jenkins.example.CopyHyperLinkHelper;
import org.codinjutsu.tools.jenkins.util.SymbolPool;
import org.codinjutsu.tools.jenkins.view.action.ActionUtil;
import org.codinjutsu.tools.jenkins.view.ui.BrowserPanel;
import org.jetbrains.annotations.NotNull;

import javax.swing.*;
import java.awt.*;
import java.awt.event.InputEvent;
import java.awt.event.KeyEvent;

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
//        //注册快捷键
//        this.registerShortcutForAction();
    }

    @Override
    public void actionPerformed(@NotNull AnActionEvent event) {
        ActionUtil.getProject(event).ifPresent(project -> {
            final BrowserPanel browserPanel = BrowserPanel.getInstance(project);
            browserPanel.getSelectedBuild().ifPresent(build ->
                    CopyHyperLinkHelper.copy(build.getUrl(), SymbolPool.HASH + build.getNumber()));
        });
    }

    public void registerShortcutForAction() {
        // 获取当前活动的键位映射
        Keymap activeKeymap = KeymapManager.getInstance().getActiveKeymap();

        // 定义您想要的快捷键，例如 Ctrl+C
        KeyStroke keyStroke = KeyStroke.getKeyStroke(KeyEvent.VK_C,
                Toolkit.getDefaultToolkit().getMenuShortcutKeyMaskEx() | InputEvent.SHIFT_DOWN_MASK);

        // 创建一个新的快捷键
        Shortcut newShortcut = new KeyboardShortcut(keyStroke, null);

        // 为动作添加快捷键
        activeKeymap.addShortcut(ACTION_ID, newShortcut);

        // 如果需要，可以更新IDE的键位映射
        KeymapManager.getInstance().getActiveKeymap().addShortcut(ACTION_ID, newShortcut);
    }

    public void unregisterShortcutForAction() {
        // 获取当前活动的键位映射
        Keymap activeKeymap = KeymapManager.getInstance().getActiveKeymap();
        // 移除动作的快捷键
        activeKeymap.removeAllActionShortcuts("myplugin.myCustomAction");
    }
}
