package org.codinjutsu.tools.jenkins.cache;

import lombok.Data;
import org.codinjutsu.tools.jenkins.model.jenkins.Job;

import java.io.Serializable;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * ==========================
 * 开发：wei.wang
 * 创建时间：2024-08-07 16:48
 * 版本：1.0
 * 描述：
 * ==========================
 */
@Data
public class JobCache implements Serializable {
    private static final Map<String, Job> JOB_FULL_NAME_MAPPING = new ConcurrentHashMap<>();

    private static final Map<String, ConcurrentHashMap<String, String>> JOB_PARAM_DEFAULT_VALUE = new ConcurrentHashMap<>();

    public static void addParamDefault(String jobName, String paramName, String value) {
        JOB_PARAM_DEFAULT_VALUE.putIfAbsent(jobName, new ConcurrentHashMap<>());
        JOB_PARAM_DEFAULT_VALUE.get(jobName).put(paramName, value);
    }

    public static String getParamRecentlyValue(String jobName, String paramName) {
        ConcurrentHashMap<String, String> paramMap = JOB_PARAM_DEFAULT_VALUE.get(jobName);
        return paramMap == null ? null : paramMap.get(paramName);
    }

    public static void putIfAbsent(String fullName, Job job) {
        JOB_FULL_NAME_MAPPING.putIfAbsent(fullName, job);
    }

    public static Job get(String fullName) {
        return JOB_FULL_NAME_MAPPING.get(fullName);
    }
}
