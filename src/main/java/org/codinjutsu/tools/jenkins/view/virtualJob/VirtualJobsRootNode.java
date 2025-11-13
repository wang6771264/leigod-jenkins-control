package org.codinjutsu.tools.jenkins.view.virtualJob;

import org.codinjutsu.tools.jenkins.view.JenkinsTreeNode;
import org.codinjutsu.tools.jenkins.view.JenkinsTreeNodeVisitor;

/**
 * 虚拟任务根节点
 * 用于在树中显示所有虚拟任务的容器
 */
public class VirtualJobsRootNode implements JenkinsTreeNode {

    @Override
    public void render(JenkinsTreeNodeVisitor treeNodeRenderer) {

    }

    @Override
    public String toString() {
        return "Virtual Jobs";
    }

    @Override
    public String getUrl() {
        return "virtual://root";
    }
}