package org.codinjutsu.tools.jenkins.settings.multiServer;

import com.alibaba.fastjson.JSONObject;
import lombok.extern.slf4j.Slf4j;

import javax.swing.table.DefaultTableModel;

/**
 * ==========================
 * 开发：wei.wang
 * 创建时间：2024-09-25 14:05
 * 版本：1.0
 * 描述：bean对象的表格模型
 * ==========================
 */
@Slf4j
public class BeanDataTableModel<T> extends DefaultTableModel {

    /**
     * 数据模型的类对象
     */
    private final Class<T> clazz;

    public BeanDataTableModel(Class<T> clazz) {
        super(getColumnNames(clazz), 0);
        this.clazz = clazz;
    }

    private static String[] getColumnNames(Class<?> clazz) {
        String[] columnNames = new String[clazz.getDeclaredFields().length];
        for (int i = 0; i < clazz.getDeclaredFields().length; i++) {
            columnNames[i] = clazz.getDeclaredFields()[i].getName();
        }
        return columnNames;
    }

    /**
     * 获取对象
     *
     * @param row
     * @return
     */
    public T getBean(int row) {
        if (row == -1) {
            return null;
        }
        JSONObject json = new JSONObject();
        for (int i = 0; i < this.getColumnCount(); i++) {
            Object valueAt = this.getValueAt(row, i);
            if (valueAt == null) {
                continue;
            }
            json.put(this.getColumnName(i), valueAt);
        }
        return json.toJavaObject(this.clazz);
    }

    public void addRow(T obj) {
        String[] values = new String[this.getColumnCount()];
        JSONObject jsonObject = JSONObject.parseObject(JSONObject.toJSONString(obj));
        for (int i = 0; i < this.getColumnCount(); i++) {
            values[i] = jsonObject.getString(this.getColumnName(i));
        }
        super.addRow(values);
    }

}
