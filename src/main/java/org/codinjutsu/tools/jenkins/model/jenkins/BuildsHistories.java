package org.codinjutsu.tools.jenkins.model.jenkins;

import lombok.Data;

import java.io.Serializable;
import java.util.List;

/**
 * ==========================
 * 开发：wei.wang
 * 创建时间：2024-08-07 17:56
 * 版本：1.0
 * 描述：构建历史
 * ==========================
 */
@Data
public class BuildsHistories implements Serializable {
    /**
     * 构建历史
     */
    private List<BuildHistory> builds;
}
