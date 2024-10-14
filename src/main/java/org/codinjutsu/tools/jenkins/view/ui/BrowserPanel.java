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

package org.codinjutsu.tools.jenkins.view.ui;

import com.intellij.notification.NotificationType;
import com.intellij.openapi.actionSystem.ActionManager;
import com.intellij.openapi.actionSystem.DefaultActionGroup;
import com.intellij.openapi.components.*;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.ui.SimpleToolWindowPanel;
import com.intellij.openapi.util.text.StringUtil;
import com.intellij.ui.PopupHandler;
import com.intellij.ui.ScrollPaneFactory;
import com.intellij.util.ui.tree.TreeUtil;
import lombok.Getter;
import org.codinjutsu.tools.jenkins.DoubleClickAction;
import org.codinjutsu.tools.jenkins.JenkinsTree;
import org.codinjutsu.tools.jenkins.JenkinsTreeState;
import org.codinjutsu.tools.jenkins.common.CopyHyperlinkAction;
import org.codinjutsu.tools.jenkins.enums.BuildStatusEnum;
import org.codinjutsu.tools.jenkins.enums.BuildTypeEnum;
import org.codinjutsu.tools.jenkins.logic.*;
import org.codinjutsu.tools.jenkins.model.jenkins.*;
import org.codinjutsu.tools.jenkins.persistent.JenkinsAppSettings;
import org.codinjutsu.tools.jenkins.persistent.JenkinsProjectSettings;
import org.codinjutsu.tools.jenkins.persistent.JenkinsSettings;
import org.codinjutsu.tools.jenkins.settings.multiServer.MultiJenkinsSettings;
import org.codinjutsu.tools.jenkins.util.CollectionUtil;
import org.codinjutsu.tools.jenkins.util.GuiUtil;
import org.codinjutsu.tools.jenkins.view.JenkinsStatusBarWidget;
import org.codinjutsu.tools.jenkins.view.JenkinsTreeNode;
import org.codinjutsu.tools.jenkins.view.action.*;
import org.codinjutsu.tools.jenkins.view.action.settings.SortByStatusAction;
import org.jetbrains.annotations.NonNls;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.swing.*;
import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.TreePath;
import java.awt.*;
import java.util.List;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;
import java.util.stream.Collectors;


@State(name = "JenkinsBrowserPanel", storages = {
        @Storage(value = StoragePathMacros.PRODUCT_WORKSPACE_FILE, roamingType = RoamingType.DISABLED)
}, getStateRequiresEdt = true)
@Service(Service.Level.PROJECT)
public final class BrowserPanel extends SimpleToolWindowPanel implements PersistentStateComponent<JenkinsTreeState> {

    @NonNls
    public static final String POPUP_PLACE = "POPUP";
    @NonNls
    public static final String JENKINS_PANEL_PLACE = "jenkinsBrowserActions";
    private static final Logger logger = Logger.getInstance(BrowserPanel.class);
    private static final JobNameComparator JOB_NAME_COMPARATOR = new JobNameComparator();
    private static final Comparator<Job> sortByStatusComparator = Comparator.comparing(BrowserPanel::toBuildStatus);
    private static final Comparator<Job> sortByNameComparator = Comparator.comparing(Job::getNameToRenderSingleJob, JOB_NAME_COMPARATOR);
    @NotNull
    private final JenkinsTree jobTree;
    @NotNull
    private final Runnable refreshViewJob;
    @Getter
    @NotNull
    private final Project project;
    private final JenkinsAppSettings jenkinsAppSettings;
    @NotNull
    private final JenkinsSettings jenkinsSettings;
    @NotNull
    private final JenkinsProjectSettings jenkinsProjectSettings;
    @Getter
    private final MultiJenkins multiJenkins;
    private final RequestManagerInterface requestManager;
    private final Map<String, Job> watchedJobs = new ConcurrentHashMap<>();
    private JPanel rootPanel;
    private JPanel jobPanel;
    private boolean sortedByBuildStatus;
    private ScheduledFuture<?> refreshViewFutureTask;
    private FavoriteView favoriteView;
    @Nullable
    private ViewV2 currentSelectedView;

