package org.codinjutsu.tools.jenkins.example;

import com.intellij.openapi.ui.ComboBox;
import org.codinjutsu.tools.jenkins.logic.RequestManager;
import org.codinjutsu.tools.jenkins.model.jenkins.BuildArtifacts;
import org.codinjutsu.tools.jenkins.model.jenkins.BuildHistory;
import org.codinjutsu.tools.jenkins.model.jenkins.Job;
import org.codinjutsu.tools.jenkins.model.jenkins.ProjectJob;
import org.codinjutsu.tools.jenkins.view.ui.BrowserPanel;

import javax.swing.*;
import java.awt.event.ItemEvent;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.util.List;
import java.util.Optional;

import static org.codinjutsu.tools.jenkins.view.parameter.renderer.CascadeChoiceParameterRenderer.BUILD_VER;
import static org.codinjutsu.tools.jenkins.view.parameter.renderer.CascadeChoiceParameterRenderer.JOB_NAME;

public class CascadeSearchableComboBox extends CascadeSelectComponent {
    /**
     * 输入框下拉列表
     */
    private final SelectComboBox selects;

    public CascadeSearchableComboBox(String id, ProjectJob projectJob, CascadeSelectComponent child) {
        super(id, child, projectJob);
        this.selects = initItemSelectable();
    }

    /**
     * 初始化model数据
     *
     * @param items
     */
    public void initItems(List<String> items) {
        DefaultComboBoxModel<String> model = (DefaultComboBoxModel<String>) this.selects.getModel();
        model.removeAllElements();
        items.forEach(model::addElement);
        //添加监听器
        this.addKeyListener(items);
        //添加下拉选项监听
        this.addItemListener();
        //默认将第一个选项选中
        this.selects.setSelectedIndex(-1);
        this.selects.setSelectedIndex(0);
    }

    @Override
    public ComboBox<String> getSelects() {
        return this.selects;
    }

    @Override
    public JComponent getComponent() {
        return this.selects;
    }

    @Override
    protected String getSelectedItem() {
        return (String) this.selects.getSelectedItem();
    }

    private SelectComboBox initItemSelectable() {
        SelectComboBox selects = new SelectComboBox(new DefaultComboBoxModel<>());
        selects.setEditable(true);
        return selects;
    }

    private void addKeyListener(List<String> items) {
        // 监听输入框的变化
        // 获取编辑组件
        JTextField textField = (JTextField) this.selects.getEditor().getEditorComponent();
        textField.addKeyListener(new KeyAdapter() {
            @Override
            public void keyReleased(KeyEvent e) {
                String searchText = textField.getText().toLowerCase();
                if (searchText.isBlank()) {
                    return;
                }
                char keyChar = e.getKeyChar();
                // 既不是数字字母也不是符号条件的符号直接返回
                if (!Character.isLetterOrDigit(keyChar) && SYMBOLS.indexOf(keyChar) == -1) {
                    return;
                }
                DefaultComboBoxModel<String> model = (DefaultComboBoxModel<String>) selects.getModel();
                SwingUtilities.invokeLater(() -> {
                    // 清空现有项
                    model.removeAllElements();

                    // 根据输入过滤项
                    items.stream().filter(item -> item.toLowerCase().contains(searchText))
                            .forEach(model::addElement);
                    if (model.getSize() > 0) {
                        selects.setSelectedIndex(-1);
                        selects.showPopup();
                        textField.setText(searchText);
                        // 通知布局管理器重新计算布局
                        selects.revalidate();
                        selects.repaint();
                    }
                });
            }
        });
    }

    @Override
    protected void addItemListener() {
        this.selects.addItemListener(e -> {
            if (e.getStateChange() == ItemEvent.SELECTED) {
                this.selects.setToolTipText((String) e.getItem());
                // 根据父级联下拉列表的选中项更新子级联下拉列表的选项
                if (child != null) {
                    child.cascadeUpdate((String) e.getItem());
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
        // 根据 parentSelectedItem 更新 childComboBox 的模型
        DefaultComboBoxModel<String> model = (DefaultComboBoxModel<String>) this.getSelects().getModel();
        model.removeAllElements(); // 清空现有项
        // 假设根据 parentSelectedItem 获取子选项列表
        if (JOB_NAME.equals(this.id)) {
            //根据选项获取job名称
            Optional<Job> job = BrowserPanel.getInstance(this.projectJob.getProject()).getJob(parentSelectedItem);
            if (job.isPresent()) {
                List<BuildHistory> buildHistories = RequestManager.getInstance(this.projectJob.getProject()).
                        findRecently5SuccessBuilds(job.get());
                buildHistories.stream().map(BuildHistory::getNumber).forEach(model::addElement);
                this.addItemListener();
            }
        }
        // 构建版本号的子集更新
        if (BUILD_VER.equals(this.id)) {
            //根据选项获取job名称
            String jobName = this.parent.parent.getSelectedItem();
            Optional<Job> job = BrowserPanel.getInstance(this.projectJob.getProject())
                    .getJob(jobName);
            if (job.isPresent()) {
                List<BuildArtifacts.Artifact> artifacts = RequestManager
                        .getInstance(this.projectJob.getProject())
                        .findArtifactsByBuildNumber(job.get(), parentSelectedItem);
                artifacts.stream().map(BuildArtifacts.Artifact::getRelativePath)
                        .forEach(model::addElement);
                this.addItemListener();
            }
        }
    }

}