package org.codinjutsu.tools.jenkins.settings;

import lombok.Builder;
import lombok.Value;
import org.jetbrains.annotations.Nullable;

@Builder
@Value
public class ServerSetting {
    @Nullable String url;
    /**
     * specified in '<a href="http://localhost:8080/jenkins/manage/">http://localhost:8080/jenkins/manage/</a>'
     */
    @Nullable String jenkinsUrl;
    @Nullable String username;
    @Nullable String apiToken;
    @Builder.Default
    boolean apiTokenModified = false;
    int timeout;
}
