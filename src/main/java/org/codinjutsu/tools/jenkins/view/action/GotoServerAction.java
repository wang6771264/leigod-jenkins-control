package org.codinjutsu.tools.jenkins.view.action;

import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.util.text.StringUtil;
import org.codinjutsu.tools.jenkins.JenkinsAppSettings;
import org.codinjutsu.tools.jenkins.model.jenkins.Jenkins;
import org.codinjutsu.tools.jenkins.view.ui.BrowserPanel;
import org.jetbrains.annotations.NotNull;

import java.util.Optional;

public class GotoServerAction extends AbstractGotoWebPageAction {

    public GotoServerAction(BrowserPanel browserPanel) {
        super("Go to server page", "Open the jenkins server in a web browser", browserPanel);
    }


    @NotNull
    @Override
    protected String getUrl() {
        return browserPanel.getSelectedServer().map(Jenkins::getServerUrl).orElse("");
    }

    @Override
    public void update(AnActionEvent event) {
        Optional<Jenkins> selectedServer = browserPanel.getSelectedServer();
        if(selectedServer.isPresent()){
            Jenkins jenkins = selectedServer.get();
            event.getPresentation().setVisible(!StringUtil.equals(jenkins.getServerUrl(),
                    JenkinsAppSettings.DUMMY_JENKINS_SERVER_URL));
            return;
        }
        event.getPresentation().setVisible(false);
    }
}
