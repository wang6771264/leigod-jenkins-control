package org.codinjutsu.tools.jenkins.component.example;

import javax.swing.*;
import java.awt.*;
import java.io.Serializable;
import java.util.List;

public class JListExample implements Serializable {
    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            JFrame frame = new JFrame("Checkbox JList Example");
            frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
            frame.setLayout(new FlowLayout());
            frame.setSize(400, 800);
            frame.setPreferredSize(new Dimension(400, 800));

            List<JCheckBox> list  = List.of(new JCheckBox("CheckBox1"), new JCheckBox("CheckBox2"),new JCheckBox("CheckBox3"));
            DefaultListModel<JCheckBox> listModel = new DefaultListModel<>();
            list.forEach(listModel::addElement);

            JList<JCheckBox> jlist = new JList<>(listModel);
            jlist.setLayoutOrientation(JList.HORIZONTAL_WRAP);
            jlist.setCellRenderer(new CheckBoxListRenderer());
            jlist.setVisibleRowCount(1); // 设置为1行可见

            jlist.addListSelectionListener(e -> {
                if (!e.getValueIsAdjusting()) {
                    int index = jlist.getSelectedIndex();
                    if (index != -1) {
                        list.forEach(checkBox -> checkBox.setSelected(false));
                        JCheckBox checkBox = list.get(index);
                        checkBox.setSelected(true);
                        listModel.set(index, list.get(index)); // Toggle checkbox state
                        jlist.repaint();
                    }
                }
            });

            JLabel label = new JLabel("Select a JList with Checkboxes");
            JPanel panel = new JPanel();
            panel.setLayout(new BoxLayout(panel, BoxLayout.Y_AXIS));
            panel.add(label);
            panel.add(new JScrollPane(jlist));
            frame.add(panel);
            frame.pack();
            frame.setVisible(true);
        });
    }

    static class CheckBoxListRenderer extends JCheckBox implements ListCellRenderer<JCheckBox> {
        @Override
        public Component getListCellRendererComponent(JList<? extends JCheckBox> list, JCheckBox value,
                                                      int index, boolean isSelected, boolean cellHasFocus) {
            value.setSelected(isSelected);
            setEnabled(list.isEnabled());
            setFont(list.getFont());
            setBackground(isSelected ? Color.LIGHT_GRAY : Color.WHITE);
            return this;
        }
    }
}
