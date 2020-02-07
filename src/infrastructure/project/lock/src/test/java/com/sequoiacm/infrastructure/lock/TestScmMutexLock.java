package com.sequoiacm.infrastructure.lock;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import org.junit.AfterClass;
import org.junit.Assert;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

import com.sequoiacm.infrastructure.lock.curator.CuratorLockFactory;
import com.sequoiacm.infrastructure.lock.curator.CuratorLockTools;
import com.sequoiacm.infrastructure.lock.curator.CuratorZKCleaner;

public class TestScmMutexLock {
    public static final long SMALLSLEEPTIME = 1000;
    public static final long LOCK_WAITTIME = 500;

    private static String zkConnStr = "192.168.20.70:2181,192.168.20.922222:2181";
    private static String[] lockPath = { "ws", "dir", "bat", "file" };
    private static CuratorLockFactory lockFactoryImpl;
    private static List<Integer> resultList;
    private static AtomicInteger startStep;
    private static AtomicInteger lockStep;
    private static boolean isLocked;

    @BeforeClass
    public static void setUp() throws Exception {
        resultList = new ArrayList<Integer>();
        try {
            lockFactoryImpl = new CuratorLockFactory(zkConnStr, 5);
        }
        catch (Exception e) {
            e.printStackTrace();
        }
        CuratorZKCleaner.getInstance().clearResidualNode(lockFactoryImpl.getCuratorClient(),
                CuratorLockTools.getLockPath(lockPath), 0);
    }

    @Before
    public void init() {
        resultList.clear();
        startStep = new AtomicInteger(2);
        lockStep = new AtomicInteger(1);
        isLocked = false;
    }

    @AfterClass
    public static void tearDown() {
        lockFactoryImpl.close();
    }

    /**
     * use Mutex lock()
     */
    @Test
    public void testMutexLock() throws InterruptedException {
        ScmLock mtl = lockFactoryImpl.createLock(lockPath);

        TestScmMutexLock.MutexLock_ThreadA1 mtla11 = new TestScmMutexLock.MutexLock_ThreadA1(mtl);
        TestScmMutexLock.MutexLock_ThreadA1 mtla12 = new TestScmMutexLock.MutexLock_ThreadA1(mtl);
        mtla11.start();
        mtla12.start();
        mtla11.join();
        mtla12.join();

        List<Integer> myList = new ArrayList<>(200);
        for (int i = 0; i < 2; i++) {
            for (int j = 1; j < 101; j++) {
                myList.add(j);
            }
        }

        Assert.assertNotNull(TestScmMutexLock.resultList);
        Assert.assertEquals(myList.size(), TestScmMutexLock.resultList.size());
        for (int k = 0; k < myList.size(); k++) {
            Assert.assertEquals(myList.get(k), TestScmMutexLock.resultList.get(k));
        }
    }

    /**
     * use Mutex lock(long waitTime, TimeUnit unit)
     */
    @Test
    public void testMutexLock_Wait() throws InterruptedException {
        ScmLock mtl = lockFactoryImpl.createLock(lockPath);

        TestScmMutexLock.MutexLock_ThreadA2 mtla2 = new TestScmMutexLock.MutexLock_ThreadA2(mtl);
        TestScmMutexLock.MutexLock_ThreadB mtlb = new TestScmMutexLock.MutexLock_ThreadB(mtl);
        mtla2.start();
        mtlb.start();
        mtla2.join();
        mtlb.join();

        List<Integer> myListOne = new ArrayList<>(200);
        List<Integer> myListTwo = new ArrayList<>(100);
        for (int i = 0; i < 2; i++) {
            for (int j = 1; j < 101; j++) {
                myListOne.add(j);
            }
        }

        for (int k = 1; k < 101; k++) {
            myListTwo.add(k);
        }

        // CuratorLockProperty.smallSleepTime >
        // CuratorLockProperty.lock_waitTime
        Assert.assertNotNull(TestScmMutexLock.resultList);
        Assert.assertEquals(myListTwo.size(), TestScmMutexLock.resultList.size());
        Assert.assertEquals(false, TestScmMutexLock.isLocked);
        for (int m = 0; m < myListTwo.size(); m++) {
            Assert.assertEquals(myListTwo.get(m), TestScmMutexLock.resultList.get(m));
        }

        // // CuratorLockProperty.smallSleepTime <
        // // CuratorLockProperty.lock_waitTime
        // Assert.assertNotNull(TestScmMutexLock.resultList);
        // Assert.assertEquals(myListOne.size(),
        // TestScmMutexLock.resultList.size());
        // if (TestScmMutexLock.isLocked) {
        // for (int m = 0; m < myListOne.size(); m++) {
        // Assert.assertEquals(myListOne.get(m),
        // TestScmMutexLock.resultList.get(m));
        // }
        // }
    }