    public BrowserPanel(@NotNull Project project) {
        super(true);
        this.project = project;

        final LoadSelectedViewJob loadSelectedViewJob = new LoadSelectedViewJob(project);
        this.refreshViewJob = loadSelectedViewJob::queue;

        this.requestManager = RequestManager.getInstance(project);
        this.jenkinsAppSettings = JenkinsAppSettings.getSafeInstance(project);
        this.jenkinsSettings = JenkinsSettings.getSafeInstance(project);
        this.jenkinsProjectSettings = JenkinsProjectSettings.getSafeInstance(project);
        setProvideQuickActions(false);

        List<Jenkins> jenkinsList = new ArrayList<>();
        List<MultiJenkinsSettings> multiSettings = this.jenkinsSettings.getMultiSettings();
        multiSettings.forEach(multiSetting -> {
            Jenkins jenkins = Jenkins.byDefault(multiSetting.getName());
            jenkins.setServerUrl(multiSetting.getJenkinsServer());
            jenkinsList.add(jenkins);
        });
        this.multiJenkins = new MultiJenkins(jenkinsList);
        this.jobTree = new JenkinsTree(project, this.jenkinsSettings, jenkinsList);
        this.updateDoubleClickAction(getDoubleClickAction(this.jenkinsAppSettings.getDoubleClickAction()));

        this.jobPanel.setLayout(new BorderLayout());
        this.jobPanel.add(ScrollPaneFactory.createScrollPane(this.jobTree.asComponent()), BorderLayout.CENTER);

        setContent(this.rootPanel);
    }

    @NotNull
    private static JobAction getDoubleClickAction(@NotNull DoubleClickAction doubleClickAction) {
        return switch (doubleClickAction) {
            case LOAD_BUILDS -> JobActions.loadBuilds();
            case SHOW_LAST_LOG -> JobActions.showLastLog();
            default -> JobActions.triggerBuild();
        };
    }

    @NotNull
    private static BuildStatusEnum toBuildStatus(Job job) {
        return BuildStatusEnum.getStatusByColor(job.getColor());
    }

    public static BrowserPanel getInstance(Project project) {
        return project.getService(BrowserPanel.class);
    }

    private static void visit(Job job, BuildStatusVisitor buildStatusVisitor) {
        Build lastBuild = job.getLastBuild();
        if (job.isBuildable() && lastBuild != null) {
            BuildStatusEnum status = lastBuild.getStatus();
            if (job.getLastBuild().isBuilding()) {
                buildStatusVisitor.visitBuilding();
                return;
            }
            if (BuildStatusEnum.FAILURE == status) {
                buildStatusVisitor.visitFailed();
                return;
            }
            if (BuildStatusEnum.SUCCESS == status) {
                buildStatusVisitor.visitSuccess();
                return;
            }
            if (BuildStatusEnum.UNSTABLE == status) {
                buildStatusVisitor.visitUnstable();
                return;
            }
            if (BuildStatusEnum.ABORTED == status) {
                buildStatusVisitor.visitAborted();
                return;
            }
            if (BuildStatusEnum.NULL == status) {
                buildStatusVisitor.visitUnknown();
                return;
            }
        }

        buildStatusVisitor.visitUnknown();
    }

    private void updateDoubleClickAction(@NotNull JobAction doubleClickAction) {
        GuiUtil.runInSwingThread(() -> jobTree.updateDoubleClickAction(doubleClickAction));
    }

    /*whole method could be moved inside of ExecutorProvider (executor would expose interface that would allow to schedule
      new task previously cancelling previous ones) */
    public void initScheduledJobs() {
        final ExecutorService executorService = ExecutorService.getInstance(project);
        final ScheduledThreadPoolExecutor executor = executorService.getExecutor();
        executorService.safeTaskCancel(refreshViewFutureTask);
        executor.remove(refreshViewJob);

        if (jenkinsSettings.isServerUrlSet() && jenkinsAppSettings.getJobRefreshPeriod() > 0) {
            refreshViewFutureTask = executor.scheduleWithFixedDelay(refreshViewJob, jenkinsAppSettings.getJobRefreshPeriod(), jenkinsAppSettings.getJobRefreshPeriod(), TimeUnit.MINUTES);
        }
    }

    public Optional<Jenkins> getSelectedServer() {
        return jobTree.getLastSelectedPath(JenkinsTreeNode.RootNode.class)
                .map(JenkinsTreeNode.RootNode::jenkins);
    }

