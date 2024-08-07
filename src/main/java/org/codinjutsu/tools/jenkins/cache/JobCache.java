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

    public static void putIfAbsent(String fullName, Job job) {
        JOB_FULL_NAME_MAPPING.putIfAbsent(fullName, job);
    }

    public static Job get(String fullName) {
        return JOB_FULL_NAME_MAPPING.get(fullName);
    }
}
