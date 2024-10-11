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

package org.codinjutsu.tools.jenkins.model.jenkins;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

public class MultiJenkins {

    /**
     * jenkins的配置
     */
    private final List<Jenkins> jenkinss;

    private Map<String, Job> jobMap;

    public MultiJenkins(List<Jenkins> jenkinss) {
        this.jenkinss = jenkinss;
    }

    public List<Job> getJobs() {
        if (this.jenkinss == null) {
            return Collections.emptyList();
        }
        return this.jenkinss.stream().map(Jenkins::getJobs).flatMap(List::stream).toList();
    }

    public ViewV2 getViewByName(String lastSelectedViewName) {
        if (this.jenkinss == null || this.jenkinss.isEmpty()) {
            return null;
        }
        return this.jenkinss.stream().map(Jenkins::getViews).flatMap(List::stream)
                .filter(view -> view.getName().equals(lastSelectedViewName))
                .findFirst().orElse(null);
    }

    public void update(Jenkins workspace) {
        jenkinss.forEach(jenkins -> {
            if (Objects.equals(jenkins.getName(), workspace.getName())) {
                jenkins.update(workspace);
            }
        });
    }

    public void setJobs(List<Job> jobList) {
        Map<String, List<Job>> jenkinsJobMap = jobList.stream().collect(Collectors.groupingBy(job -> {
            //服务链接
            return this.jenkinss.stream()
                    .filter(jenkins -> job.getUrl().startsWith(jenkins.getServerUrl()))
                    .findFirst().map(Jenkins::getServerUrl).orElse("");
        }));
        for (Jenkins jenkins : this.jenkinss) {
            List<Job> jobs = jenkinsJobMap.get(jenkins.getServerUrl());
            if (jobs != null && !jobs.isEmpty()) {
                jenkins.setJobs(jobs);
            }
        }
    }

    public ViewV2 getPrimaryView() {
        for (Jenkins jenkins : jenkinss) {
            return jenkins.getPrimaryView();
        }
        return null;
    }

    public List<ViewV2> getViews() {
        return jenkinss.stream().map(Jenkins::getViews).flatMap(List::stream).toList();
    }
}
