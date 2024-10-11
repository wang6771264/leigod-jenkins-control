package org.codinjutsu.tools.jenkins.example;

import com.alibaba.fastjson.JSON;
import lombok.Getter;
import org.codinjutsu.tools.jenkins.settings.multiServer.BeanDataTableModel;
import org.codinjutsu.tools.jenkins.settings.multiServer.JenkinsServerTableItem;

import javax.swing.*;
import javax.swing.table.TableCellEditor;
import javax.swing.table.TableCellRenderer;
import java.awt.*;

public class JenkinsTableExample {
    public static void main(String[] args) {
        // 创建一个 JFrame
        JFrame frame = new JFrame("Jenkins 服务信息");
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.setSize(600, 400);

        // 创建一个表格模型，允许编辑
        BeanDataTableModel<JenkinsServerTableItem> model = new BeanDataTableModel<>(JenkinsServerTableItem.class);

        // 创建一个 JTable
        JTable table = new JTable(model) {
            @Override
            public boolean isCellEditable(int row, int column) {
                return true;
            }
        };

        // 将表格放入滚动面板
        JScrollPane scrollPane = new JScrollPane(table);

        // 创建一个面板来放置按钮
        JPanel buttonPanel = new JPanel();
        // 设置为左对齐
        buttonPanel.setLayout(new FlowLayout(FlowLayout.LEFT));
        // 创建加按钮
        JButton addBtn = new JButton("+");
        JButton delBtn = new JButton("-");
        addBtn.addActionListener(e -> {
            // 添加一行空数据，用户可以编辑
            model.addRow(new Object[]{"", "", "", "", ""});
        });
        delBtn.addActionListener(e -> {
            //删除选中的行
            model.removeRow(table.getSelectedRow());
        });
        buttonPanel.add(addBtn);
        buttonPanel.add(delBtn);

        // 创建一个面板来放置按钮
        JPanel bottomPanel = new JPanel();
        // 设置为左对齐
        bottomPanel.setLayout(new FlowLayout(FlowLayout.LEFT));
        JButton editButton = new JButton("Reload Connection");
        JButton deleteButton = new JButton("Test Connection");

        editButton.addActionListener(e -> {
            // 在按钮被点击时停止编辑并执行逻辑
            JOptionPane.showMessageDialog(bottomPanel, "编辑按钮被点击, 行: " + table.getSelectedRow());
            System.out.println(JSON.toJSONString(model.getBean(table.getSelectedRow())));
        });

        deleteButton.addActionListener(e -> {
            // 在按钮被点击时停止编辑并执行逻辑
            JOptionPane.showMessageDialog(bottomPanel, "删除按钮被点击, 行: " + table.getSelectedRow());
            System.out.println(JSON.toJSONString(model.getBean(table.getSelectedRow())));
        });

        bottomPanel.add(editButton);
        bottomPanel.add(deleteButton);

        // 创建一个主面板，使用垂直布局
        JPanel mainPanel = new JPanel();
        mainPanel.setLayout(new BorderLayout());
        mainPanel.add(buttonPanel, BorderLayout.NORTH); // 将按钮面板放在北部
        mainPanel.add(scrollPane, BorderLayout.CENTER); // 将表格放在中心
        mainPanel.add(bottomPanel, BorderLayout.SOUTH); // 将按钮面板放在南部

        // 添加主面板到框架
        frame.add(mainPanel);

        // 显示窗口
        frame.setVisible(true);
    }

    // 自定义按钮渲染器
    static class ButtonRenderer implements TableCellRenderer {

        private final ButtonEditor editor;

        public ButtonRenderer(ButtonEditor editor) {
            this.editor = editor;
        }

        @Override
        public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected, boolean hasFocus, int row, int column) {
            return this.editor.getPanel();
        }
    }

    /**
     * 自定义按钮编辑器
     */
    @Getter
    static class ButtonEditor extends AbstractCellEditor implements TableCellEditor {
        private final JButton reload;
        private final JButton test;
        private final JPanel panel;
        private int row;

        public ButtonEditor() {
            this.panel = new JPanel();
            this.panel.setLayout(new BoxLayout(panel, BoxLayout.Y_AXIS)); // 垂直布局
            this.reload = new JButton("Reload Connection");
            this.test = new JButton("Test Connection");
            // 设置按钮的高度固定，宽度自适应
            Dimension buttonDimension = new Dimension(100, 17); // 宽度自适应，高度固定为30
            reload.setPreferredSize(buttonDimension);
            test.setPreferredSize(buttonDimension);

            this.reload.addActionListener(e -> {
                // 在按钮被点击时停止编辑并执行逻辑
                fireEditingStopped(); // 结束编辑
                JOptionPane.showMessageDialog(panel, "编辑按钮被点击, 行: " + row);
            });

            test.addActionListener(e -> {
                // 在按钮被点击时停止编辑并执行逻辑
                fireEditingStopped(); // 结束编辑
                JOptionPane.showMessageDialog(panel, "删除按钮被点击, 行: " + row);
            });

            panel.add(reload);
            panel.add(test);
        }

        @Override
        public Component getTableCellEditorComponent(JTable table, Object value, boolean isSelected, int row, int column) {
            this.row = row;
            return panel; // 返回包含按钮的面板
        }

        @Override
        public Object getCellEditorValue() {
            //返回按钮本身
            return this;
        }
    }
}
