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

package org.codinjutsu.tools.jenkins.logic;

import com.alibaba.fastjson.JSON;
import com.intellij.openapi.Disposable;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.progress.ProgressManager;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.vfs.VirtualFile;
import com.offbytwo.jenkins.JenkinsServer;
import com.offbytwo.jenkins.helper.BuildConsoleStreamListener;
import com.offbytwo.jenkins.model.*;
import org.apache.commons.lang.StringUtils;
import org.codinjutsu.tools.jenkins.cache.JobCache;
import org.codinjutsu.tools.jenkins.enums.BuildStatusEnum;
import org.codinjutsu.tools.jenkins.enums.BuildTypeEnum;
import org.codinjutsu.tools.jenkins.exception.JenkinsPluginRuntimeException;
import org.codinjutsu.tools.jenkins.exception.NoBuildFoundException;
import org.codinjutsu.tools.jenkins.exception.NoJobFoundException;
import org.codinjutsu.tools.jenkins.exception.RunBuildError;
import org.codinjutsu.tools.jenkins.model.FavoriteJob;
import org.codinjutsu.tools.jenkins.model.jenkins.Build;
import org.codinjutsu.tools.jenkins.model.jenkins.Computer;
import org.codinjutsu.tools.jenkins.model.jenkins.Job;
import org.codinjutsu.tools.jenkins.model.jenkins.*;
import org.codinjutsu.tools.jenkins.persistent.JenkinsAppSettings;
import org.codinjutsu.tools.jenkins.persistent.JenkinsSettings;
import org.codinjutsu.tools.jenkins.security.JenkinsSecurityClient;
import org.codinjutsu.tools.jenkins.security.JenkinsVersion;
import org.codinjutsu.tools.jenkins.security.Response;
import org.codinjutsu.tools.jenkins.security.SecurityClientFactory;
import org.codinjutsu.tools.jenkins.settings.multiServer.MultiJenkinsSettings;
import org.codinjutsu.tools.jenkins.util.SymbolPool;
import org.codinjutsu.tools.jenkins.view.parameter.renderer.NodeParameterRenderer;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.VisibleForTesting;

import javax.swing.*;
import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Function;
import java.util.function.Supplier;
import java.util.function.UnaryOperator;
import java.util.stream.Collectors;

public class RequestManager implements RequestManagerInterface, Disposable {

    private static final Logger logger = Logger.getInstance(RequestManager.class);

    private static final String BUILDHIVE_CLOUDBEES = "buildhive";

    private final Project project;

    private final UrlBuilder urlBuilder;
    private final RssParser rssParser = new RssParser();
    /**
     * 客户端缓存
     */
    private final Map<String, JenkinsClient> jenkinsClients = new LinkedHashMap<>();

    public RequestManager(Project project) {
        this.project = project;
        this.urlBuilder = UrlBuilder.getInstance(project);
    }

    public static RequestManager getInstance(Project project) {
        return Optional.ofNullable(project.getService(RequestManager.class)).orElseGet(() -> new RequestManager(project));
    }

    private static boolean canContainNestedJobs(@NotNull Job job) {
        return job.getJobTypeEnum().containNestedJobs();
    }

    @Override
    public List<Jenkins> loadJenkinsWorkspace(JenkinsAppSettings configuration, JenkinsSettings jenkinsSettings) {
        if (handleNotYetLoggedInState()) return null;
        final List<Jenkins> jenkinsList = new ArrayList<>();
        jenkinsClients.forEach((server, client) -> {
            URL url = urlBuilder.createJenkinsWorkspaceUrl(server);
            JenkinsSecurityClient jenkinsSecurityClient = client.getJenkinsSecurityClient();
            String jsonData = jenkinsSecurityClient.executeForJson(url);
            JenkinsPlateform jenkinsPlateform;
            if (server.contains(BUILDHIVE_CLOUDBEES)) {
                jenkinsPlateform = JenkinsPlateform.CLOUDBEES;
            } else {
                jenkinsPlateform = JenkinsPlateform.CLASSIC;
            }
            client.setJenkinsPlateform(jenkinsPlateform);
            final Jenkins jenkins = client.getJenkinsParser().createWorkspace(client, jsonData);
            final var validationResult = ConfigurationValidator.getInstance(project).validate(server, jenkins.getServerUrl());
            if (!validationResult.isValid()) {
                //TODO 连接校验失败处理
                return;
            }
            jenkins.setName(client.getName());
            jenkinsList.add(jenkins);
        });
        return jenkinsList;
    }

