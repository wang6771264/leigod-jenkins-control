package org.codinjutsu.tools.jenkins.example;

import com.intellij.openapi.ui.ComboBox;
import org.jetbrains.annotations.NotNull;

import javax.swing.*;
import java.io.Serializable;

/**
 * ==========================
 * 开发：wei.wang
 * 创建时间：2024-08-14 23:43
 * 版本：1.0
 * 描述：下拉列表组合框
 * ==========================
 */
public class SelectComboBox extends ComboBox<String> implements SelectComponent, Serializable {

    public SelectComboBox(@NotNull ComboBoxModel<String> model) {
        super(model);
    }

    @Override
    public String selectedItem() {
        return (String) this.getSelectedItem();
    }
}
