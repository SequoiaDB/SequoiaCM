package com.sequoiacm.infrastructure.test;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

import org.testng.Assert;
import org.testng.annotations.Test;

class TestLockValue {
    private List<Integer> valueList = new ArrayList<>();
    public void add(int value) {
        valueList.add(value);
    }

    public List<Integer> getList() {
        return valueList;
    }
}

class TestLockIncreaseThread extends Thread {
    private Lock lock;
    private TestLockValue value;
    private AtomicInteger count;
    public TestLockIncreaseThread(TestLockValue i, AtomicInteger count, Lock lock) {
        this.lock = lock;
        value = i;
        this.count = count;
    }

    @Override
    public void run() {
        count.decrementAndGet();
        while (count.get() != 0) {
        }

        lock.lock();
        try {
            for (int i = 0; i < 100; i++) {
                value.add(i);
            }
        }
        finally {
            lock.unlock();
        }
    }
}

public class TestLock {

    @Test
    public void test() throws InterruptedException {
        TestLockValue value = new TestLockValue();
        AtomicInteger count = new AtomicInteger(2);
        Lock l = new ReentrantLock();
        TestLockIncreaseThread t1 = new TestLockIncreaseThread(value, count, l);
        TestLockIncreaseThread t2 = new TestLockIncreaseThread(value, count, l);

        t1.start();
        t2.start();

        t1.join();
        t2.join();

        int checkCount = 0;
        for (int i : value.getList()) {
            Assert.assertEquals(i, checkCount % 100);
            checkCount++;
        }
    }
}