    /**
     * Note! needs to be called after plugin is logged in
     */
    @Override
    public Map<String, Build> loadJenkinsRssLatestBuilds(JenkinsSettings jenkinsSettings) {
        if (handleNotYetLoggedInState()) return Collections.emptyMap();
        Map<String, Build> latestBuilds = new LinkedHashMap<>();
        jenkinsClients.forEach((server, client) -> {
            URL url = urlBuilder.createRssLatestUrl(server);
            //加载最近的更新
            String rssData = client.getJenkinsSecurityClient().executeForJson(url);
            Map<String, Build> buildMap = rssParser.loadJenkinsRssLatestBuilds(rssData);
            latestBuilds.putAll(buildMap);
        });

        return latestBuilds;
    }

    private List<Job> loadJenkinsView(String viewUrl) {
        if (handleNotYetLoggedInState()) return Collections.emptyList();
        JenkinsClient client = this.getClientByUrl(viewUrl);
        JenkinsPlateform jenkinsPlateform = client.getJenkinsPlateform();
        URL url = urlBuilder.createViewUrl(jenkinsPlateform, viewUrl);
        String jenkinsViewData = client.getJenkinsSecurityClient().executeForJson(url);
        final List<Job> jobsFromView;
        final JenkinsParser jenkinsParser = client.getJenkinsParser();
        if (jenkinsPlateform.equals(JenkinsPlateform.CLASSIC)) {
            jobsFromView = jenkinsParser.createJobs(jenkinsViewData);
        } else {
            jobsFromView = jenkinsParser.createCloudbeesViewJobs(jenkinsViewData);
        }
        return withNestedJobs(jobsFromView);
    }

    @NotNull
    private List<Job> withNestedJobs(@NotNull List<Job> jobs) {
        final List<Job> jobWithNested = new ArrayList<>();
        for (Job job : jobs) {
            if (canContainNestedJobs(job)) {
                jobWithNested.add(withNestedJobs(job));
            } else {
                jobWithNested.add(job);
            }
        }
        return withNodeParameterFix(jobWithNested);
    }

    /**
     * @deprecated 2020-05-26 remove if NodeParameter implement choices API
     */
    @Deprecated
    @NotNull
    private List<Job> withNodeParameterFix(@NotNull List<Job> jobs) {
        final AtomicReference<List<Computer>> computers = new AtomicReference<>();
        final Supplier<Collection<Computer>> getOrLoad = () -> {
            List<Computer> cachedComputers = computers.get();
            if (cachedComputers == null) {
                cachedComputers = loadComputer(JenkinsSettings.getSafeInstance(project));
                computers.set(cachedComputers);
            }
            return cachedComputers;
        };
        // Suppliers.memoize(() currently mot working
        return jobs.stream().map(job -> withNodeParameterFix(job, getOrLoad)).collect(Collectors.toList());
    }

    /**
     * @deprecated 2020-05-26 remove if NodeParameter implement choices API
     */
    @Deprecated
    @NotNull
    private Job withNodeParameterFix(@NotNull Job job, @NotNull Supplier<Collection<Computer>> computers) {
        final boolean fixJob = job.getParameters().stream().map(JobParameter::getJobParameterType).anyMatch(NodeParameterRenderer.NODE_PARAMETER::equals);
        if (fixJob) {
            final Job.JobBuilder fixedJob = job.toBuilder();
            fixedJob.clearParameters();
            for (JobParameter jobParameter : job.getParameters()) {
                if (NodeParameterRenderer.NODE_PARAMETER.equals(jobParameter.getJobParameterType())) {
                    final JobParameter.JobParameterBuilder fixedJobParameter = jobParameter.toBuilder();
                    final List<String> computerNames = computers.get().stream().map(Computer::getDisplayName).collect(Collectors.toList());
                    fixedJobParameter.choices(computerNames);
                    fixedJob.parameter(fixedJobParameter.build());
                } else {
                    fixedJob.parameter(jobParameter);
                }
            }
            return fixedJob.build();
        }

        return job;
    }

