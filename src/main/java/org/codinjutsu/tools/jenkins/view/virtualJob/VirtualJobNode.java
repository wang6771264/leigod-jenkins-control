package org.codinjutsu.tools.jenkins.view.virtualJob;

import lombok.Getter;
import org.codinjutsu.tools.jenkins.view.JenkinsTreeNode;
import org.codinjutsu.tools.jenkins.view.JenkinsTreeNodeVisitor;
import org.jetbrains.annotations.NotNull;

/**
 * 虚拟任务节点
 * 表示一个包含多个Jenkins任务的工作流
 */
@Getter
public class VirtualJobNode implements JenkinsTreeNode {

    @Override
    public void render(JenkinsTreeNodeVisitor treeNodeRenderer) {

    }

    @Override
    public String toString() {
        return "🔗 " + this.getName(); // 使用特殊图标标识虚拟任务
    }

    @Override
    public @NotNull String getUrl() {
        return "virtual://" + this.getName();
    }
}