package org.codinjutsu.tools.jenkins.settings.multiServer;

import com.intellij.openapi.options.SearchableConfigurable;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.text.StringUtil;
import org.codinjutsu.tools.jenkins.JenkinsAppSettings;
import org.codinjutsu.tools.jenkins.JenkinsSettings;
import org.codinjutsu.tools.jenkins.JenkinsWindowManager;
import org.codinjutsu.tools.jenkins.exception.ConfigurationException;
import org.codinjutsu.tools.jenkins.logic.ConfigurationValidator;
import org.codinjutsu.tools.jenkins.logic.RequestManager;
import org.codinjutsu.tools.jenkins.security.JenkinsVersion;
import org.codinjutsu.tools.jenkins.settings.ServerSetting;
import org.codinjutsu.tools.jenkins.view.annotation.FormValidator;
import org.jetbrains.annotations.*;

import javax.swing.*;
import java.util.Optional;

public class MultiServerConfigurable implements SearchableConfigurable {

    private final Project project;
    private @Nullable MultiServerSettingComponent serverComponent;
    private FormValidator<JTextField> formValidator;

    public MultiServerConfigurable(Project project) {
        this.project = project;
    }

    @Nls(capitalization = Nls.Capitalization.Title)
    @Override
    public String getDisplayName() {
        return "MultiServer";
    }

    @Nullable
    @Override
    public JComponent createComponent() {
        final var serverComponentToSet = new MultiServerSettingComponent(this.project, this::testConnection);
        //TODO 表格表单校验
        setServerComponent(serverComponentToSet);
        return serverComponentToSet.getPanel();
    }

    @VisibleForTesting
    void setServerComponent(@Nullable MultiServerSettingComponent serverComponent) {
        this.serverComponent = serverComponent;
    }

    private ConfigurationValidator.@NotNull ValidationResult testConnection(@NotNull ServerSetting serverSetting) {
        final String apiToken = serverSetting.getApiToken();
        final String serverUrl = Optional.ofNullable(serverSetting.getUrl()).orElse("");
        final String configuredJenkinsUrl = Optional.ofNullable(serverSetting.getJenkinsUrl())
                .filter(StringUtil::isNotEmpty)
                .orElse(serverUrl);
        final String jenkinsUrlFromServer = RequestManager.getInstance(project).testAuthenticate(serverUrl,
                serverSetting.getUsername(), apiToken, "", JenkinsVersion.VERSION_2,
                serverSetting.getTimeout());
        if (StringUtil.isEmpty(jenkinsUrlFromServer)) {
            throw new ConfigurationException("Cannot find 'Jenkins URL'. Please check your Jenkins Location");
        }
        return ConfigurationValidator.getInstance(project).validate(configuredJenkinsUrl, jenkinsUrlFromServer);
    }

    @Override
    public String getHelpTopic() {
        return "preferences.jenkins.servers";
    }

    @Override
    public boolean isModified() {
        final var jenkinsAppSettings = JenkinsAppSettings.getSafeInstance(project);
        final var jenkinsSettings = JenkinsSettings.getSafeInstance(project);
        return readSettingFromUi()
                .map(serverSetting -> isModified(serverSetting, jenkinsAppSettings, jenkinsSettings))
                .orElse(false);
    }

    private @NotNull Optional<MultiServerSettings> readSettingFromUi() {
        return Optional.ofNullable(serverComponent).map(MultiServerSettingComponent::getMultiServerSettings);
    }

    public static boolean isModified(MultiServerSettings serverSetting, JenkinsAppSettings appSettings,
                                     JenkinsSettings jenkinsSettings) {
        // TODO 判断表格是否修改过
        return false;
    }

    @Override
    public void apply() throws ConfigurationException {
        try {
            Optional.ofNullable(formValidator).ifPresent(FormValidator::validate);
        } catch (org.codinjutsu.tools.jenkins.exception.ConfigurationException ex) {
            throw new ConfigurationException(ex.getMessage());
        }
        readSettingFromUi().ifPresent(this::apply);
        JenkinsWindowManager.getInstance(project).ifPresent(JenkinsWindowManager::reloadConfiguration);
    }

    private void apply(MultiServerSettings serverSettings) throws ConfigurationException {
        final JenkinsAppSettings appSettings = JenkinsAppSettings.getSafeInstance(project);
        final JenkinsSettings jenkinsSettings = JenkinsSettings.getSafeInstance(project);
        //TODO 检查ui和持久化的数据是否一致,不一致则保存

        jenkinsSettings.setMultiSettings(serverSettings.getSettings());
        jenkinsSettings.setConnectionTimeout(serverSettings.getConnectedTimeout());
    }

    @Override
    public void reset() {
        Optional.ofNullable(serverComponent).ifPresent(this::reset);
    }

    private void reset(MultiServerSettingComponent serverComponentToReset) {
        // TODO 填充表格
    }

    @Override
    public void disposeUIResources() {
        serverComponent = null;
        formValidator = null;
    }

    @Override
    public @NotNull @NonNls String getId() {
        return "org.codinjutsu.tools.jenkins.multiServers";
    }

}
