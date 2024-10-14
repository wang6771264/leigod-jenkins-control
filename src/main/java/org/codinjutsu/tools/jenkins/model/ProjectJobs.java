package org.codinjutsu.tools.jenkins.model;

import lombok.Builder;
import lombok.Getter;

import java.io.Serializable;

/**
 * ==========================
 * 开发：wei.wang
 * 创建时间：2024-10-14 13:51
 * 版本：1.0
 * 描述：项目`Jenkins`的`Project`节点
 * ==========================
 */
@Getter
@Builder
public class ProjectJobs implements Serializable {
    /**
     * 节点名称
     */
    private String name;

    /**
     * 节点url
     */
    private String url;
}
