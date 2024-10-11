package org.codinjutsu.tools.jenkins.example;

import com.intellij.ui.treeStructure.SimpleTree;

import javax.swing.*;
import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.DefaultTreeModel;
import javax.swing.tree.TreePath;

public class TreeExample {

    public static void main(String[] args) {
        // 创建树的根节点和二级节点
        DefaultMutableTreeNode rootNode = new DefaultMutableTreeNode("根节点");
        DefaultMutableTreeNode node1 = new DefaultMutableTreeNode("节点1");
        DefaultMutableTreeNode node11 = new DefaultMutableTreeNode("节点1-1");

        DefaultMutableTreeNode node2 = new DefaultMutableTreeNode("节点2");
        DefaultMutableTreeNode node21 = new DefaultMutableTreeNode("节点2-1");

        // 构建树结构
        rootNode.add(node1);
        rootNode.add(node2);
        node1.add(node11);
        node2.add(node21);

        // 创建SimpleTree并设置模型
        JTree tree = new JTree();
        tree.setModel(new DefaultTreeModel(rootNode));

        // 隐藏根节点
        tree.setRootVisible(false);
        // 选择二级节点
        TreePath path = new TreePath(node11.getPath());
        tree.setSelectionPath(path);

        // 展开二级节点的父节点
        tree.expandPath(new TreePath(node1.getPath()));

        // 创建并显示JFrame
        JFrame frame = new JFrame("SimpleTree Example");
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.add(new JScrollPane(tree));
        frame.setSize(300, 300);
        frame.setVisible(true);
    }
}