    public @NotNull Optional<Build> getSelectedBuild() {
        return jobTree.getLastSelectedPath(JenkinsTreeNode.BuildNode.class)
                .map(JenkinsTreeNode.BuildNode::build);
    }

    public @NotNull String getSelectedBuildUrl() {
        return getSelectedBuild().map(Build::getUrl).orElse("");
    }

    @Nullable
    public Job getSelectedJob() {
        return jobTree.getLastSelectedPath(JenkinsTreeNode.JobNode.class)
                .map(JenkinsTreeNode.JobNode::job).orElse(null);
    }

    public List<Job> getAllSelectedJobs() {
        return TreeUtil.collectSelectedObjectsOfType(jobTree.getTree(), JenkinsTreeNode.JobNode.class).stream()
                .map(JenkinsTreeNode.JobNode::job).collect(Collectors.toList());
    }

    @NotNull
    public List<Job> getAllJobs() {
        return CollectionUtil.flattenedJobs(multiJenkins.getJobs());
    }

    @NotNull
    public Optional<Job> getJob(String name) {
        return getAllJobs().stream().filter(job -> job.getNameToRenderSingleJob().equals(name)).findFirst();
    }

    public void setSortedByStatus(boolean sortedByBuildStatus) {
        this.sortedByBuildStatus = sortedByBuildStatus;
        jobTree.keepLastState(() -> jobTree.sortJobs(getCurrentSorting()));
    }

    @NotNull
    private Comparator<Job> getCurrentSorting() {
        return sortedByBuildStatus ? sortByStatusComparator : sortByNameComparator;
    }

    private void updateSelection() {
        jobTree.updateSelection();
    }

    public @Nullable ViewV2 getCurrentSelectedView() {
        return currentSelectedView;
    }

    @NotNull
    public RequestManagerInterface getJenkinsManager() {
        return requestManager;
    }

    private void addJenkinsNode(Jenkins jenkins) {
        this.jobTree.addJenkinsNode(jenkins);
    }

    private void clearJenkinsNode() {
        this.jobTree.clearJenkinsNode();
    }

    public void loadJob(final Job job) {
        loadJob(job, j -> {
        });
    }

    public void loadJob(final Job job, Consumer<Job> loadedJob) {
        if (!SwingUtilities.isEventDispatchThread()) {
            logger.warn("BrowserPanel.loadJob called from outside of EDT");
        }
        JenkinsBackgroundTaskFactory.getInstance(project).createBackgroundTask(
                "Loading job", true, new JenkinsBackgroundTask.JenkinsTask() {

                    private Job returnJob;

                    @Override
                    public void run(@NotNull RequestManagerInterface requestManager) {
                        returnJob = requestManager.loadJob(job);
                    }

                    @Override
                    public void onSuccess() {
                        JenkinsBackgroundTask.JenkinsTask.super.onSuccess();
                        job.updateContentWith(returnJob);
                        refreshJob(job);
                        loadedJob.accept(job);
                    }
                }).queue();
    }

    public void refreshJob(Job job) {
        updateJobNode(job);
    }

    private void updateJobNode(Job job) {
        BuildStatusAggregator buildStatusAggregator = new BuildStatusAggregator();
        GuiUtil.runInSwingThread(() -> {
            jobTree.updateJobNode(job);
            CollectionUtil.flattenedJobs(multiJenkins.getJobs()).forEach(j -> visit(j, buildStatusAggregator));
            JenkinsStatusBarWidget.getInstance(project).updateStatusIcon(buildStatusAggregator);
        });
    }

    public boolean hasFavoriteJobs() {
        return jenkinsSettings.hasFavoriteJobs();
    }

    public void notifyInfoJenkinsToolWindow(@NotNull String message) {
        JenkinsNotifier.getInstance(project).notify(message, NotificationType.INFORMATION);
    }

    public void notifyInfoJenkinsToolWindow(@NotNull String message, String urlToOpen) {
        JenkinsNotifier.getInstance(project).notify(message, urlToOpen, NotificationType.INFORMATION);
    }

    public void notifyErrorJenkinsToolWindow(@NotNull String message) {
        JenkinsNotifier.getInstance(project).error(message);
    }

    public void handleEmptyConfiguration() {
        currentSelectedView = null;
        setJobsUnavailable();
    }

    public void setJobsUnavailable() {
        clearView();
        jobTree.setJobsUnavailable();
    }

