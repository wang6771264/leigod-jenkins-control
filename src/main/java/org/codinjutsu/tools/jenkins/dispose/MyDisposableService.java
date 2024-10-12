package org.codinjutsu.tools.jenkins.dispose;

import com.intellij.openapi.Disposable;

public class MyDisposableService implements Disposable {
    @Override
    public void dispose() {
        // 在这里执行资源清理，比如关闭连接、释放内存等
        System.out.println("Cleaning up resources...");
    }
}