    @NotNull
    private Job withNestedJobs(@NotNull Job job) {
        job.setNestedJobs(withNestedJobs(loadNestedJobs(job.getUrl())));
        return job;
    }

    private List<Job> loadNestedJobs(String currentJobUrl) {
        if (handleNotYetLoggedInState()) return Collections.emptyList();
        URL url = urlBuilder.createNestedJobUrl(currentJobUrl);
        JenkinsClient client = this.getClientByUrl(currentJobUrl);
        JenkinsSecurityClient securityClient = client.getJenkinsSecurityClient();
        return client.getJenkinsParser().createJobs(securityClient.executeForJson(url));
    }

    private boolean handleNotYetLoggedInState() {
        boolean threadStack = false;
        boolean result = false;
        if (SwingUtilities.isEventDispatchThread()) {
            logger.warn("RequestManager.handleNotYetLoggedInState called from EDT");
            threadStack = true;
        }

        if (jenkinsClients.isEmpty()) {
            logger.warn("Not yet logged in, all calls until login will fail");
            threadStack = true;
            result = true;
        }
        if (threadStack) Thread.dumpStack();
        return result;
    }

    @NotNull
    private Job loadJob(String jenkinsJobUrl) {
        //加载job详情
        JenkinsClient client = this.getClientByUrl(jenkinsJobUrl);
        JenkinsSecurityClient securityClient = client.getJenkinsSecurityClient();
        if (handleNotYetLoggedInState()) return createEmptyJob(jenkinsJobUrl);
        URL url = urlBuilder.createJobUrl(jenkinsJobUrl);
        String jenkinsJobData = securityClient.executeForJson(url);
        Job job = client.getJenkinsParser().createJob(jenkinsJobData);
        //添加到缓存
        JobCache.putIfAbsent(job.getFullName(), job);
        return job;
    }

    @NotNull
    private Job createEmptyJob(String jenkinsJobUrl) {
        return Job.builder().name("").buildable(false).fullName("").url(jenkinsJobUrl).parameters(Collections.emptyList()).inQueue(false).build();
    }

    private void stopBuild(String jenkinsBuildUrl) {
        if (handleNotYetLoggedInState()) return;
        URL url = urlBuilder.createStopBuildUrl(jenkinsBuildUrl);
        JenkinsSecurityClient securityClient = this.getSecurityClientByUrl(jenkinsBuildUrl);
        securityClient.executeForJson(url);
    }

    @VisibleForTesting
    @NotNull
    Build loadBuild(String jenkinsBuildUrl) {
        if (handleNotYetLoggedInState()) return Build.NULL;
        URL url = urlBuilder.createBuildUrl(jenkinsBuildUrl);
        JenkinsClient client = this.getClientByUrl(jenkinsBuildUrl);
        JenkinsSecurityClient securityClient = client.getJenkinsSecurityClient();
        String jenkinsJobData = securityClient.executeForJson(url);
        return client.getJenkinsParser().createBuild(jenkinsJobData);
    }

    private List<Build> loadBuilds(String jenkinsBuildUrl, RangeToLoad rangeToLoad) {
        if (handleNotYetLoggedInState()) return Collections.emptyList();
        URL url = urlBuilder.createBuildsUrl(jenkinsBuildUrl, rangeToLoad);
        JenkinsClient client = this.getClientByUrl(jenkinsBuildUrl);
        JenkinsSecurityClient securityClient = client.getJenkinsSecurityClient();
        String jenkinsJobData = securityClient.executeForJson(url);
        return client.getJenkinsParser().createBuilds(jenkinsJobData);
    }

