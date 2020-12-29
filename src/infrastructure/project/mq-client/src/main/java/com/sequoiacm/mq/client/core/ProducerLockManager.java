package com.sequoiacm.mq.client.core;

import org.springframework.beans.factory.annotation.Value;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.locks.ReentrantLock;

// 投递消息和注册回调这两个动作需要加锁，
// 防止在投递消息与注册回调之间的空隙，有线程来找回调对象
// 根据msgKey哈希取模得到锁对象
public class ProducerLockManager {
    private final int lockSize;

    private List<ReentrantLock> lockList;

    public ProducerLockManager(int lockSize) {
        this.lockSize = lockSize;
        lockList = new ArrayList<>(lockSize);
        for (int i = 0; i < lockSize; i++) {
            lockList.add(new ReentrantLock());
        }
    }

    public ReentrantLock getLock(String key) {
        int idx = Math.abs(key.hashCode() % lockSize);
        return lockList.get(idx);
    }
}
