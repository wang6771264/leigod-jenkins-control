package org.codinjutsu.tools.jenkins.view.extension;

import com.intellij.openapi.extensions.ExtensionPointName;
import com.intellij.openapi.project.Project;
import org.codinjutsu.tools.jenkins.model.jenkins.Build;
import org.jetbrains.annotations.Nls;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public interface ViewTestResults {

    ExtensionPointName<ViewTestResults> EP_NAME = ExtensionPointName.create("leigod.jenkins.control.viewTestResults");

    @Nullable
    @Nls(capitalization = Nls.Capitalization.Sentence)
    String getDescription();

    boolean canHandle(@NotNull Build build);

    void handle(@NotNull Project project, @NotNull Build build);
}
