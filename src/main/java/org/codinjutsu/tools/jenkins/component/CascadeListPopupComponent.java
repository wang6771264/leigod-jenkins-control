package org.codinjutsu.tools.jenkins.component;

import com.intellij.openapi.ui.ComboBox;
import com.intellij.openapi.ui.popup.JBPopupFactory;
import com.intellij.openapi.ui.popup.ListPopup;
import com.intellij.openapi.ui.popup.PopupStep;
import com.intellij.openapi.ui.popup.util.BaseListPopupStep;
import com.intellij.ui.components.JBTextField;
import com.intellij.ui.popup.list.ListPopupImpl;
import org.codinjutsu.tools.jenkins.model.jenkins.ProjectJob;
import org.jetbrains.annotations.Nullable;

import javax.swing.*;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.ArrayList;
import java.util.List;

/**
 * 使用 IntelliJ IDEA 原生的 ListPopup 实现级联搜索组件
 */
public class CascadeListPopupComponent extends CascadeSelectComponent {

    private final JBTextField textField;
    // 新增：屏蔽标志位
    private volatile boolean isUpdatingProgrammatically = false; 
    private List<String> allItems;
    private String selectedValue;
    private ListPopup currentPopup;

    public CascadeListPopupComponent(String id, ProjectJob projectJob, CascadeSelectComponent child) {
        super(id, child, projectJob);
        this.textField = new JBTextField();
        this.allItems = new ArrayList<>();
        initTextField();
    }

    /**
     * 初始化文本框
     */
    private void initTextField() {
        textField.setEditable(true);
        textField.setPreferredSize(new Dimension(200, 30));
        textField.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                showPopup("");
            }
        });
    }

    /**
     * 初始化数据项
     */
    public void initItems(List<String> items, String selectedItem) {
        this.allItems = new ArrayList<>(items);
        this.selectedValue = selectedItem;

        if (selectedItem != null && !selectedItem.isEmpty()) {
            setSelectedItem(selectedValue);
            // 触发级联更新
            if (child != null) {
                child.cascadeUpdate(selectedValue);
            }
        } else if (!items.isEmpty()) {
            // 默认选中第一项
            setSelectedItem(items.get(0));
            showPopup("");
        }
    }

    /**
     * 显示弹出搜索列表
     */
    private void showPopup(String prefix) {
        if (allItems.isEmpty()) {
            return;
        }
        //前置值是空的后者选中则不处理
        if (prefix == null || isUpdatingProgrammatically) {
            return;
        }

        // 创建支持搜索的 ListPopup
        BaseListPopupStep<String> step = new BaseListPopupStep<>(null, allItems) {
            @Override
            public @Nullable PopupStep<?> onChosen(String selectedValue, boolean finalChoice) {
                return doFinalStep(() -> {
                    setSelectedItem(selectedValue);
                    // 触发级联更新
                    if (child != null) {
                        child.cascadeUpdate(selectedValue);
                    }
                });
            }

            @Override
            public boolean isSpeedSearchEnabled() {
                return true;
            }

            @Override
            public boolean isAutoSelectionEnabled() {
                return false;
            }
            
            @Override
            public boolean hasSubstep(String selectedValue) {
                return false;
            }
        };

        currentPopup = JBPopupFactory.getInstance().createListPopup(step);
        ((ListPopupImpl) currentPopup).getSpeedSearch().updatePattern(prefix);
        // 在文本框下方显示弹出列表
        currentPopup.showUnderneathOf(textField);
    }

    /**
     * 设置选中项
     */
    private void setSelectedItem(String item) {
        this.selectedValue = item;
        this.isUpdatingProgrammatically = true;
        this.textField.setText(item);
        this.textField.setToolTipText(item);
        this.isUpdatingProgrammatically = false;
    }

    @Override
    public JComponent getComponent() {
        return textField;
    }

    @Override
    protected String getSelectedItem() {
        return selectedValue;
    }

    @Override
    public ComboBox<String> getSelects() {
        // 不再使用 ComboBox，返回 null
        return null;
    }

    @Override
    protected void addItemListener() {
        // 选择逻辑已在 showPopup 的 onChosen 中处理
    }

    @Override
    protected void cascadeUpdate(String parentSelectedItem) {
        List<String> items = new ArrayList<>();
        if (parentSelectedItem != null && !parentSelectedItem.isBlank()) {
            items = this.findItems(parentSelectedItem);
        }

        this.allItems = items;

        // 清空当前选择
        this.selectedValue = null;
        this.textField.setText("");

        // 如果有新的选项，可以默认选中第一个
        if (!items.isEmpty()) {
            setSelectedItem(items.get(0));
            // 触发子级联更新
            if (child != null) {
                child.cascadeUpdate(items.get(0));
            }
        } else {
            // 如果没有选项，通知子级联清空
            if (child != null) {
                child.cascadeUpdate(null);
            }
        }
    }

    /**
     * 获取当前所有项
     */
    public List<String> getAllItems() {
        return new ArrayList<>(allItems);
    }

    /**
     * 关闭当前弹出窗口
     */
    public void closePopup() {
        if (currentPopup != null && currentPopup.isVisible()) {
            currentPopup.cancel();
        }
    }
}