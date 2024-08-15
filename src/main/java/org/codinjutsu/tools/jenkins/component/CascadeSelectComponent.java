package org.codinjutsu.tools.jenkins.component;

import lombok.Setter;
import org.codinjutsu.tools.jenkins.model.jenkins.ProjectJob;

import javax.swing.*;
import java.awt.*;
import java.io.Serializable;

/**
 * ==========================
 * 开发：wei.wang
 * 创建时间：2024-08-14 21:32
 * 版本：1.0
 * 描述：级联单选组件
 * ==========================
 */
public abstract class CascadeSelectComponent implements Serializable {
    /**
     * 支持的符号
     */
    protected static final String SYMBOLS = "!@#$%^&*()_+<>?/.,;:-=[]\\{}|";
    /**
     * 组件的唯一标识
     */
    protected final String id;
    /**
     * 当前组件的上级
     */
    @Setter
    protected CascadeSelectComponent parent;
    /**
     * 当前组件的下级
     */
    protected final CascadeSelectComponent child;
    /**
     * 项目和job的组合实体
     */
    protected final ProjectJob projectJob;

    public CascadeSelectComponent(String id, CascadeSelectComponent child, ProjectJob projectJob) {
        this.id = id;
        this.child = child;
        this.projectJob = projectJob;
    }

    public abstract ItemSelectable getSelects();

    public abstract JComponent getComponent();

    protected abstract String getSelectedItem();

    protected abstract void addItemListener();

    protected abstract void cascadeUpdate(String selectedParentItem);
}
