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

import com.intellij.openapi.util.text.StringUtil;
import lombok.Getter;
import lombok.Setter;
import org.codinjutsu.tools.jenkins.JenkinsAppSettings;
import org.jetbrains.annotations.Nullable;

import java.util.LinkedList;
import java.util.List;

@Setter
public class Jenkins {

    private @Nullable String name;
    @Getter
    private String serverUrl;

    @Getter
    private List<Job> jobs;

    @Getter
    private List<ViewV2> views;
    @Getter
    private ViewV2 primaryView;

    public Jenkins(@Nullable String description, String serverUrl) {
        this.name = description;
        this.serverUrl = serverUrl;
        this.jobs = new LinkedList<>();
    }

    public @Nullable String getName() {
        return name;
    }

    public void update(Jenkins jenkins) {
        this.name = jenkins.getName();
        this.serverUrl = jenkins.getServerUrl();
        this.jobs.clear();
        this.jobs.addAll(jenkins.getJobs());

        this.views = jenkins.getViews();
        this.primaryView = jenkins.getPrimaryView();
    }

    public static Jenkins byDefault(String name) {
        return new Jenkins(name, JenkinsAppSettings.DUMMY_JENKINS_SERVER_URL);
    }

    public String getNameToRender() {
        final var description = getName();
        final var label = new StringBuilder("Jenkins");
        if (StringUtil.isNotEmpty(description)) {
            label.append(' ');
            label.append(description);
        }
        return label.toString();
    }

    public void addJob(Job job) {
        if (this.jobs == null) {
            this.jobs = new LinkedList<>();
        }
        this.jobs.add(job);
    }
}
