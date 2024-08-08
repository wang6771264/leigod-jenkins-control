package org.codinjutsu.tools.jenkins.example;

import javax.swing.*;
import javax.swing.text.html.HTMLEditorKit;
import java.awt.*;

public class CopyHyperLinkHelper {
    private CopyHyperLinkHelper() {
    }

    private static final JTextPane COPY_TEXT_PANE = new JTextPane() {{
        setContentType("text/html");
        setEditorKit(new HTMLEditorKit());
    }};

    private static final String HYPER_LINK_FORMAT = "<html><a href=\"%s\">%s</a></html>";

    public static void main(String[] args) {
        JFrame frame = new JFrame("JTextPane with Copy Support");
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.setSize(400, 300);
        frame.setLayout(new BorderLayout());
        String htmlText = String.format(HYPER_LINK_FORMAT, "http://172.31.4.7:8090/jenkins/job/leigod-nn-goods-build/364/", "#364");
        // 设置一些示例文本
        COPY_TEXT_PANE.setText(htmlText);

        // 调用selectAllText方法选中所有文本
        selectAndShowTextPane(COPY_TEXT_PANE);
        COPY_TEXT_PANE.copy();

        // 设置一些示例文本2
        htmlText = String.format(HYPER_LINK_FORMAT, "http://172.31.4.7:8090/jenkins/job/leigod-nn-goods-build/365/", "#365");
        COPY_TEXT_PANE.setText(htmlText);
        // 调用selectAllText方法选中所有文本
        selectAndShowTextPane(COPY_TEXT_PANE);
        COPY_TEXT_PANE.copy();

        // 将文本区域添加到框架中
        frame.add(new JScrollPane(COPY_TEXT_PANE), BorderLayout.CENTER);
        frame.setVisible(true);
    }

    public static void copy(String link, String displayName) {
        String htmlText = String.format(HYPER_LINK_FORMAT, link, displayName);
        COPY_TEXT_PANE.setText(htmlText);
        //调用selectAllText方法选中所有文本
        selectAndShowTextPane(COPY_TEXT_PANE);
        COPY_TEXT_PANE.copy();
    }

    private static void selectAllText(JTextPane textPane) {
        // 获取文本长度
        int textLength = textPane.getDocument().getLength();
        // 将插入点移动到文本末尾
        textPane.setCaretPosition(textLength);
        // 将插入点移动到文本开头，从而选中所有文本
        textPane.moveCaretPosition(0);
    }

    private static void selectAndShowTextPane(JTextPane textPane) {
        // 选中所有文本
        selectAllText(textPane);
        // 将JTextPane添加到JScrollPane中，然后显示在JFrame上
        // 这里省略了创建JFrame和JScrollPane的代码，只展示关键步骤
        // JScrollPane scrollPane = new JScrollPane(textPane);
        // JFrame frame = new JFrame("Select All Example");
        // frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        // frame.add(scrollPane);
        // frame.pack();
        // frame.setVisible(true);
    }
}