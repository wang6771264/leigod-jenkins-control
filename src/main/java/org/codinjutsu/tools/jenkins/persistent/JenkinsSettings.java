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

package org.codinjutsu.tools.jenkins.persistent;

import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.components.PersistentStateComponent;
import com.intellij.openapi.components.RoamingType;
import com.intellij.openapi.components.State;
import com.intellij.openapi.components.Storage;
import com.intellij.openapi.project.Project;
import com.intellij.util.xmlb.XmlSerializerUtil;
import org.apache.commons.lang.StringUtils;
import org.codinjutsu.tools.jenkins.model.FavoriteJob;
import org.codinjutsu.tools.jenkins.model.jenkins.Job;
import org.codinjutsu.tools.jenkins.security.JenkinsVersion;
import org.codinjutsu.tools.jenkins.settings.multiServer.MultiJenkinsSettings;
import org.codinjutsu.tools.jenkins.state.ProjectState;
import org.codinjutsu.tools.jenkins.util.JobUtil;
import org.jetbrains.annotations.NotNull;

import java.util.HashMap;
import java.util.List;
import java.util.Optional;

@State(name = "Jenkins.Settings", storages = {
        @Storage(value = "jenkins_share_setting.xml", roamingType = RoamingType.PER_OS)
})
public class JenkinsSettings implements PersistentStateComponent<ProjectState> {

    private final ProjectState myState = new ProjectState();

    public static JenkinsSettings getSafeInstance(Project project) {
        JenkinsSettings settings = ApplicationManager.getApplication()
                .getService(JenkinsSettings.class);
        return settings != null ? settings : new JenkinsSettings();
    }

    @Override
    public ProjectState getState() {
        return myState;
    }

    @Override
    public void loadState(@NotNull ProjectState state) {
        XmlSerializerUtil.copyBean(state, myState);
    }

    public void addFavorite(@NotNull List<Job> jobs) {
        jobs.stream().map(JobUtil::createFavoriteJob).forEach(myState::addFavoriteJobs);
    }

    public boolean isFavoriteJob(@NotNull Job job) {
        return myState.getFavoriteJobs().stream().anyMatch(favoriteJob -> JobUtil.isFavoriteJob(job, favoriteJob));
    }

    public void removeFavorite(@NotNull List<Job> selectedJobs) {
        selectedJobs.forEach(jobToRemove -> myState.removeFavoriteJob(favoriteJob -> JobUtil.isFavoriteJob(jobToRemove, favoriteJob)));
    }

    @NotNull
    public List<FavoriteJob> getFavoriteJobs() {
        return myState.getFavoriteJobs();
    }

    public boolean isFavoriteViewEmpty() {
        return myState.getFavoriteJobs().isEmpty();
    }

    public String getLastSelectedView() {
        return myState.getLastSelectedView();
    }

    public void setLastSelectedView(String viewName) {
        myState.setLastSelectedView(viewName);
    }

    public JenkinsVersion getVersion() {
        return this.myState.getJenkinsVersion();
    }

    public void setVersion(JenkinsVersion jenkinsVersion) {
        this.myState.setJenkinsVersion(jenkinsVersion);
    }

    public void clearFavoriteJobs() {
        myState.clearFavoriteJobs();
    }

    public boolean hasFavoriteJobs() {
        return !myState.getFavoriteJobs().isEmpty();
    }

    public int getConnectionTimeout() {
        return myState.getConnectionTimeout();
    }

    public void setConnectionTimeout(int timeoutInSeconds) {
        myState.setConnectionTimeout(timeoutInSeconds);
    }

    public void setMultiSettings(List<MultiJenkinsSettings> list) {
        myState.setMultiSettings(list);
    }

    public List<MultiJenkinsSettings> getMultiSettings() {
        return myState.getMultiSettings();
    }

    @SuppressWarnings("unchecked")
    public void setServerTableStyle(Object serverTableStyle) {
        if (serverTableStyle != null) {
            myState.setServerTableStyle((HashMap<String, Integer>) serverTableStyle);
        }
    }

    public Integer getServerTableColumnWidth(String columnName) {
        return Optional.ofNullable(myState.getServerTableStyle())
                .map(o -> o.get(columnName)).orElse(null);
    }

    /**
     * 表格样式是否持久化
     *
     * @return
     */
    public boolean hasPersistentTableStyle() {
        return myState.getServerTableStyle() != null && !myState.getServerTableStyle().isEmpty();
    }

    /**
     * 是否有连接需要处理
     *
     * @return
     */
    public boolean isServerUrlSet() {
        List<MultiJenkinsSettings> multiSettings = this.getMultiSettings();
        return multiSettings.stream().anyMatch(settings -> StringUtils.isNotBlank(settings.getJenkinsUrl()));
    }
}