    @Override
    public void runBuild(Job job, JenkinsAppSettings configuration, Map<String, ?> parameters) {
        if (handleNotYetLoggedInState()) return;
        if (job.hasParameters() && !parameters.isEmpty()) {
            parameters.keySet().removeIf(key -> !job.hasParameter(key));
        }
        final AtomicInteger fileCount = new AtomicInteger();

        final Collection<RequestData> requestData = new LinkedHashSet<>(parameters.size());
        parameters.entrySet().stream()
                .filter(entry -> entry.getValue() instanceof VirtualFile)
                .map(entry -> new FileParameter(entry.getKey(), (VirtualFile) entry.getValue(), () -> String.format("file%d", fileCount.getAndIncrement())))
                .forEach(requestData::add);
        parameters.entrySet().stream()
                .filter(entry -> entry.getValue() instanceof String)
                .map(entry -> new StringParameter(entry.getKey(), (String) entry.getValue()))
                .forEach(requestData::add);
        //有参构建
        runBuildWithParams(job, configuration, requestData);
    }

    private void runBuild(Job job, JenkinsAppSettings configuration, Collection<RequestData> requestData) {
        if (handleNotYetLoggedInState()) return;
        URL url = urlBuilder.createRunJobUrl(job.getUrl(), configuration);
        JenkinsSecurityClient securityClient = this.getSecurityClientByJob(job);
        final Response response = securityClient.executeForJson(url, requestData);
        if (response.getStatusCode() != HttpURLConnection.HTTP_CREATED) {
            throw new RunBuildError(Optional.ofNullable(response.getError()).orElse("Unknown"));
        }
    }

    private void runBuildWithParams(Job job, JenkinsAppSettings configuration,
                                    Collection<RequestData> requestData) {
        if (handleNotYetLoggedInState()) return;
        URL url = urlBuilder.createRunJobWithParamsUrl(job.getUrl(), configuration);
        JenkinsSecurityClient securityClient = this.getSecurityClientByJob(job);
        final Response response = securityClient.executeFormData(url, requestData);
        if (response.getStatusCode() != HttpURLConnection.HTTP_CREATED) {
            throw new RunBuildError(Optional.ofNullable(response.getError()).orElse("Unknown"));
        }
    }

    @Override
    public void authenticate(JenkinsAppSettings jenkinsAppSettings, JenkinsSettings jenkinsSettings) {
        SecurityClientFactory.setVersion(jenkinsSettings.getVersion());
        final int connectionTimout = getConnectionTimout(jenkinsSettings.getConnectionTimeout());
        for (MultiJenkinsSettings multiSetting : jenkinsSettings.getMultiSettings()) {
            String jenkinsServer = multiSetting.getJenkinsServer();
            JenkinsClient jenkinsClient = new JenkinsClient();
            jenkinsClient.setName(multiSetting.getName());
            jenkinsClient.setServer(jenkinsServer);
            jenkinsClients.put(jenkinsServer, jenkinsClient);
            if (multiSetting.isValid()) {
                JenkinsSecurityClient securityClient = SecurityClientFactory.basic(multiSetting.getUsername(),
                        multiSetting.getApiToken(), "", connectionTimout);
                jenkinsClient.setJenkinsSecurityClient(securityClient);
                final String serverUrl = multiSetting.getJenkinsServer();
                securityClient.connect(urlBuilder.createAuthenticationUrl(serverUrl));
                jenkinsClient.setJenkinsServer(new JenkinsServer(new JenkinsControlClient(urlBuilder.createServerUrl(serverUrl),
                        securityClient)));
                final UnaryOperator<String> urlMapper = ApplicationManager.getApplication()
                        .getService(UrlMapperService.class)
                        .getMapper(jenkinsSettings, serverUrl);
                jenkinsClient.setJenkinsParser(new JenkinsJsonParser(urlMapper));
            }
        }
    }

