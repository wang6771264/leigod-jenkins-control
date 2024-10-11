package org.codinjutsu.tools.jenkins.util;

import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;

/**
 * 快照watcher对象
 */
public class SnapshotWatcher {

    // 快照对象
    private Object snapshot;
    private long lastUpdatedTime;
    private volatile boolean running = false;

    private final Consumer<Object> consumer;
    private ScheduledFuture<?> scheduledFuture;

    private final ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(1);

    public SnapshotWatcher(Consumer<Object> consumer) {
        this.consumer = consumer;
    }

    // 设置快照对象
    public void updateSnapshot(Object newSnapshot) {
        if (!this.running) {
            this.running = true;
            this.scheduledFuture = scheduler.scheduleAtFixedRate(this::checkForUpdates, 0, 1, TimeUnit.SECONDS);
        }
        this.snapshot = newSnapshot;
        this.lastUpdatedTime = System.currentTimeMillis();
        System.out.println("快照更新为: " + newSnapshot);
    }

    // 检查快照更新时间
    private void checkForUpdates() {
        long currentTime = System.currentTimeMillis();
        // 如果超过3秒没有更新
        long timeoutMillis = 3000;
        if (currentTime - lastUpdatedTime > timeoutMillis) {
            onTimeout(); // 触发回调
        }
    }

    // 超时回调方法
    private void onTimeout() {
        this.running = false;
        System.out.println("快照对象在3秒内没有更新，进行清理操作！");
        if (this.consumer != null) {
            this.consumer.accept(snapshot);
        }
        // 在这里添加你需要执行的清理逻辑
        clearSnapshot(); // 清理快照
    }

    // 清理快照对象
    private void clearSnapshot() {
        this.snapshot = null; // 清空快照对象
        System.out.println("快照对象已被清理。");
        this.scheduledFuture.cancel(true);
    }

    // 关闭调度器
    public void shutdown() {
        scheduler.shutdown();
        try {
            if (!scheduler.awaitTermination(60, TimeUnit.SECONDS)) {
                scheduler.shutdownNow();
            }
        } catch (InterruptedException e) {
            scheduler.shutdownNow();
        }
    }
}
