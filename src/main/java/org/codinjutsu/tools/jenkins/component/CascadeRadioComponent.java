package org.codinjutsu.tools.jenkins.component;

import org.codinjutsu.tools.jenkins.model.jenkins.ProjectJob;

import javax.swing.*;
import java.awt.event.ItemEvent;
import java.util.ArrayList;
import java.util.List;

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
        if (items.isEmpty()) {
            selects.setSelectedIndex(-1);
            if (this.child != null) {
                this.child.cascadeUpdate(null);
            }
        } else {
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
        }
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
        List<String> items = new ArrayList<>();
        if (parentSelectedItem != null && !parentSelectedItem.isBlank()) {
            items = this.findItems(parentSelectedItem);
        }
        // 清空现有项
        RadioButtonGroup radioButtonGroup = this.getSelects();
        radioButtonGroup.removeAll();
        //添加全部选项
        this.addAll(items);
    }
}