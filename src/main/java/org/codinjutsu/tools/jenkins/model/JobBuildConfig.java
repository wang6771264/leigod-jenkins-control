package org.codinjutsu.tools.jenkins.model;

import com.intellij.util.xmlb.annotations.Attribute;
import com.intellij.util.xmlb.annotations.Tag;
import lombok.Data;

import java.io.Serializable;
import java.util.Map;

/**
 * ==========================
 * 开发：wei.wang
 * 创建时间：2024-07-25 22:45
 * 版本：1.0
 * 描述：job的构建配置
 * ==========================
 */
@Data
@Tag("jobBuildConfig")
public class JobBuildConfig implements Serializable {
    /**
     * 配置名称
     */
    @Attribute("configName")
    private String name;
    @Attribute("jobUrl")
    private String jobUrl;
    @Attribute("jobUrl")
    private String jobName;
    @Attribute("params")
    private Map<String, String> paramAndValue;

    @Data
    @Tag("param")
    public static class ParamValue implements Serializable {
        /**
         * 配置的key
         */
        @Attribute("key")
        private String key;
        /**
         * 配置的value
         */
        @Attribute("value")
        private String value;

        private Integer type;
    }
}
