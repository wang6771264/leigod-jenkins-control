package org.codinjutsu.tools.jenkins.settings.multiServer;

import com.alibaba.fastjson.JSON;
import com.intellij.openapi.extensions.BaseExtensionPointName;
import com.intellij.openapi.extensions.ExtensionPointName;
import com.intellij.openapi.options.Configurable;
import com.intellij.openapi.options.SearchableConfigurable;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.text.StringUtil;
import org.codinjutsu.tools.jenkins.persistent.JenkinsSettings;
import org.codinjutsu.tools.jenkins.JenkinsWindowManager;
import org.codinjutsu.tools.jenkins.exception.ConfigurationException;
import org.codinjutsu.tools.jenkins.logic.ConfigurationValidator;
import org.codinjutsu.tools.jenkins.logic.LoginService;
import org.codinjutsu.tools.jenkins.logic.RequestManager;
import org.codinjutsu.tools.jenkins.model.jenkins.Jenkins;
import org.codinjutsu.tools.jenkins.security.JenkinsVersion;
import org.codinjutsu.tools.jenkins.settings.ServerSetting;
import org.codinjutsu.tools.jenkins.view.annotation.FormValidator;
import org.codinjutsu.tools.jenkins.view.extension.JobParameterRenderer;
import org.codinjutsu.tools.jenkins.view.extension.ViewTestResults;
import org.codinjutsu.tools.jenkins.view.ui.BrowserPanel;
import org.jetbrains.annotations.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.swing.*;
import java.util.*;

public class MultiServerConfigurable implements SearchableConfigurable, Configurable.WithEpDependencies {

    private static final Logger log = LoggerFactory.getLogger(MultiServerConfigurable.class);
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
        final JenkinsSettings jenkinsSettings = JenkinsSettings.getSafeInstance(project);
        return readSettingFromUi()
                .map(serverSetting -> isModified(serverSetting, jenkinsSettings))
                .orElse(false);
    }

    private @NotNull Optional<MultiServerSettings> readSettingFromUi() {
        return Optional.ofNullable(serverComponent).map(MultiServerSettingComponent::getMultiServerSettings);
    }

    public boolean isModified(MultiServerSettings uiSetting, JenkinsSettings jenkinsSettings) {
        List<MultiJenkinsSettings> newSettings = uiSetting.getSettings();
        List<MultiJenkinsSettings> oldSettings = jenkinsSettings.getMultiSettings();
        //fixme 转成json之后比较看看是否修改过
        return !Objects.equals(JSON.toJSONString(newSettings), JSON.toJSONString(oldSettings));
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
        final JenkinsSettings jenkinsSettings = JenkinsSettings.getSafeInstance(project);
        jenkinsSettings.setMultiSettings(serverSettings.getSettings());
        jenkinsSettings.setConnectionTimeout(serverSettings.getConnectedTimeout());
        //检查ui和持久化的数据是否一致,不一致则保存
        BrowserPanel browserPanel = BrowserPanel.getInstance(project);
        if (browserPanel != null) {
            browserPanel.clearJenkins();
            serverSettings.getSettings().forEach(jenkinsSetting -> {
                Jenkins jenkins = Jenkins.byDefault(jenkinsSetting.getName());
                jenkins.setServerUrl(jenkinsSetting.getJenkinsServer());
                browserPanel.addJenkins(jenkins);
            });
            final LoginService loginService = LoginService.getInstance(project);
            loginService.performAuthentication();
        }
        Optional.ofNullable(serverComponent).ifPresent(MultiServerSettingComponent::resetApiTokenModified);
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

    @Override
    public @NotNull Collection<BaseExtensionPointName<?>> getDependencies() {
        ExtensionPointName<JobParameterRenderer> renderer = JobParameterRenderer.EP_NAME;
        ExtensionPointName<ViewTestResults> viewTestResults = ViewTestResults.EP_NAME;
        List<BaseExtensionPointName<?>> dependencies = new ArrayList<>();
        dependencies.add(renderer);
        dependencies.add(viewTestResults);
        return dependencies;
    }
}
