package org.codinjutsu.tools.jenkins.extensions;

import com.intellij.dvcs.push.PrePushHandler;
import com.intellij.dvcs.push.PushInfo;
import com.intellij.openapi.progress.ProgressIndicator;
import com.intellij.openapi.project.Project;
import lombok.Data;
import org.jetbrains.annotations.Nls;
import org.jetbrains.annotations.NotNull;

import java.io.Serializable;
import java.util.List;

/**
 * ==========================
 * 开发：wei.wang
 * 创建时间：2024-08-12 10:49
 * 版本：1.0
 * 描述：push之前标记作业
 * ==========================
 */
@Data
public class MarkJobPrePushHandler implements PrePushHandler, Serializable {
    @Override
    public @NotNull @Nls(capitalization = Nls.Capitalization.Title) String getPresentableName() {
        return "";
    }

    @NotNull
    @Override
    public Result handle(@NotNull Project project,
                         @NotNull List<PushInfo> pushDetails,
                         @NotNull ProgressIndicator indicator) {
        System.out.println("MarkJobPrePushHandler");
        return PrePushHandler.super.handle(project, pushDetails, indicator);
    }
}
