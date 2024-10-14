package org.codinjutsu.tools.jenkins.logic;

import com.offbytwo.jenkins.model.TestResult;
import org.codinjutsu.tools.jenkins.persistent.JenkinsAppSettings;
import org.codinjutsu.tools.jenkins.persistent.JenkinsSettings;
import org.codinjutsu.tools.jenkins.enums.BuildTypeEnum;
import org.codinjutsu.tools.jenkins.model.FavoriteJob;
import org.codinjutsu.tools.jenkins.model.jenkins.*;
import org.codinjutsu.tools.jenkins.security.JenkinsVersion;
import org.jetbrains.annotations.NotNull;

import java.util.List;
import java.util.Map;

public interface RequestManagerInterface {
    List<Jenkins> loadJenkinsWorkspace(JenkinsAppSettings configuration, JenkinsSettings jenkinsSettings);

    Map<String, Build> loadJenkinsRssLatestBuilds(JenkinsSettings jenkinsSettings);

    void runBuild(Job job, JenkinsAppSettings configuration, Map<String, ?> parameters);

    void authenticate(JenkinsAppSettings jenkinsAppSettings, JenkinsSettings jenkinsSettings);

    String testAuthenticate(String serverUrl, String username, String password, String crumbData, JenkinsVersion version, int connectionTimoutInSeconds);

    List<Job> loadFavoriteJobs(List<FavoriteJob> favoriteJobs);

    void stopBuild(Build build);

    @NotNull
    Job loadJob(Job job);

    @NotNull
    List<Job> loadJenkinsView(@NotNull ViewV2 view);

    @NotNull
    Build loadBuild(Build build);

    List<Build> loadBuilds(Job job);

    void loadConsoleTextFor(Job job, BuildTypeEnum buildTypeEnum,
                            RequestManager.BuildLogConsoleStreamListener buildConsoleStreamListener);

    void loadConsoleTextFor(Build build,
                            RequestManager.BuildLogConsoleStreamListener buildConsoleStreamListener);

    List<TestResult> loadTestResultsFor(Job job);

    List<TestResult> loadTestResultsFor(Build build);

    @NotNull
    List<Computer> loadComputer(JenkinsSettings settings);

    List<String> getGitParameterChoices(Job job, JobParameter jobParameter);
}
