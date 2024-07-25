package org.codinjutsu.tools.jenkins.util;

import com.intellij.openapi.util.text.StringUtil;
import lombok.experimental.UtilityClass;
import org.codinjutsu.tools.jenkins.model.jenkins.Job;
import org.codinjutsu.tools.jenkins.model.FavoriteJob;
import org.jetbrains.annotations.NotNull;

@UtilityClass
public class JobUtil {

    @NotNull
    public FavoriteJob createFavoriteJob(@NotNull Job job) {
        return new FavoriteJob(createNameForFavorite(job), job.getUrl());
    }

    public boolean isFavoriteJob(@NotNull Job job, @NotNull FavoriteJob favoriteJob) {
        return isFavoriteJobName(createNameForFavorite(job), favoriteJob) || isFavoriteJobUrl(job.getUrl(), favoriteJob);
    }

    @NotNull
    String createNameForFavorite(@NotNull Job job) {
        return job.getFullName();
    }

    private boolean isFavoriteJobName(@NotNull String jobName, @NotNull FavoriteJob favoriteJob) {
        return StringUtil.equals(jobName, favoriteJob.getName());
    }

    private boolean isFavoriteJobUrl(@NotNull String url, @NotNull FavoriteJob favoriteJob) {
        return StringUtil.equals(url, favoriteJob.getUrl());
    }
}
