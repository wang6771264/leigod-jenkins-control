package org.codinjutsu.tools.jenkins.component;

import com.intellij.ide.DataManager;
import com.intellij.openapi.actionSystem.DataContext;
import com.intellij.openapi.ui.ComboBox;
import com.intellij.ui.popup.PopupFactoryImpl;
import com.intellij.ui.popup.list.ListPopupImpl;
import org.codinjutsu.tools.jenkins.model.jenkins.ProjectJob;
import org.codinjutsu.tools.jenkins.util.SymbolPool;

import javax.swing.*;
import java.awt.event.ItemEvent;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.util.ArrayList;
import java.util.List;
import java.util.StringJoiner;
import java.util.concurrent.atomic.AtomicReference;

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
    public void initItems(List<String> items, String selectedItem) {
        DefaultComboBoxModel<String> model = (DefaultComboBoxModel<String>) this.selects.getModel();
        model.removeAllElements();
        int index = 0;
        for (int i = 0; i < items.size(); i++) {
            String item = items.get(i);
            if(item.equals(selectedItem)){
                index = i;
            }
            model.addElement(item);
        }
        //添加监听器
        this.addKeyListener(items);
        //添加下拉选项监听
        this.addItemListener();
        //默认将第一个选项选中
        this.selects.setSelectedIndex(-1);
        this.selects.setSelectedIndex(index);
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
        AtomicReference<String> aRefString = new AtomicReference<>(String.join(SymbolPool.COMMA, items));
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
                if (!Character.isLetterOrDigit(keyChar) && SYMBOLS.indexOf(keyChar) == -1
                        && e.getKeyCode() != KeyEvent.VK_BACK_SPACE) {
                    return;
                }
                SwingUtilities.invokeLater(() -> {
                    // 根据输入过滤项
                    List<String> newItems = new ArrayList<>();
                    StringJoiner joiner = new StringJoiner(SymbolPool.COMMA);
                    for (String item : items) {
                        if(item.toLowerCase().contains(searchText.toLowerCase())){
                            newItems.add(item);
                            joiner.add(item);
                        }
                    }
                    textField.setText(searchText);
                    //如果选项没有任何变化就不处理
                    String newItemString = joiner.toString();
                    if(aRefString.get().equals(newItemString)){
                        selects.showPopup();
                        return;
                    }
                    aRefString.set(newItemString);
                    DefaultComboBoxModel<String> model = (DefaultComboBoxModel<String>) selects.getModel();
                    // 清空现有项
                    model.removeAllElements();
                    if (!newItems.isEmpty()) {
                        newItems.forEach(model::addElement);
                        selects.showPopup();
                        // 通知布局管理器重新计算布局
                        selects.revalidate();
                        selects.repaint();
                    }else{
                        //当没有任何选项时也需要更新下级列表
                        if (child != null) {
                            child.cascadeUpdate(null);
                        }
                    }
                    selects.setSelectedIndex(-1);
                    textField.setText(searchText);
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
        List<String> items = new ArrayList<>();
        if (parentSelectedItem != null && !parentSelectedItem.isBlank()) {
            items = this.findItems(parentSelectedItem);
        }
        // 根据 parentSelectedItem 更新 childComboBox 的模型
        DefaultComboBoxModel<String> model = (DefaultComboBoxModel<String>) this.getSelects().getModel();
        model.removeAllElements(); // 清空现有项
        if(!items.isEmpty()){
            items.forEach(model::addElement); // 添加新项
            this.addItemListener();
        }
    }

}