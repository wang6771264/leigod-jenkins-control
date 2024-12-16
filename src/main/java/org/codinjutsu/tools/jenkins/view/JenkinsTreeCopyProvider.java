package org.codinjutsu.tools.jenkins.view;

import com.intellij.ide.CopyProvider;
import com.intellij.ide.TextCopyProvider;
import com.intellij.openapi.actionSystem.ActionUpdateThread;
import com.intellij.openapi.actionSystem.DataContext;
import com.intellij.openapi.project.Project;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.Value;
import lombok.experimental.Delegate;
import org.codinjutsu.tools.jenkins.JenkinsTree;
import org.codinjutsu.tools.jenkins.model.jenkins.Build;
import org.codinjutsu.tools.jenkins.model.jenkins.Job;
import org.codinjutsu.tools.jenkins.util.CopyHyperLinkHelper;
import org.codinjutsu.tools.jenkins.util.HtmlClipboard;
import org.codinjutsu.tools.jenkins.util.SymbolPool;
import org.codinjutsu.tools.jenkins.view.ui.BrowserPanel;
import org.jetbrains.annotations.NotNull;

import javax.swing.tree.DefaultMutableTreeNode;
import java.util.Collection;
import java.util.Collections;
import java.util.stream.Collectors;

@Value
@Getter(AccessLevel.NONE)
public class JenkinsTreeCopyProvider implements CopyProvider {
    Project project;
    JenkinsTree tree;
    @Delegate
    CopyProvider textCopyProvider = new TextCopyProvider() {
        @Override
        public @NotNull Collection<String> getTextLinesToCopy() {
            Job selectedJob = BrowserPanel.getInstance(project).getSelectedJob();
            if (selectedJob != null && selectedJob.getLastBuild() != null) {
                Build lastBuild = selectedJob.getLastBuild();
                CopyHyperLinkHelper.copy(lastBuild.getUrl(), SymbolPool.HASH + lastBuild.getNumber());
                HtmlClipboard.copyHtmlToClipboard(SymbolPool.HASH + lastBuild.getNumber(), lastBuild.getUrl());
            }
//            return JenkinsTreeCopyProvider.this.getTextLinesToCopy();
            return Collections.emptyList();
        }

        @Override
        public boolean isCopyEnabled(@NotNull DataContext dataContext) {
//            final var textLinesToCopy = this.getTextLinesToCopy();
//            return !textLinesToCopy.isEmpty();
            return true;
        }

        @Override
        public @NotNull ActionUpdateThread getActionUpdateThread() {
            return ActionUpdateThread.EDT;
        }
    };

    @NotNull
    Collection<String> getTextLinesToCopy() {
        return tree.getSelectedPathComponents()
                .map(DefaultMutableTreeNode::getUserObject)
                .filter(CopyTextProvider.class::isInstance)
                .map(CopyTextProvider.class::cast)
                .map(CopyTextProvider::getTextLinesToCopy)
                .flatMap(Collection::stream)
                .collect(Collectors.toList());
    }

    public JenkinsTreeCopyProvider(Project project, JenkinsTree tree) {
        this.project = project;
        this.tree = tree;
    }

    public JenkinsTreeCopyProvider(JenkinsTree tree) {
        this(null, tree);
    }
}