    private void clearView() {
        jobTree.clear();
        JenkinsStatusBarWidget.getInstance(project).updateStatusIcon(BuildStatusAggregator.EMPTY);
    }

    public void postAuthenticationInitialization() {
        if (hasFavoriteJobs()) {
            createFavoriteViewIfNecessary();
        }

        String lastSelectedViewName = jenkinsSettings.getLastSelectedView();
        ViewV2 viewToLoad;
        if (StringUtil.isEmpty(lastSelectedViewName)) {
            viewToLoad = multiJenkins.getPrimaryView();
        } else if (favoriteView != null && lastSelectedViewName.equals(favoriteView.getName())) {
            viewToLoad = favoriteView;
        } else {
            viewToLoad = multiJenkins.getViewByName(lastSelectedViewName);
        }
        loadView(viewToLoad);
    }

    public void initGui() {
        installActionsInToolbar();
        installActionsInPopupMenu();
    }

    private void installActionsInToolbar() {
        final ActionManager actionManager = ActionManager.getInstance();
        DefaultActionGroup actionGroup = new DefaultActionGroup("JenkinsToolbarGroup", false);
        actionGroup.add(new SelectViewAction(this));
        actionGroup.add(new RefreshNodeAction(this));
        actionGroup.add(actionManager.getAction(LoadBuildsAction.ACTION_ID));
        actionGroup.add(actionManager.getAction(RunBuildAction.ACTION_ID));
        actionGroup.add(actionManager.getAction(StopBuildAction.ACTION_ID));
        actionGroup.add(new SortByStatusAction(this));
        actionGroup.add(new RefreshRssAction());
        actionGroup.addSeparator();
        actionGroup.add(actionManager.getAction("Jenkins.ShowSettingsGroup"));

        GuiUtil.installActionGroupInToolBar(actionGroup, this, actionManager, JENKINS_PANEL_PLACE);
    }

    private void installActionsInPopupMenu() {
        //fixme 列表添加菜单
        DefaultActionGroup popupGroup = new DefaultActionGroup("JenkinsPopupAction", true);
        popupGroup.add(ActionManager.getInstance().getAction(CopyHyperlinkAction.ACTION_ID));
        popupGroup.addSeparator();
        popupGroup.add(ActionManager.getInstance().getAction(RunBuildAction.ACTION_ID));
        popupGroup.add(ActionManager.getInstance().getAction(RetryLastSuccessfulBuildAction.ACTION_ID));
        popupGroup.add(ActionManager.getInstance().getAction(StopBuildAction.ACTION_ID));
//        popupGroup.add(ActionManager.getInstance().getAction(BuilcConfigAction.ACTION_ID));
        popupGroup.addSeparator();
        popupGroup.add(new ShowLogAction(BuildTypeEnum.LAST));
        popupGroup.add(new ShowLogAction(BuildTypeEnum.LAST_SUCCESSFUL));
        popupGroup.add(new ShowLogAction(BuildTypeEnum.LAST_FAILED));
        popupGroup.add(new ShowBuildLogAction());
        //`My Favorites`收藏的`job`
        popupGroup.addSeparator();
        popupGroup.add(new SetJobAsFavoriteAction(this));
        popupGroup.add(new UnsetJobAsFavoriteAction(this));
        popupGroup.addSeparator();
        //`Project Jobs`收藏的`job`
        popupGroup.addSeparator();
        popupGroup.add(new SetJobForProjectAction(this));
        popupGroup.add(new UnsetJobForProjectAction(this));
        popupGroup.addSeparator();
        popupGroup.add(new GotoServerAction(this));
        popupGroup.add(new GotoJobPageAction(this));
        popupGroup.add(new GotoBuildPageAction(this));
        popupGroup.add(new GotoBuildConsolePageAction(this));
        popupGroup.add(new GotoBuildTestResultsPageAction(this));
        popupGroup.add(new GotoAllureReportPageAction(this));
        popupGroup.add(new GotoLastBuildPageAction(this));
        popupGroup.addSeparator();
        popupGroup.add(new UploadPatchToJobAction(this));

        PopupHandler.installPopupMenu(jobTree.asComponent(), popupGroup, POPUP_PLACE);
    }

