package org.codinjutsu.tools.jenkins.task;

import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.progress.ProgressIndicator;
import com.intellij.openapi.progress.Task;
import com.intellij.openapi.project.Project;
import org.codinjutsu.tools.jenkins.JobTracker;
import org.codinjutsu.tools.jenkins.TraceableBuildJob;
import org.codinjutsu.tools.jenkins.TraceableBuildJobFactory;
import org.codinjutsu.tools.jenkins.logic.RequestManagerInterface;
import org.codinjutsu.tools.jenkins.model.jenkins.Job;
import org.codinjutsu.tools.jenkins.persistent.JenkinsAppSettings;
import org.codinjutsu.tools.jenkins.task.callback.RunBuildCallback;
import org.codinjutsu.tools.jenkins.view.ui.BuildParamDialog;
import org.jetbrains.annotations.NotNull;

import java.util.Map;

public class RunBuild extends Task.Backgroundable {

    private final Job job;
    private final JenkinsAppSettings configuration;
    private final Map<String, ?> paramValueMap;
    private final RequestManagerInterface requestManager;
    private final RunBuildCallback runBuildCallback;

    private static final Logger logger = Logger.getInstance(BuildParamDialog.class);

    public RunBuild(Project project, Job job, JenkinsAppSettings configuration, Map<String, ?> paramValueMap,
                    RequestManagerInterface requestManager, RunBuildCallback runBuildCallback) {
        super(project, "Running Jenkins build", false);
        this.job = job;
        this.configuration = configuration;
        this.paramValueMap = paramValueMap;
        this.requestManager = requestManager;
        this.runBuildCallback = runBuildCallback;
    }

    @Override
    public void onSuccess() {
        super.onSuccess();
        runBuildCallback.notifyOnOk(job);
    }

    @Override
    public void onThrowable(@NotNull Throwable error) {
        logger.warn("Exception occured while trying to invoke build", error);
        runBuildCallback.notifyOnError(job, error);
    }

    @Override
    public void run(@NotNull ProgressIndicator indicator) {
        indicator.setIndeterminate(true);

        TraceableBuildJob buildJob = TraceableBuildJobFactory.newBuildJob(job, configuration,
                paramValueMap, requestManager);

        JobTracker.getInstance().registerJob(buildJob);
        buildJob.run();
    }
}