    @Override
    public String testAuthenticate(String serverUrl, String username, String password, String crumbData, JenkinsVersion version, int connectionTimoutInSeconds) {
        final JenkinsJsonParser jsonParserWithServerUrls = new JenkinsJsonParser(UnaryOperator.identity());
        SecurityClientFactory.setVersion(version);
        final int connectionTimout = getConnectionTimout(connectionTimoutInSeconds);
        final JenkinsSecurityClient jenkinsSecurityClientForTest;
        if (org.codinjutsu.tools.jenkins.util.StringUtil.isNotBlank(username)) {
            jenkinsSecurityClientForTest = SecurityClientFactory.basic(username, password, crumbData, connectionTimout);
        } else {
            jenkinsSecurityClientForTest = SecurityClientFactory.none(crumbData, connectionTimout);
        }
        final String serverData = jenkinsSecurityClientForTest.connect(urlBuilder.createAuthenticationUrl(serverUrl));
        return jsonParserWithServerUrls.getServerUrl(serverData);
    }

    @Override
    public List<Job> loadFavoriteJobs(List<FavoriteJob> favoriteJobs) {
        if (handleNotYetLoggedInState()) return Collections.emptyList();
        final List<Job> jobs = new LinkedList<>();
        //加载收藏的任务
        for (FavoriteJob favoriteJob : favoriteJobs) {
            String url = favoriteJob.getUrl();
            JenkinsClient client = this.getClientByUrl(url);
            if (client == null) {
                logger.error("Unable to find JenkinsClient by url: {}", url);
                continue;
            }
            jobs.add(loadJob(url));
        }
        return withNestedJobs(jobs);
    }

    @Override
    public void stopBuild(Build build) {
        stopBuild(build.getUrl());
    }

    @Override
    public @NotNull Job loadJob(Job job) {
        return withNodeParameterFix(loadJob(job.getUrl()), () -> loadComputer(JenkinsSettings.getSafeInstance(project)));
    }

    @NotNull
    @Override
    public List<Job> loadJenkinsView(@NotNull ViewV2 view) {
        return loadJenkinsView(view.getUrl());
    }

    @Override
    public List<Build> loadBuilds(Job job) {
        final var rangeToLoad = RangeToLoad.to(JenkinsAppSettings.getSafeInstance(project).getBuildsToLoadPerJob());
        return loadBuilds(job.getUrl(), rangeToLoad);
    }

    @NotNull
    @Override
    public Build loadBuild(Build build) {
        return loadBuild(build.getUrl());
    }

    @Override
    public void loadConsoleTextFor(Build buildModel, BuildLogConsoleStreamListener buildConsoleStreamListener) {
        loadConsoleTextFor(getBuild(buildModel), buildConsoleStreamListener, buildModel::getNameToRender);
    }

    @Override
    public void loadConsoleTextFor(Job job, BuildTypeEnum buildTypeEnum, BuildLogConsoleStreamListener buildConsoleStreamListener) {
        loadConsoleTextFor(getBuildForType(buildTypeEnum).apply(getJob(job)), buildConsoleStreamListener, job::getNameToRenderSingleJob);
    }

    private static final String HYPERLINK_FORMAT = "<html><a href=\"%s\">%s</a></html>";

    private String getHyperlink(String url, String displayName) {
        return String.format(HYPERLINK_FORMAT, url, displayName);
    }