    /**
     * use Mutex TryLock()
     */
    @Test
    public void testMutexLock_Try() throws InterruptedException {
        ScmLock mtl = lockFactoryImpl.createLock(lockPath);

        TestScmMutexLock.MutexLock_ThreadA2 mtla2 = new TestScmMutexLock.MutexLock_ThreadA2(mtl);
        TestScmMutexLock.MutexLock_ThreadC mtlc = new TestScmMutexLock.MutexLock_ThreadC(mtl);
        mtla2.start();
        mtlc.start();
        mtla2.join();
        mtlc.join();

        List<Integer> myListOne = new ArrayList<>(200);
        List<Integer> myListTwo = new ArrayList<>(100);
        for (int i = 0; i < 2; i++) {
            for (int j = 1; j < 101; j++) {
                myListOne.add(j);
            }
        }

        for (int k = 1; k < 101; k++) {
            myListTwo.add(k);
        }

        // CuratorLockProperty.smallSleepTime >
        // CuratorLockProperty.tryLock_waitTime
        Assert.assertNotNull(TestScmMutexLock.resultList);
        Assert.assertEquals(myListTwo.size(), TestScmMutexLock.resultList.size());
        Assert.assertEquals(false, TestScmMutexLock.isLocked);
        for (int m = 0; m < myListTwo.size(); m++) {
            Assert.assertEquals(myListTwo.get(m), TestScmMutexLock.resultList.get(m));
        }

        // // CuratorLockProperty.smallSleepTime <
        // // CuratorLockProperty.tryLock_waitTime
        // Assert.assertNotNull(TestScmMutexLock.resultList);
        // Assert.assertEquals(myListOne.size(),TestScmMutexLock.resultList.size());
        // if(TestScmMutexLock.isLocked){
        // for (int m=0;m<myListOne.size();m++) {
        // Assert.assertEquals(myListOne.get(m),TestScmMutexLock.resultList.get(m));
        // }
        // }
    }

    /* Define Implement Thread */
    static class CommonThread extends Thread {
        protected ScmLock scmLock;

        public CommonThread(ScmLock scmLock) {
            this.scmLock = scmLock;
        }

        public void dowork(List<Integer> list) {
            for (int i = 1; i < 101; i++) {
                if (i == 50) {
                    try {
                        Thread.sleep(SMALLSLEEPTIME);
                    }
                    catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                }
                list.add(i);
            }
        }
    }

    static class MutexLock_ThreadA1 extends CommonThread {

        public MutexLock_ThreadA1(ScmLock lock) {
            super(lock);
        }

        @Override
        public void run() {
            TestScmMutexLock.startStep.decrementAndGet();

            while (TestScmMutexLock.startStep.get() != 0) {
            }
            try {
                this.scmLock.lock();
                dowork(TestScmMutexLock.resultList);
            }
            catch (Exception e) {
                e.printStackTrace();
            }
            finally {
                this.scmLock.unlock();
            }
        }
    }

    static class MutexLock_ThreadA2 extends CommonThread {

        public MutexLock_ThreadA2(ScmLock lock) {
            super(lock);
        }

        @Override
        public void run() {
            try {
                this.scmLock.lock();
                TestScmMutexLock.lockStep.decrementAndGet();
                dowork(TestScmMutexLock.resultList);
            }
            catch (Exception e) {
                e.printStackTrace();
            }
            finally {
                this.scmLock.unlock();
            }
        }
    }

    static class MutexLock_ThreadB extends CommonThread {

        public MutexLock_ThreadB(ScmLock lock) {
            super(lock);
        }

        @Override
        public void run() {
            while (TestScmMutexLock.lockStep.get() != 0) {
            }
            boolean locked = false;
            try {
                locked = this.scmLock.lock(LOCK_WAITTIME, TimeUnit.MILLISECONDS);
                if (locked) {
                    TestScmMutexLock.isLocked = locked;
                    dowork(TestScmMutexLock.resultList);
                }
            }
            catch (Exception e) {
                e.printStackTrace();
            }
            finally {
                if (locked) {
                    this.scmLock.unlock();
                }
            }
        }
    }

    static class MutexLock_ThreadC extends CommonThread {

        public MutexLock_ThreadC(ScmLock lock) {
            super(lock);
        }

        @Override
        public void run() {
            while (TestScmMutexLock.lockStep.get() != 0) {
            }
            boolean locked = false;
            try {
                locked = this.scmLock.tryLock();
                if (locked) {
                    TestScmMutexLock.isLocked = locked;
                    dowork(TestScmMutexLock.resultList);
                }
            }
            catch (Exception e) {
                e.printStackTrace();
            }
            finally {
                if (locked) {
                    this.scmLock.unlock();
                }
            }
        }
    }

}
