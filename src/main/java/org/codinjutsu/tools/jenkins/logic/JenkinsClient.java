package org.codinjutsu.tools.jenkins.logic;

import com.offbytwo.jenkins.JenkinsServer;
import lombok.Data;
import org.codinjutsu.tools.jenkins.security.JenkinsSecurityClient;
import org.jetbrains.annotations.NotNull;

import java.io.Serializable;
import java.util.function.UnaryOperator;

/**
 * ==========================
 * 开发：wei.wang
 * 创建时间：2024-09-26 11:21
 * 版本：1.0
 * 描述：jenkins客户端信息
 * ==========================
 */
@Data
public class JenkinsClient implements Serializable {
    private String name;
    private String server;
    /**
     * jenkins的安全客户端
     */
    private JenkinsSecurityClient jenkinsSecurityClient;
    /**
     * jenkins的平台
     */
    private @Deprecated JenkinsPlateform jenkinsPlateform = JenkinsPlateform.CLASSIC;
    private @NotNull JenkinsParser jenkinsParser = new JenkinsJsonParser(UnaryOperator.identity());
    /**
     * jenkins的服务器
     */
    private JenkinsServer jenkinsServer;
}
