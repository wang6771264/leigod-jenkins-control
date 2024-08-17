package org.codinjutsu.tools.jenkins.component;

import lombok.Setter;
import org.codinjutsu.tools.jenkins.logic.RequestManager;
import org.codinjutsu.tools.jenkins.model.jenkins.BuildArtifacts;
import org.codinjutsu.tools.jenkins.model.jenkins.BuildHistory;
import org.codinjutsu.tools.jenkins.model.jenkins.Job;
import org.codinjutsu.tools.jenkins.model.jenkins.ProjectJob;
import org.codinjutsu.tools.jenkins.view.ui.BrowserPanel;

import javax.swing.*;
import java.awt.*;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static org.codinjutsu.tools.jenkins.view.parameter.renderer.CascadeChoiceParameterRenderer.BUILD_VER;
import static org.codinjutsu.tools.jenkins.view.parameter.renderer.CascadeChoiceParameterRenderer.RELATIVE_PATH;

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

    protected List<String> findItems(String parentSelectedItem){
        // 假设根据 parentSelectedItem 获取子选项列表
        if (BUILD_VER.equals(this.id)) {
            return this.findBuildNumber(parentSelectedItem);
        }
        // 构建版本号的子集更新
        if (RELATIVE_PATH.equals(this.id)) {
            return this.findArtifacts(parentSelectedItem);
        }
        return List.of();
    }

    private List<String> findBuildNumber(String parentSelectedItem){
        List<String> items = new ArrayList<>();
        //根据选项获取job名称
        Optional<Job> job = BrowserPanel.getInstance(this.projectJob.getProject())
                .getJob(parentSelectedItem);
        if (job.isPresent()) {
            java.util.List<BuildHistory> buildHistories = RequestManager.getInstance(this.projectJob.getProject()).
                    findRecently5SuccessBuilds(job.get());
            buildHistories.stream().map(BuildHistory::getNumber).forEach(items::add);
        }
        return items;
    }

    private static final String DASH_DEPENDENCIES = "-dependencies/";

    private List<String> findArtifacts(String parentSelectedItem){
        List<String> items = new ArrayList<>();
        //根据选项获取job名称
        String jobName = this.parent.parent.getSelectedItem();
        Optional<Job> job = BrowserPanel.getInstance(this.projectJob.getProject())
                .getJob(jobName);
        if (job.isPresent()) {
            List<BuildArtifacts.Artifact> artifacts = RequestManager
                    .getInstance(this.projectJob.getProject())
                    .findArtifactsByBuildNumber(job.get(), parentSelectedItem);
            artifacts.stream().map(BuildArtifacts.Artifact::getRelativePath)
                    .filter(item -> !item.contains(DASH_DEPENDENCIES)).forEach(items::add);
        }
        return items;
    }

}
