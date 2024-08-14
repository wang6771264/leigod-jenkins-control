package org.codinjutsu.tools.jenkins.extensions;

import com.intellij.dvcs.push.PrePushHandler;
import com.intellij.dvcs.push.PushInfo;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.module.Module;
import com.intellij.openapi.module.ModuleManager;
import com.intellij.openapi.progress.ProgressIndicator;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.roots.ModuleRootManager;
import com.intellij.openapi.vfs.VirtualFile;
import lombok.Data;
import org.jetbrains.annotations.Nls;
import org.jetbrains.annotations.NotNull;

import java.io.Serializable;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

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

    private Logger logger = Logger.getInstance(MarkJobPrePushHandler.class);
    /**
     * 模块名称和仓库名称的映射
     */
    private Map<String, String> MODULE_REPO_NAME_MAPPING = new HashMap<>();

    @Override
    public @NotNull @Nls(capitalization = Nls.Capitalization.Title) String getPresentableName() {
        return "";
    }

    @NotNull
    @Override
    public Result handle(@NotNull Project project,
                         @NotNull List<PushInfo> pushDetails,
                         @NotNull ProgressIndicator indicator) {
        if (pushDetails.isEmpty()) {
            return Result.ABORT;
        }
        logger.info("before push:" + project.getName());
        Module[] modules = ModuleManager.getInstance(project).getModules();
        for (Module module : modules) {
            String name = module.getName();
            // 获取模块的文件目录
            VirtualFile contentRoot0 = ModuleRootManager.getInstance(module).getContentRoots()[0];
            //添加模块的root路径和模块名称的映射,方便后续使用module名称获取对应的job_name
            MODULE_REPO_NAME_MAPPING.put(contentRoot0.getUrl(), name);
        }

        logger.info("mark job of project new" + project.getName());
        logger.info("after push:" + project.getName());
        return Result.ABORT;
    }
}