    private void loadConsoleTextFor(com.offbytwo.jenkins.model.Build build,
                                    BuildLogConsoleStreamListener buildConsoleStreamListener,
                                    @NotNull Supplier<String> logName) {
        try {
            final int pollingInSeconds = 1;
            final int poolingTimeout = Math.toIntExact(TimeUnit.HOURS.toSeconds(1));
            buildConsoleStreamListener.forBuild(loadBuild(build.getUrl()));
            if (build.equals(com.offbytwo.jenkins.model.Build.BUILD_HAS_NEVER_RUN)) {
                buildConsoleStreamListener.onData("No Build available\n");
                buildConsoleStreamListener.finished();
            } else {
                String hyperlink = getHyperlink(build.getUrl(), SymbolPool.HASH + build.getNumber());
                buildConsoleStreamListener.onData("Log for Build " + build.getUrl() + "console\n, buildNumber:" + hyperlink);
                streamConsoleOutput(build.details(), buildConsoleStreamListener, pollingInSeconds, poolingTimeout);
            }
        } catch (IOException | InterruptedException e) {
            logger.warn("cannot load log for " + logName.get());
            Thread.currentThread().interrupt();
            buildConsoleStreamListener.finished();
            logger.error(String.format("Finished build log,url:%s,number:%s", build.getUrl(), build.getNumber()));
        }
    }

    private void streamConsoleOutput(BuildWithDetails buildWithDetails, BuildLogConsoleStreamListener listener, int poolingInterval, int poolingTimeout) throws InterruptedException, IOException {
        // Calculate start and timeout
        final long startTime = System.currentTimeMillis();
        final long timeoutTime = startTime + (poolingTimeout * 1000L);
        final long sleepMillis = poolingInterval * 1000L;

        final AtomicInteger bufferOffset = new AtomicInteger(0);
        while (true) {
            //noinspection BusyWait
            Thread.sleep(sleepMillis);
            ProgressManager.checkCanceled();
            final ConsoleLog consoleLog = buildWithDetails.getConsoleOutputText(bufferOffset.get());
            String logString = consoleLog.getConsoleLog();
            if (logString != null && !logString.isEmpty()) {
                listener.onData(logString);
            }
            if (Boolean.TRUE.equals(consoleLog.getHasMoreData())) {
                bufferOffset.set(consoleLog.getCurrentBufferSize());
            } else {
                listener.finished();
                break;
            }
            if (System.currentTimeMillis() > timeoutTime) {
                throw new JenkinsPluginRuntimeException(String.format("Pooling for build %s - %d timeout! " + "Check if job stuck in jenkins", buildWithDetails.getDisplayName(), buildWithDetails.getNumber()));
            }
        }
    }

    @NotNull
    private Function<JobWithDetails, com.offbytwo.jenkins.model.Build> getBuildForType(BuildTypeEnum buildTypeEnum) {
        final Function<JobWithDetails, com.offbytwo.jenkins.model.Build> buildProvider;
        switch (buildTypeEnum) {
            case LAST_SUCCESSFUL:
                buildProvider = JobWithDetails::getLastSuccessfulBuild;
                break;
            case LAST_FAILED:
                buildProvider = JobWithDetails::getLastFailedBuild;
                break;
            case LAST://Fallthrough
            default:
                buildProvider = preferLastBuildRunning(JobWithDetails::getLastCompletedBuild);
        }
        return buildProvider;
    }

    @NotNull
    private Function<JobWithDetails, com.offbytwo.jenkins.model.Build> preferLastBuildRunning(Function<JobWithDetails, com.offbytwo.jenkins.model.Build> fallback) {
        return job -> {
            try {
                com.offbytwo.jenkins.model.Build lastBuild = job.getLastBuild();
                if (lastBuild.details().isBuilding()) {
                    return lastBuild;
                }
            } catch (IOException e) {
                logger.warn("cannot load details for " + job.getName());
            }
            return fallback.apply(job);
        };
    }

    @Override
    public List<TestResult> loadTestResultsFor(Job job) {
        return loadTestResultsFor(getBuildForType(BuildTypeEnum.LAST).apply(getJob(job)));
    }

    @Override
    public List<TestResult> loadTestResultsFor(Build build) {
        return loadTestResultsFor(getBuild(build));
    }

