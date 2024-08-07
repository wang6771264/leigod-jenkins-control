package org.codinjutsu.tools.jenkins.model.jenkins;

import lombok.Data;

import java.io.Serializable;

/**
 * ==========================
 * 开发：wei.wang
 * 创建时间：2024-08-07 17:37
 * 版本：1.0
 * 描述：构建历史对象
 * ==========================
 */
@Data
public class BuildHistory implements Serializable {
    /**
     * class类
     */
    private String _class;

    /**
     * 构建编号
     */
    private String number;

    /**
     * 构建结果
     */
    private String result;

    public String toDepoly(String name){
        return name + "#" + number;
    }
}
