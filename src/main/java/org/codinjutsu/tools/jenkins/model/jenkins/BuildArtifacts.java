package org.codinjutsu.tools.jenkins.model.jenkins;

import lombok.Data;

import java.io.Serializable;
import java.util.List;

/**
 * ==========================
 * 开发：wei.wang
 * 创建时间：2024-08-14 15:41
 * 版本：1.0
 * 描述：
 * ==========================
 */
@Data
public class BuildArtifacts implements Serializable {
    /**
     * 作业链接
     */
    private String jobUrl;
    /**
     * 构建编号
     */
    private String buildNumber;
    /**
     * 构建的包
     */
    private List<Artifact> artifacts;

    @Data
    public static class Artifact implements Serializable {
        private String displayPath;
        private String fileName;
        private String relativePath;
    }
}