    private List<TestResult> loadTestResultsFor(com.offbytwo.jenkins.model.Build build) {
        try {
            final List<TestResult> result = new ArrayList<>();
            if (build.getTestResult() != null) {
                result.add(build.getTestResult());
            }
            if (build.getTestReport().getChildReports() != null) {
                result.addAll(build.getTestReport().getChildReports().stream().map(TestChildReport::getResult).collect(Collectors.toList()));
            }
            return result;
        } catch (IOException e) {
            logger.warn("cannot load test results for " + build.getUrl());
            return Collections.emptyList();
        }
    }

    @NotNull
    @Override
    public List<Computer> loadComputer(JenkinsSettings settings) {
        if (handleNotYetLoggedInState()) {
            return Collections.emptyList();
        }
        return jenkinsClients.entrySet().stream().flatMap(entry -> {
            String server = entry.getKey();
            JenkinsClient client = entry.getValue();
            final URL url = urlBuilder.createComputerUrl(server);
            return client.getJenkinsParser().createComputers(client.getJenkinsSecurityClient()
                    .executeForJson(url)).stream();
        }).collect(Collectors.toList());
    }

    @NotNull
    @Override
    public List<String> getGitParameterChoices(Job job, JobParameter jobParameter) {
        return Optional.ofNullable(jobParameter.getJobParameterType()).map(JobParameterType::getClassName).map(jobClassName -> getFillValueItems(job, jobClassName, jobParameter.getName())).orElse(Collections.emptyList());
    }

    @NotNull
    private List<String> getFillValueItems(Job job, String parameterClassName, String parameterName) {
        final URL url = urlBuilder.createFillValueItemsUrl(job.getUrl(), parameterClassName, parameterName);
        JenkinsClient client = this.getClientByJob(job);
        JenkinsSecurityClient securityClient = client.getJenkinsSecurityClient();
        return client.getJenkinsParser().getFillValueItems(securityClient.executeForJson(url));
    }

    @NotNull
    private JobWithDetails getJob(@NotNull Job job) {
        final Optional<JobWithDetails> jobWithDetails;
        try {
            JenkinsClient client = this.getClientByUrl(job.getUrl());
            // maybe refactor and use job url
            jobWithDetails = Optional.ofNullable(client.getJenkinsServer().getJob(job.getFullName()));
        } catch (IOException e) {
            logger.warn(e.getMessage(), e);
            throw new NoJobFoundException(job, e);
        }
        return jobWithDetails.orElseThrow(() -> new NoJobFoundException(job));
    }

    @NotNull
    private com.offbytwo.jenkins.model.Build getBuild(@NotNull Build build) {
        final Optional<com.offbytwo.jenkins.model.Build> serverBuild;
        try {
            final var buildQueueItem = new QueueItem();
            final Executable executable = new Executable();
            buildQueueItem.setExecutable(executable);
            executable.setNumber((long) build.getNumber());
            executable.setUrl(build.getUrl());
            JenkinsServer jenkinsServer = this.getByBuild(build);
            serverBuild = Optional.ofNullable(jenkinsServer.getBuild(buildQueueItem).details());
        } catch (IOException e) {
            logger.warn(e.getMessage(), e);
            throw new NoBuildFoundException(build, e);
        }
        return serverBuild.orElseThrow(() -> new NoBuildFoundException(build));
    }

    /**
     * 根据<p>job</p>获取<p>jenkins</p>客户端连接
     *
     * @param job
     * @return
     */
    private JenkinsServer getServerByJob(Job job) {
        return this.getServerByUrl(job.getUrl());
    }

    private JenkinsServer getServerByUrl(String url) {
        return jenkinsClients.entrySet().stream().filter(entry -> {
            String key = entry.getKey();
            return url.startsWith(key);
        }).map(entry -> entry.getValue().getJenkinsServer()).findFirst().orElse(null);
    }

    /**
     * 根据<p>job</p>获取<p>jenkins</p>客户端连接
     *
     * @param job
     * @return
     */
    private JenkinsSecurityClient getSecurityClientByJob(Job job) {
        return this.getSecurityClientByUrl(job.getUrl());
    }

    private JenkinsClient getClientByJob(Job job) {
        return this.getClientByUrl(job.getUrl());
    }

