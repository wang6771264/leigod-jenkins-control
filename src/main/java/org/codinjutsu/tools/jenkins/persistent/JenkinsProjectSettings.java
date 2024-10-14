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

import com.intellij.openapi.components.PersistentStateComponent;
import com.intellij.openapi.components.RoamingType;
import com.intellij.openapi.components.State;
import com.intellij.openapi.components.Storage;
import com.intellij.openapi.project.Project;
import com.intellij.util.xmlb.XmlSerializerUtil;
import lombok.Data;
import org.codinjutsu.tools.jenkins.model.FavoriteJob;
import org.codinjutsu.tools.jenkins.model.jenkins.Job;
import org.codinjutsu.tools.jenkins.util.JobUtil;
import org.jetbrains.annotations.NotNull;

import java.util.LinkedList;
import java.util.List;
import java.util.function.Predicate;

@State(name = "Jenkins.Settings", storages = {
        @Storage(value = "jenkins_project_setting.xml", roamingType = RoamingType.PER_OS)
})
public class JenkinsProjectSettings implements PersistentStateComponent<JenkinsProjectSettings.State> {

    private final State myState = new State();

    public static JenkinsProjectSettings getSafeInstance(Project project) {
        JenkinsProjectSettings settings = project.getService(JenkinsProjectSettings.class);
        return settings != null ? settings : new JenkinsProjectSettings();
    }

    @Override
    public State getState() {
        return myState;
    }

    @Override
    public void loadState(@NotNull State state) {
        XmlSerializerUtil.copyBean(state, myState);
    }

    public void setProjectJobs(List<FavoriteJob> projectJobs) {
        myState.setProjectJobs(projectJobs);
    }

    public void addProjectJob(@NotNull List<Job> jobs) {
        jobs.stream().map(JobUtil::createFavoriteJob).forEach(myState::addProjectJobs);
    }

    public boolean isFavoriteJob(@NotNull Job job) {
        return myState.getProjectJobs().stream().anyMatch(favoriteJob -> JobUtil.isFavoriteJob(job, favoriteJob));
    }

    public void removeProjectJob(@NotNull List<Job> selectedJobs) {
        selectedJobs.forEach(jobToRemove -> myState.removeProjectJob(favoriteJob -> JobUtil.isFavoriteJob(jobToRemove, favoriteJob)));
    }

    public List<FavoriteJob> getProjectJobs() {
        return myState.getProjectJobs();
    }

    @Data
    public static class State {
        private List<FavoriteJob> projectJobs = new LinkedList<>();

        public void clearProjectJobs() {
            projectJobs.clear();
        }

        public void addProjectJobs(FavoriteJob favoriteJob) {
            projectJobs.add(favoriteJob);
        }

        public void removeProjectJob(Predicate<? super FavoriteJob> filter) {
            projectJobs.removeIf(filter);
        }
    }
}