    public void loadView(final ViewV2 view) {
        this.currentSelectedView = view;
        if (!SwingUtilities.isEventDispatchThread()) {
            logger.warn("BrowserPanel.loadView called from outside EDT");
        }
        refreshViewJob.run();
    }

    public void refreshCurrentView() {
        if (!SwingUtilities.isEventDispatchThread()) {
            logger.warn("BrowserPanel.refreshCurrentView called outside EDT");
        }
        refreshViewJob.run();
    }

    public void setAsFavorite(final List<Job> jobs) {
        jenkinsSettings.addFavorite(jobs);
        createFavoriteViewIfNecessary();
        updateSelection();
    }

    public void removeFavorite(final List<Job> selectedJobs) {
        jenkinsSettings.removeFavorite(selectedJobs);
        updateSelection();
        if (jenkinsSettings.isFavoriteViewEmpty() && currentSelectedView instanceof FavoriteView) {
            favoriteView = null;
            loadView(multiJenkins.getPrimaryView());
        } else {
            if (currentSelectedView instanceof FavoriteView) {
                loadView(currentSelectedView);
            }
        }
    }

    /**
     * 移除项目job
     *
     * @param selectedJobs 选中`jobs`
     */
    public void removeProjectJob(final List<Job> selectedJobs) {
        jenkinsProjectSettings.removeProjectJob(selectedJobs);
        updateSelection();
    }

    public boolean isAFavoriteJob(@NotNull Job job) {
        return jenkinsSettings.isFavoriteJob(job);
    }

    public void setAsProject(final List<Job> jobs) {
        jenkinsProjectSettings.addProjectJob(jobs);
        DefaultMutableTreeNode projectJobNode = this.jobTree.lastProjectJobNode();
        if (projectJobNode != null) {
            jobs.forEach(job -> {
                //添加job
                projectJobNode.add(new DefaultMutableTreeNode(new JenkinsTreeNode.JobNode(job)));
            });
        } else {
            this.jobTree.addProjectJobNode(this.project);
        }
        updateSelection();
    }

    public boolean isAProjectJob(@NotNull Job job) {
        return jenkinsProjectSettings.isFavoriteJob(job);
    }

    private void createFavoriteViewIfNecessary() {
        if (favoriteView == null) {
            favoriteView = FavoriteView.create();
        }
    }

    public boolean isConfigured() {
        return jenkinsSettings.isServerUrlSet();
    }

    public void updateWorkspace(Jenkins jenkinsWorkspace) {
        multiJenkins.update(jenkinsWorkspace);
    }

    public void addToWatch(String changeListName, Job job) {
        final Build lastBuild = job.getLastBuild();
        if (lastBuild != null) {
            final int nextBuildNumber = lastBuild.getNumber() + 1;
            final Build nextBuild = lastBuild.toBuilder()
                    .number(nextBuildNumber)
                    .url(String.format("%s/%d/", job.getUrl(), nextBuildNumber))
                    .build();
            job.setLastBuild(nextBuild);
        }
        watchedJobs.put(changeListName, job);
    }

    /**
     * 监听正在运行的job
     */
    private void watch() {
        if (!SwingUtilities.isEventDispatchThread()) {
            logger.warn("BrowserPanel.watch called from outside EDT");
        }
        if (!watchedJobs.isEmpty()) {
            for (final Map.Entry<String, Job> entry : watchedJobs.entrySet()) {
                final Job job = entry.getValue();
                final Build lastBuild = job.getLastBuild();
                JenkinsBackgroundTaskFactory.getInstance(project).createBackgroundTask("Jenkins build watch", true,
                        new JenkinsBackgroundTask.JenkinsTask() {

                            private Build build;

                            @Override
                            public void run(@NotNull RequestManagerInterface requestManager) {
                                build = requestManager.loadBuild(lastBuild);
                            }

                            @Override
                            public void onSuccess() {
                                JenkinsBackgroundTask.JenkinsTask.super.onSuccess();
                                if (Optional.ofNullable(lastBuild).map(Build::isBuilding).orElse(false)
                                        && !build.isBuilding()) {
                                    notifyInfoJenkinsToolWindow(
                                            String.format("Status of build for Changelist \"%s\" is %s",
                                                    entry.getKey(), build.getStatus().getStatus()));
                                }
                                job.setLastBuild(build);
                            }

                            @Override
                            public void onThrowable(@NotNull Throwable error) {
                                JenkinsBackgroundTask.JenkinsTask.super.onThrowable(error);
                                notifyErrorJenkinsToolWindow(String.format("Error while watch for Changelist \"%s\" is %s",
                                        entry.getKey(), error.getMessage()));
                            }
                        }).queue();
            }
        }
    }

