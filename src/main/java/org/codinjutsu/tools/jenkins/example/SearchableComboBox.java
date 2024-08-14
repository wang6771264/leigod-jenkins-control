package org.codinjutsu.tools.jenkins.example;

import com.intellij.openapi.ui.ComboBox;
import lombok.Getter;
import lombok.Setter;
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

@Getter
public class SearchableComboBox {

    private static final List<Character> symbols = List.of('_', '-');

    private final String id;
    private final ComboBox<String> selects;
    @Setter
    private SearchableComboBox parentComboBox;
    private final SearchableComboBox childComboBox;
    private final ProjectJob projectJob;

    public SearchableComboBox(String id, List<String> items,
                              ProjectJob projectJob, SearchableComboBox childComboBox) {
        this.id = id;
        this.projectJob = projectJob;
        this.selects = new ComboBox<>(new DefaultComboBoxModel<>());
        this.selects.setEditable(true);
        this.childComboBox = childComboBox;
        for (String item : items) {
            this.selects.addItem(item);
        }
        //添加输入框的变化
        this.addKeyListener(items);
        //添加下拉选项监听
        if (childComboBox != null) {
            this.addItemListener();
        }
    }

    private void addKeyListener(List<String> items) {
        // 监听输入框的变化
        // 获取编辑组件
        JTextField textField = (JTextField) this.selects.getEditor().getEditorComponent();
        textField.addKeyListener(new KeyAdapter() {
            @Override
            public void keyReleased(KeyEvent e) {
                String searchText = textField.getText().toLowerCase();
                DefaultComboBoxModel<String> model = (DefaultComboBoxModel<String>) selects.getModel();
                if (searchText.isBlank()) {
                    model.removeAllElements(); // 清空现有项
                    for (String item : items) {
                        model.addElement(item);
                    }
                    return;
                }
                char keyChar = e.getKeyChar();
                // 检查字符是否是字母或数字
                if (!Character.isLetterOrDigit(keyChar) && !symbols.contains(keyChar)) {
                    return;
                }
                SwingUtilities.invokeLater(() -> {
                    model.removeAllElements(); // 清空现有项

                    // 根据输入过滤项
                    items.stream().filter(item -> item.toLowerCase().contains(searchText))
                            .forEach(model::addElement);

                    if (model.getSize() > 0) {
                        selects.showPopup();
                    }
                });
            }
        });
    }

    private void addItemListener() {
        if (childComboBox == null) {
            return;
        }
        this.selects.addItemListener(e -> {
            if (e.getStateChange() == ItemEvent.SELECTED) {
                // 根据父级联下拉列表的选中项更新子级联下拉列表的选项
                String selectedParentItem = (String) e.getItem();
                updateChildComboBox(childComboBox.getSelects(), selectedParentItem);
            }
        });
    }

    /**
     * 根据父级联下拉列表的选中项更新子级联下拉列表的选项
     *
     * @param childComboBox      子列表组件
     * @param parentSelectedItem 父级联下拉列表的选中项
     */
    private void updateChildComboBox(ComboBox<String> childComboBox, String parentSelectedItem) {
        if (parentSelectedItem == null || parentSelectedItem.isBlank()) {
            return;
        }
        // 根据 parentSelectedItem 更新 childComboBox 的模型
        DefaultComboBoxModel<String> model = (DefaultComboBoxModel<String>) childComboBox.getModel();
        model.removeAllElements(); // 清空现有项
        // 假设根据 parentSelectedItem 获取子选项列表
        if (JOB_NAME.equals(this.id)) {
            //根据选项获取job名称
            Optional<Job> job = BrowserPanel.getInstance(this.projectJob.getProject()).getJob(parentSelectedItem);
            if (job.isPresent()) {
                List<BuildHistory> buildHistories = RequestManager.getInstance(this.projectJob.getProject()).
                        findRecently5SuccessBuilds(job.get());
                buildHistories.stream().map(BuildHistory::getNumber).forEach(model::addElement);
            }
        }
        // 构建版本号的子集更新
        if (BUILD_VER.equals(this.id)) {
            //根据选项获取job名称
            String jobName = (String) parentComboBox.getSelects().getSelectedItem();
            Optional<Job> job = BrowserPanel.getInstance(this.projectJob.getProject())
                    .getJob(jobName);
            if (job.isPresent()) {
                List<BuildArtifacts.Artifact> artifacts = RequestManager
                        .getInstance(this.projectJob.getProject())
                        .findArtifactsByBuildNumber(job.get(), parentSelectedItem);
                artifacts.stream().map(BuildArtifacts.Artifact::getRelativePath)
                        .forEach(model::addElement);
            }
        }
    }

}