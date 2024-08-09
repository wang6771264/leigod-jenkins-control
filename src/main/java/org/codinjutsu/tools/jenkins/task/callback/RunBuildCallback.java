package org.codinjutsu.tools.jenkins.task.callback;

import org.codinjutsu.tools.jenkins.model.jenkins.Job;

public interface RunBuildCallback {

    void notifyOnOk(Job job);

    void notifyOnError(Job job, Throwable ex);
}