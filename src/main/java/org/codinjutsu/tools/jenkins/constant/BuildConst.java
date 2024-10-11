package org.codinjutsu.tools.jenkins.constant;

import com.google.common.collect.Lists;

import java.util.List;

/**
 * ==========================
 * 开发：wei.wang
 * 创建时间：2024-10-11 16:12
 * 版本：1.0
 * 描述：jenkins构建常量
 * ==========================
 */
public interface BuildConst {
    String K8S_ENV = "K8S_ENV";
    String ENV = "ENV";

    String SKIP_TEST = "SKIP_TEST";

    List<String> ENV_LIST = Lists.newArrayList("test", "test1", "prod", "test2");
    String DEFAULT_ENV = "test,test1";
    String SKIP_TEST_VALUE = " -Dmaven.test.skip=true";

    static boolean isEnvProp(String prop) {
        return K8S_ENV.equalsIgnoreCase(prop) || ENV.equals(prop);
    }

}
