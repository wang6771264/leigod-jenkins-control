package org.codinjutsu.tools.jenkins.extensions;

import com.intellij.dvcs.push.PrePushHandler;
import com.intellij.dvcs.push.PushInfo;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.module.Module;
import com.intellij.openapi.module.ModuleManager;
import com.intellij.openapi.progress.ProgressIndicator;
import com.intellij.openapi.project.Project;
import git4idea.repo.GitRemote;
import git4idea.repo.GitRepositoryImpl;
import lombok.Data;
import org.codinjutsu.tools.jenkins.cache.JobCache;
import org.jetbrains.annotations.Nls;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.idea.maven.model.MavenId;
import org.jetbrains.idea.maven.project.MavenProject;
import org.jetbrains.idea.maven.project.MavenProjectsManager;

import java.io.Serializable;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

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
    private Map<String, String> MODULE_NAME_REPO_MAPPING = new HashMap<>();

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
        for (PushInfo pushDetail : pushDetails) {
            Optional<GitRemote> first = ((GitRepositoryImpl) pushDetail.getRepository())
                    .getInfo().getRemotes().stream().findFirst();
            if (first.isEmpty()) {
                continue;
            }
            GitRemote gitRemote = first.get();
            Optional<String> pushUrlOpt = gitRemote.getPushUrls().stream().findFirst();
            if (pushUrlOpt.isEmpty()) {
                continue;
            }
            String pushUrl = pushUrlOpt.get();
            String repoName = pushUrl.substring(pushUrl.lastIndexOf("/") + 1).replace(".git", "");
            //找到对应的构建作业,标记为需要构建的job
            JobCache.addNeedBuild(repoName);
        }
        logger.info("mark job of project new" + project.getName());
        return Result.OK;
    }

    /**
     * 获取maven的最上级模块
     *
     * @param module
     * @return
     */
    public Module getTopLevelMavenModule(Module module) {
        Project project = module.getProject();
        Module currentModule = module;

        while (true) {
            MavenProjectsManager mavenProjectsManager = MavenProjectsManager.getInstance(project);
            MavenProject mavenProject = mavenProjectsManager.findProject(currentModule);

            if (mavenProject != null) {
                // 获取父 POM 的信息
                MavenId parentId = mavenProject.getParentId();

                if (parentId != null) {
                    // 查找父模块
                    Module parentModule = findModuleByMavenId(project, parentId.getGroupId(), parentId.getArtifactId(), parentId.getVersion());
                    if (parentModule != null) {
                        // 更新当前模块为父模块
                        currentModule = parentModule;
                    } else {
                        // 找不到父模块，停止查找
                        break;
                    }
                } else {
                    // 没有父模块，停止查找
                    break;
                }
            } else {
                // 找不到 Maven 项目信息，停止查找
                break;
            }
        }
        // 返回最上级模块
        return currentModule;
    }

    private Module findModuleByMavenId(Project project, String groupId, String artifactId, String version) {
        Module[] allModules = ModuleManager.getInstance(project).getModules();
        for (Module mod : allModules) {
            MavenProjectsManager mavenProjectsManager = MavenProjectsManager.getInstance(project);
            MavenProject mavenProject = mavenProjectsManager.findProject(mod);
            if (mavenProject != null) {
                mavenProject.getMavenId();
                MavenId mavenId = mavenProject.getMavenId();
                if (groupId.equals(mavenId.getGroupId()) &&
                        artifactId.equals(mavenId.getArtifactId()) &&
                        version.equals(mavenId.getVersion())) {
                    return mod;
                }
            }
        }
        return null;
    }
}
