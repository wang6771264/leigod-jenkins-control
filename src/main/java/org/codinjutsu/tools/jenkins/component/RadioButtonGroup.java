package org.codinjutsu.tools.jenkins.component;

import com.intellij.ui.components.JBRadioButton;
import com.intellij.ui.components.JBScrollPane;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicBoolean;

public class RadioButtonGroup extends JComponent implements ItemSelectable, SelectComponent, Serializable {
    private final ButtonGroup buttonGroup = new ButtonGroup();
    private final List<JBRadioButton> selects = new ArrayList<>();
    private final JBScrollPane scrollPane = new JBScrollPane();
    private final JPanel panel = new JPanel();
    private int selectedIndex = -1;
    private String selectItem = "";

    public RadioButtonGroup(List<String> items) {
        this(items, BoxLayout.Y_AXIS);
    }

    public RadioButtonGroup(List<String> items, Integer layout) {
        this(items, layout, null, null);
    }

    public RadioButtonGroup(List<String> items, Integer layout,
                            Integer selectedIndex, String selectItem) {
        this.selectedIndex = Optional.ofNullable(selectedIndex).orElse(this.selectedIndex);
        this.selectItem = Optional.ofNullable(selectItem).orElse(this.selectItem);

        panel.setLayout(new BoxLayout(panel, layout)); // 水平布局
        scrollPane.setViewportView(panel);
        items.forEach(this::addElement);

        // 将JPanel添加到当前组件
        this.setLayout(new BorderLayout());
        this.add(panel, BorderLayout.CENTER);

        // 通知布局管理器重新计算布局
        this.setPreferredSize(new Dimension(-1, 150));
        this.setMinimumSize(new Dimension(-1, 150));
        revalidate();
        repaint();
    }

    public void addElement(String text) {
        this.addElement(text, null);
    }

    public void addElement(String text, ItemListener listener) {
        JBRadioButton radio = new JBRadioButton(text);
        if (listener != null) {
            radio.addItemListener(listener);
        }
        buttonGroup.add(radio);
        selects.add(radio);
        panel.add(radio);
    }

    /**
     * 更新UI
     */
    public void updateUI(){
        // 通知布局管理器重新计算布局
        revalidate();
        repaint();
    }

    @Override
    public Object[] getSelectedObjects() {
        if (selectedIndex != -1) {
            return new Object[]{selectItem};
        }
        return null;
    }

    public String getSelectedItem() {
        return this.selectItem;
    }

    @Override
    public String selectedItem() {
        return this.selectItem;
    }

    public void radioSelectedCallback() {
        int selectIndex = -1;
        String selectItem = "";
        for (int i = 0; i < selects.size(); i++) {
            JRadioButton radio = selects.get(i);
            if (radio.isSelected()) {
                selectIndex = i;
                selectItem = radio.getText();
            }
        }
        boolean change = false;
        if (selectIndex != this.selectedIndex) {
            change = true;
            this.selectedIndex = selectIndex;
            this.selectItem = selectItem;
        }
        fireItemStateChanged(new ItemEvent(this,
                ItemEvent.ITEM_STATE_CHANGED,
                this,
                change ? ItemEvent.SELECTED : ItemEvent.DESELECTED));
    }

    @Override
    public void addItemListener(ItemListener listener) {
        AtomicBoolean isFirst = new AtomicBoolean(false);
        this.selects.forEach(radio -> radio.addItemListener(e -> {
            //所有的子属性都添加一个状态变更监听器
            listener.itemStateChanged(e);
            //给父级也添加一个状态变更监听器
            if (isFirst.compareAndSet(false, true)) {
                fireItemStateChanged(e);
            }
        }));
    }

    @Override
    public void removeItemListener(ItemListener listener) {
        this.listenerList.remove(ItemListener.class, listener);
    }

    protected void fireItemStateChanged(ItemEvent e) {
        Object[] listeners = listenerList.getListenerList();
        for (int i = listeners.length - 2; i >= 0; i -= 2) {
            if (listeners[i] == ItemListener.class) {
                ((ItemListener) listeners[i + 1]).itemStateChanged(e);
            }
        }
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            JFrame frame = new JFrame("Dynamic RadioButton Example");
            frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
            frame.setLayout(new FlowLayout());
            frame.setSize(400, 800);
            frame.setPreferredSize(new Dimension(400, 800));

            JPanel panel = new JPanel();
            panel.setLayout(new BoxLayout(panel, BoxLayout.Y_AXIS));
            RadioButtonGroup radioButtonGroup = new RadioButtonGroup(java.util.List.of("a", "b", "c"));
            JLabel radioLabel = new JLabel("Select a Radio Button");
            panel.add(radioLabel);
            panel.add(radioButtonGroup);
            frame.add(panel);

            frame.pack();
            frame.setVisible(true);
        });
    }

    public void removeAll() {
        this.selects.clear();
        this.panel.removeAll();
    }

    public void setSelectedIndex(int index) {
        if (this.selects.isEmpty()) {
            return;
        }
        this.selectedIndex = index;
        if (index == -1) {
            this.selects.forEach(radio -> {
                radio.setSelected(false);
                if(radio.isSelected()){
                    fireItemStateChanged(new ItemEvent(radio, ItemEvent.ITEM_STATE_CHANGED, radio, ItemEvent.DESELECTED));
                }
            });
        }else{
            this.selects.forEach(radio -> {
                radio.setSelected(false);
                if(radio.isSelected()){
                    fireItemStateChanged(new ItemEvent(radio, ItemEvent.ITEM_STATE_CHANGED, radio, ItemEvent.DESELECTED));
                }
            });
            JRadioButton radio = this.selects.get(index);
            radio.setSelected(true);
            this.selectItem = radio.getText();
            fireItemStateChanged(new ItemEvent(radio, ItemEvent.ITEM_STATE_CHANGED, radio, ItemEvent.SELECTED));
        }
    }
}