    public Map<String, Job> getWatched() {
        return watchedJobs;
    }

    @Nullable
    @Override
    public JenkinsTreeState getState() {
        return jobTree.getState();
    }

    @Override
    public void loadState(@NotNull JenkinsTreeState state) {
        jobTree.loadState(state);
    }

    public void reloadConfiguration(@NotNull JenkinsAppSettings newJenkinsAppSettings) {
        updateDoubleClickAction(getDoubleClickAction(newJenkinsAppSettings.getDoubleClickAction()));
    }

    public void expandSelectedJob() {
        GuiUtil.runInSwingThread(() -> Optional.ofNullable(jobTree.getLastSelectedPathComponent())
                .filter(node -> node.getUserObject() instanceof JenkinsTreeNode.JobNode)
                .ifPresent(node -> jobTree.getTree().expandPath(new TreePath(node.getPath()))));
    }

    public void addJenkins(Jenkins jenkins) {
        multiJenkins.add(jenkins);
        this.addJenkinsNode(jenkins);
    }

    public void clearJenkins() {
        multiJenkins.clear();
        this.clearJenkinsNode();
    }

    private class LoadSelectedViewJob implements JenkinsBackgroundTask.JenkinsTask {

        @NotNull
        private final JenkinsBackgroundTask task;

        public LoadSelectedViewJob(@NotNull Project project) {
            this.task = JenkinsBackgroundTaskFactory.getInstance(project).createBackgroundTask(
                    "Loading Jenkins Jobs", true, LoadSelectedViewJob.this);
        }

        @Override
        public void run(@NotNull RequestManagerInterface requestManager) {
            try {
                setTreeBusy(true);
                ViewV2 viewToLoad = getViewToLoad();
                if (viewToLoad == null) {
                    return;
                }
                currentSelectedView = viewToLoad;
                //view加载构建任务
                loadJobs();
            } finally {
                setTreeBusy(false);
            }
        }

        @Override
        public void onSuccess() {
            final BuildStatusAggregator buildStatusAggregator = new BuildStatusAggregator();

            GuiUtil.runInSwingThread(() -> {
                fillJobTree(buildStatusAggregator);
                JenkinsStatusBarWidget.getInstance(project).updateStatusIcon(buildStatusAggregator);
            });
        }

        private void loadJobs() {
            if (SwingUtilities.isEventDispatchThread()) {
                logger.warn("BrowserPanel.loadJobs called from EDT");
            }
            final List<Job> jobList;
            final ViewV2 viewToLoadJobs = currentSelectedView;
            //fixme 根据view去更新,这里需要加一个根据选中的选项更新状态
            if (viewToLoadJobs instanceof FavoriteView) {
                jobList = requestManager.loadFavoriteJobs(jenkinsSettings.getFavoriteJobs());
            } else {
                jobList = requestManager.loadJenkinsView(viewToLoadJobs);
            }
            if (jenkinsAppSettings.isAutoLoadBuilds()) {
                for (Job job : jobList) {
                    job.setLastBuilds(requestManager.loadBuilds(job));
                }
            }
            jenkinsSettings.setLastSelectedView(viewToLoadJobs.getName());
            multiJenkins.setJobs(jobList);
        }

        @Nullable
        private ViewV2 getViewToLoad() {
            if (currentSelectedView == null) {
                return multiJenkins.getPrimaryView();
            }
            return currentSelectedView;
        }

        private void fillJobTree(final BuildStatusVisitor buildStatusVisitor) {
            final List<Job> jobList = multiJenkins.getJobs();
            jobTree.keepLastState(() -> {
                jobTree.setJobs(jobList);
                CollectionUtil.flattenedJobs(jobList).forEach(job -> visit(job, buildStatusVisitor));
                watch();
                jobTree.sortJobs(getCurrentSorting());
            });
        }

        private void setTreeBusy(final boolean isBusy) {
            GuiUtil.runInSwingThread(() -> jobTree.getTree().setPaintBusy(isBusy));
        }

        public void queue() {
            task.queue();
        }
    }
}
