package org.codinjutsu.tools.jenkins.example;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ItemEvent;

public class DynamicRadioButtonExample {
    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            JFrame frame = new JFrame("Dynamic RadioButton Example");
            frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
            frame.setLayout(new FlowLayout());
            frame.setSize(400, 800);
            frame.setPreferredSize(new Dimension(400, 800)); // 设置首选大小

            // 创建下拉列表
            JComboBox<String> comboBox = new JComboBox<>();
            comboBox.addItem("Option 1");
            comboBox.addItem("Option 2");
            comboBox.addItem("Option 3");

            // 根据单选按钮的名称创建新的下拉列表
            JComboBox<String> additionalComboBox = new JComboBox<>();

            // 布局
            JPanel panel = new JPanel();
            panel.setLayout(new BoxLayout(panel, BoxLayout.Y_AXIS)); // 设置为垂直布局

            JPanel comboBoxPanel = new JPanel();
            comboBoxPanel.add(new JLabel("Select an Option"));
            comboBoxPanel.add(comboBox);
            panel.add(comboBoxPanel);
            JPanel radioGroup = new JPanel();
            JLabel radioLabel = new JLabel("Select a Radio Button");
            radioGroup.add(radioLabel);
            panel.add(radioGroup);
            panel.add(additionalComboBox);

            // 创建按钮组
            ButtonGroup buttonGroup = new ButtonGroup();

            // 表单属性
            FormData formData = new FormData();

            // 为下拉列表添加变化监听器
            comboBox.addItemListener(e -> {
                if (e.getStateChange() == ItemEvent.SELECTED) {
                    // 移除旧的单选按钮
                    for (Component component : radioGroup.getComponents()) {
                        if (component instanceof JRadioButton) {
                            buttonGroup.remove((JRadioButton) component);
                            radioGroup.remove(component);
                        }
                    }

                    // 根据下拉列表的选项创建新的单选按钮
                    String selectedOption = (String) e.getItem();
                    String[] radioButtonNames = getRadioButtonNamesForOption(selectedOption);
                    for (String name : radioButtonNames) {
                        JRadioButton radioButton = new JRadioButton(name);
                        radioButton.addItemListener(radioEvent -> {
                            if (radioEvent.getStateChange() == ItemEvent.SELECTED) {
                                formData.setRadioButtonSelection(name);
                                createAdditionalComboBox(additionalComboBox, name);
                            }
                        });
                        buttonGroup.add(radioButton);
                        radioGroup.add(radioButton);
                    }
                    // 重新布局
                    frame.revalidate();
                    frame.repaint();
                }
            });

            // 将下拉列表添加到框架
            frame.add(panel);
            // 启动框架
            frame.pack();
            frame.setVisible(true);
        });
    }

    // 示例：根据下拉列表选项获取单选按钮名称的方法
    private static String[] getRadioButtonNamesForOption(String option) {
        // 根据选项返回不同的单选按钮名称数组
        switch (option) {
            case "Option 1":
                return new String[]{"Option 1 Detail A", "Option 1 Detail B"};
            case "Option 2":
                return new String[]{"Option 2 Detail A"};
            case "Option 3":
                return new String[]{"Option 3 Detail A", "Option 3 Detail B", "Option 3 Detail C"};
            default:
                return new String[]{};
        }
    }

    // 动态创建下拉列表的方法
    private static void createAdditionalComboBox(JComboBox<String> additionalComboBox,
                                                 String radioButtonName) {
        additionalComboBox.removeAllItems();
        switch (radioButtonName) {
            case "Option 1 Detail A":
                additionalComboBox.addItem("Detail A1");
                additionalComboBox.addItem("Detail A2");
                break;
            case "Option 1 Detail B":
                additionalComboBox.addItem("Detail B1");
                additionalComboBox.addItem("Detail B2");
                break;
            case "Option 2 Detail A":
                additionalComboBox.addItem("Detail A3");
                break;
            case "Option 3 Detail A":
                additionalComboBox.addItem("Detail A4");
                additionalComboBox.addItem("Detail A5");
                break;
            case "Option 3 Detail B":
                additionalComboBox.addItem("Detail B3");
                additionalComboBox.addItem("Detail B4");
                break;
            case "Option 3 Detail C":
                additionalComboBox.addItem("Detail C1");
                break;
        }
    }

    // 表单数据类
    static class FormData {
        private String selectedComboBoxOption;
        private String selectedRadioButton;

        public String getSelectedComboBoxOption() {
            return selectedComboBoxOption;
        }

        public void setSelectedComboBoxOption(String selectedComboBoxOption) {
            this.selectedComboBoxOption = selectedComboBoxOption;
        }

        public String getRadioButtonSelection() {
            return selectedRadioButton;
        }

        public void setRadioButtonSelection(String selectedRadioButton) {
            this.selectedRadioButton = selectedRadioButton;
        }
    }
}
