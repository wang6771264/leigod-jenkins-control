package org.codinjutsu.tools.jenkins.view.parameter.renderer;

import com.intellij.openapi.vfs.VirtualFile;
import org.codinjutsu.tools.jenkins.model.jenkins.JobParameter;
import org.codinjutsu.tools.jenkins.model.jenkins.JobParameterType;
import org.codinjutsu.tools.jenkins.model.jenkins.ProjectJob;
import org.codinjutsu.tools.jenkins.view.extension.JobParameterRenderer;
import org.codinjutsu.tools.jenkins.view.extension.JobParameterRenderers;
import org.codinjutsu.tools.jenkins.view.parameter.JobParameterComponent;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class PatchParameterRenderer extends AbstractParameterRenderer implements JobParameterRenderer {

    public static final JobParameterType TYPE = new JobParameterType("PatchParameterDefinition",
            "org.jenkinsci.plugins.patch.PatchParameterDefinition");

    @NotNull
    @Override
    public JobParameterComponent<VirtualFile> getJobParameterComponent(@NotNull JobParameter jobParameter,
                                                                       @Nullable ProjectJob projectJob,
                                                                       String defaultValue) {
        return JobParameterRenderers.createFileUpload(jobParameter, defaultValue);
    }

    @Override
    public boolean isForJobParameter(@NotNull JobParameter jobParameter) {
        return TYPE.equals(jobParameter.getJobParameterType());
    }
}