    private JenkinsSecurityClient getSecurityClientByUrl(String url) {
        return jenkinsClients.entrySet().stream().filter(entry -> {
            String key = entry.getKey();
            return url.startsWith(key);
        }).map(entry -> entry.getValue().getJenkinsSecurityClient()).findFirst().orElse(null);
    }

    private JenkinsClient getClientByUrl(String url) {
        return jenkinsClients.entrySet().stream().filter(entry -> {
            String key = entry.getKey();
            return url.startsWith(key);
        }).map(Map.Entry::getValue).findFirst().orElse(null);
    }

    /**
     * 根据build获取jenkins服务端连接
     *
     * @param build
     * @return
     */
    private JenkinsServer getByBuild(Build build) {
        String url = build.getUrl();
        return jenkinsClients.entrySet().stream().filter(entry -> {
            String key = entry.getKey();
            return url.startsWith(key);
        }).map(entry -> entry.getValue().getJenkinsServer()).findFirst().orElse(null);
    }

    private int getConnectionTimout(int connectionTimoutInSeconds) {
        return connectionTimoutInSeconds * 1000;
    }

    @Override
    public void dispose() {
        jenkinsClients.forEach((server, client) -> client.getJenkinsServer().close());
    }

    public List<BuildHistory> findRecently50SuccessBuilds(Job job) {
        return this.findRecentlySuccessBuilds(job, 50);
    }

    public List<BuildHistory> findRecently5SuccessBuilds(Job job) {
        List<BuildHistory> successBuilds = this.findRecentlySuccessBuilds(job, 10);
        return successBuilds.stream().limit(5).collect(Collectors.toList());
    }

    private static final String DOT_REPOSITORY_PREFIX = ".repository";

    public List<BuildArtifacts.Artifact> findArtifactsByBuildNumber(Job job, String buildNumber) {
        BuildArtifacts buildArtifacts;
        try {
            URL url = urlBuilder.createArtifactsUrl(job.getUrl(), buildNumber);
            JenkinsSecurityClient securityClient = this.getSecurityClientByUrl(job.getUrl());
            String jsonData = securityClient.executeForJson(url);
            if (StringUtils.isBlank(jsonData)) {
                return Collections.emptyList();
            }
            buildArtifacts = JSON.parseObject(jsonData, BuildArtifacts.class);
            List<BuildArtifacts.Artifact> artifacts = buildArtifacts.getArtifacts();
            return artifacts.stream().filter(artifact -> !artifact.getRelativePath()
                    .startsWith(DOT_REPOSITORY_PREFIX)).collect(Collectors.toList());
        } catch (Exception e) {
            logger.info("获取{%s}-{%s}构建的组件失败".formatted(job.getUrl(), buildNumber), e);
        }
        return Collections.emptyList();
    }

    /**
     * 获取最近{limit}个成功构建
     *
     * @param job
     * @return 最近{limit}个成功构建
     */
    private List<BuildHistory> findRecentlySuccessBuilds(Job job, int limit) {
        URL url = urlBuilder.createBuildHistoryUrl(job.getUrl(), limit);
        JenkinsSecurityClient securityClient = this.getSecurityClientByUrl(job.getUrl());
        String jsonData = securityClient.executeForJson(url);
        if (StringUtils.isBlank(jsonData)) {
            return Collections.emptyList();
        }
        BuildHistories histories;
        try {
            histories = JSON.parseObject(jsonData, BuildHistories.class);
            return Optional.ofNullable(histories.getBuilds()).stream().flatMap(Collection::stream).filter(buildHistory -> BuildStatusEnum.SUCCESS.equalsByStatus(buildHistory.getResult())).collect(Collectors.toList());
        } catch (Exception e) {
            logger.info("最近%s个成功构建失败".formatted(limit), e);
        }
        return Collections.emptyList();
    }

    public interface BuildLogConsoleStreamListener extends BuildConsoleStreamListener {
        void forBuild(Build build);
    }
}
