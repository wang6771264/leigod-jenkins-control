package org.codinjutsu.tools.jenkins.settings.multiServer;

import com.intellij.util.xmlb.annotations.Attribute;
import com.intellij.util.xmlb.annotations.Tag;
import lombok.*;

@Data
@Tag("jenkins")
@AllArgsConstructor
@NoArgsConstructor
public class MultiJenkinsSettings {
    /**
     * 名称
     */
    @Setter(value = AccessLevel.NONE)
    @Attribute("name")
    private String name;

    /**
     * jenkins服务器
     */
    @Setter(value = AccessLevel.NONE)
    @Attribute("jenkinsServer")
    private String jenkinsServer;
    /**
     * jenkins的链接
     */
    @Setter(value = AccessLevel.NONE)
    @Attribute("jenkinsUrl")
    private String jenkinsUrl;
    /**
     * 登录用户名
     */
    @Setter(value = AccessLevel.NONE)
    @Attribute("username")
    private String username;
    /**
     * ApiToken
     */
    @Setter(value = AccessLevel.NONE)
    @Attribute("apiToken")
    private String apiToken;

    /**
     * 是否有效
     * //fixme 通过这个字段可以判断数据是否有效
     */
    private final boolean isValid = true;

    public MultiJenkinsSettings(JenkinsServerTableItem tableItem) {
        this.name = tableItem.getName();
        this.jenkinsServer = tableItem.getJenkinsServer();
        this.jenkinsUrl = tableItem.getJenkinsUrl();
        this.username = tableItem.getUsername();
        this.apiToken = tableItem.getApiToken();
    }

    public JenkinsServerTableItem toJenkinsServerTableItem() {
        JenkinsServerTableItem item = new JenkinsServerTableItem();
        item.setName(this.name);
        item.setJenkinsServer(this.jenkinsServer);
        item.setJenkinsUrl(this.jenkinsUrl);
        item.setUsername(this.username);
        item.setApiToken(this.apiToken);
        return item;
    }
}