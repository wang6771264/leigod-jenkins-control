package org.codinjutsu.tools.jenkins.state;

import lombok.Data;
import org.codinjutsu.tools.jenkins.model.FavoriteJob;
import org.codinjutsu.tools.jenkins.model.JobBuildConfig;
import org.codinjutsu.tools.jenkins.security.JenkinsVersion;
import org.codinjutsu.tools.jenkins.settings.multiServer.MultiJenkinsSettings;
import org.jetbrains.annotations.NotNull;

import java.io.Serializable;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.function.Predicate;

/**
 * ==========================
 * 开发：wei.wang
 * 创建时间：2024-07-25 22:25
 * 版本：1.0
 * 描述：项目存储类
 * ==========================
 */
@Data
public class ProjectState implements Serializable {
    public static final String RESET_STR_VALUE = "";

    private static final int DEFAULT_CONNECTION_TIMEOUT = 10;

    private String lastSelectedView;

    private List<FavoriteJob> favoriteJobs = new LinkedList<>();
    /**
     * 每个job的构建配置
     */
    private Map<String, JobBuildConfig> jobBuildConfigMapping = new HashMap<>();
    /**
     * 多个jenkins配置
     */
    private List<MultiJenkinsSettings> multiSettings = new LinkedList<>();

    private JenkinsVersion jenkinsVersion = JenkinsVersion.VERSION_1;

    private int connectionTimeout = DEFAULT_CONNECTION_TIMEOUT;

    @NotNull
    private String jenkinsUrl = RESET_STR_VALUE;

    /**
     * jenkins服务配置表格的样式缓存
     */
    private HashMap<String, Integer> serverTableStyle = new HashMap<>();

    public void clearFavoriteJobs() {
        favoriteJobs.clear();
    }

    public void addFavoriteJobs(FavoriteJob favoriteJob) {
        favoriteJobs.add(favoriteJob);
    }

    public void removeFavoriteJob(Predicate<? super FavoriteJob> filter) {
        favoriteJobs.removeIf(filter);
    }
}
