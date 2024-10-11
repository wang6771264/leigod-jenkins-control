package org.codinjutsu.tools.jenkins.settings.multiServer;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.util.List;

/**
 * ==========================
 * 开发：wei.wang
 * 创建时间：2024-09-26 10:40
 * 版本：1.0
 * 描述：
 * ==========================
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class MultiServerSettings implements Serializable {

    /**
     * jenkins配置
     */
    private List<MultiJenkinsSettings> settings;

    /**
     * 超时时间
     */
    private int connectedTimeout;
}
