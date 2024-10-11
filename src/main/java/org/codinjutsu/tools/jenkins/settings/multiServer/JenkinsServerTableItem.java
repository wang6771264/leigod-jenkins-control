package org.codinjutsu.tools.jenkins.settings.multiServer;

import lombok.Data;
import org.apache.commons.lang.StringUtils;
import org.codinjutsu.tools.jenkins.settings.ServerSetting;

import java.io.Serializable;

/**
 * ==========================
 * 开发：wei.wang
 * 创建时间：2024-09-25 14:01
 * 版本：1.0
 * 描述：
 * ==========================
 */
@Data
public class JenkinsServerTableItem implements Serializable {
    /**
     * 名称
     */
    private String name = StringUtils.EMPTY;

    /**
     * jenkins服务器
     */
    private String jenkinsServer = StringUtils.EMPTY;
    /**
     * jenkins的链接
     */
    private String jenkinsUrl = StringUtils.EMPTY;
    /**
     * 登录用户名
     */
    private String username = StringUtils.EMPTY;
    /**
     * ApiToken
     */
    private String apiToken = StringUtils.EMPTY;

    public ServerSetting toServerSetting(){
        return ServerSetting.builder()
                .url(this.getJenkinsUrl())
                .jenkinsUrl(this.getJenkinsServer())
                .username(this.getUsername())
                .apiToken(this.getApiToken())
                .build();
    }
}
