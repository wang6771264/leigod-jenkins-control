package org.codinjutsu.tools.jenkins.model.jenkins;

import com.intellij.openapi.project.Project;
import lombok.Builder;
import lombok.Value;
import org.jetbrains.annotations.NotNull;

@Value
@Builder(toBuilder = true)
public class ProjectJob {

    @NotNull
    private Project project;

    @NotNull
    private Job job;

    @NotNull
    private Build lastBuild;
}
