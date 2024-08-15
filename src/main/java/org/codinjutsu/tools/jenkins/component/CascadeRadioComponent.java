package org.codinjutsu.tools.jenkins.component;

import org.codinjutsu.tools.jenkins.logic.RequestManager;
import org.codinjutsu.tools.jenkins.model.jenkins.BuildArtifacts;
import org.codinjutsu.tools.jenkins.model.jenkins.BuildHistory;
import org.codinjutsu.tools.jenkins.model.jenkins.Job;
import org.codinjutsu.tools.jenkins.model.jenkins.ProjectJob;
import org.codinjutsu.tools.jenkins.view.ui.BrowserPanel;

import javax.swing.*;
import java.awt.event.ItemEvent;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static org.codinjutsu.tools.jenkins.view.parameter.renderer.CascadeChoiceParameterRenderer.BUILD_VER;
import static org.codinjutsu.tools.jenkins.view.parameter.renderer.CascadeChoiceParameterRenderer.RELATIVE_PATH;

public class CascadeRadioComponent extends CascadeSelectComponent {
    private final RadioButtonGroup selects;

    public CascadeRadioComponent(String id, List<String> items,
                                 ProjectJob projectJob, CascadeSelectComponent childComboBox) {
        this(id, items, BoxLayout.Y_AXIS, projectJob, childComboBox);
    }

    public CascadeRadioComponent(String id, List<String> items, Integer layout,
                                 ProjectJob projectJob, CascadeSelectComponent childComboBox) {
        super(id, childComboBox, projectJob);
        this.selects = new RadioButtonGroup(items, layout);
        initItemSelectable(items);
        //添加下拉选项监听
        if (childComboBox != null) {
            this.addItemListener();
        }
    }

    @Override
    public RadioButtonGroup getSelects() {
        return this.selects;
    }

    @Override
    public JComponent getComponent() {
        return this.selects;
    }

    @Override
    protected String getSelectedItem() {
        return this.selects.getSelectedItem();
    }

    private void initItemSelectable(List<String> items) {
        this.addAll(items);
    }

    private void addAll(List<String> items) {
        for (String item : items) {
            selects.addElement(item, e -> {
                if (e.getStateChange() == ItemEvent.SELECTED) {
                    String text = ((JRadioButton) e.getItem()).getText();
                    this.selects.setToolTipText(text);
                    // 根据父级联下拉列表的选中项更新子级联下拉列表的选项
                    if (child != null) {
                        child.cascadeUpdate(text);
                    }
                }
            });
        }
        selects.setSelectedIndex(0);
        selects.updateUI();
    }

    @Override
    protected void addItemListener() {
        this.selects.addItemListener(e -> {
            if (e.getStateChange() == ItemEvent.SELECTED) {
                String text = ((JRadioButton) e.getItem()).getText();
                this.selects.setToolTipText(text);
                // 根据父级联下拉列表的选中项更新子级联下拉列表的选项
                if (child != null) {
                    child.cascadeUpdate(text);
                }
            }
        });
    }

    /**
     * 根据父级联下拉列表的选中项更新子级联下拉列表的选项
     *
     * @param parentSelectedItem 父级联下拉列表的选中项
     */
    @Override
    protected void cascadeUpdate(String parentSelectedItem) {
        if (parentSelectedItem == null || parentSelectedItem.isBlank()) {
            return;
        }
        // 清空现有项
        RadioButtonGroup radioButtonGroup = this.getSelects();
        radioButtonGroup.removeAll();
        List<String> items = new ArrayList<>();
        // 假设根据 parentSelectedItem 获取子选项列表
        if (BUILD_VER.equals(this.id)) {
            //根据选项获取job名称
            Optional<Job> job = BrowserPanel.getInstance(this.projectJob.getProject())
                    .getJob(parentSelectedItem);
            if (job.isPresent()) {
                List<BuildHistory> buildHistories = RequestManager.getInstance(this.projectJob.getProject()).
                        findRecently5SuccessBuilds(job.get());
                buildHistories.stream().map(BuildHistory::getNumber).forEach(items::add);
            }
        }
        // 构建版本号的子集更新
        if (RELATIVE_PATH.equals(this.id)) {
            //根据选项获取job名称
            String jobName = this.parent.parent.getSelectedItem();
            Optional<Job> job = BrowserPanel.getInstance(this.projectJob.getProject())
                    .getJob(jobName);
            if (job.isPresent()) {
                List<BuildArtifacts.Artifact> artifacts = RequestManager
                        .getInstance(this.projectJob.getProject())
                        .findArtifactsByBuildNumber(job.get(), parentSelectedItem);
                artifacts.stream().map(BuildArtifacts.Artifact::getRelativePath).forEach(items::add);
            }
        }
        //添加全部选项
        this.addAll(items);
    }
}