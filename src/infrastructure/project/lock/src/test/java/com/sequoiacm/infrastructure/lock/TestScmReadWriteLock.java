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

public class TestScmReadWriteLock {
    public static final long SMALLSLEEPTIME = 1000;
    public static final long LOCK_WAITTIME = 500;

    private static String zkConnStr = "192.168.20.70:2181";
    private static String[] lockPath = { "ws", "dir", "bat", "file" };
    private static CuratorLockFactory lockFactoryImpl;
    private static List<Integer> resultList;
    private static String resultStr;
    private static AtomicInteger startStep;
    private static AtomicInteger lockStep;
    private static boolean isLocked;

    @BeforeClass
    public static void setUp() throws Exception {
        resultList = new ArrayList<Integer>();
        try {
            lockFactoryImpl = new CuratorLockFactory(zkConnStr);
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
        for (int i = 1; i < 101; i++) {
            resultList.add(i);
        }
        resultStr = "";
        startStep = new AtomicInteger(2);
        lockStep = new AtomicInteger(1);
        isLocked = false;
        ;
    }

    @AfterClass
    public static void tearDown() {
        lockFactoryImpl.close();
    }

    /**
     * use Read-Read lock()
     */
    @Test
    public void testReadWriteLock_RR() throws InterruptedException {
        ScmLock rwl_rl = lockFactoryImpl.createReadWriteLock(lockPath).readLock();

        TestScmReadWriteLock.ReadWriteLock_ThreadA1 rwl_rla11 = new TestScmReadWriteLock.ReadWriteLock_ThreadA1(
                rwl_rl);
        TestScmReadWriteLock.ReadWriteLock_ThreadA1 rwl_rla12 = new TestScmReadWriteLock.ReadWriteLock_ThreadA1(
                rwl_rl);
        rwl_rla11.start();
        rwl_rla12.start();
        rwl_rla11.join();
        rwl_rla12.join();

        String myStr = "";
        for (int i = 0; i < 2; i++) {
            for (int j = 1; j < 101; j++) {
                myStr += j + "";
            }
        }

        Assert.assertNotNull(TestScmReadWriteLock.resultStr);
        Assert.assertEquals(myStr.length(), TestScmReadWriteLock.resultStr.length());
        Assert.assertNotEquals(myStr, TestScmReadWriteLock.resultStr);
    }

    /**
     * use Write-Write lock()
     */
    @Test
    public void testReadWriteLock_WW() throws InterruptedException {
        ScmLock rwl_wl = lockFactoryImpl.createReadWriteLock(lockPath).writeLock();

        TestScmReadWriteLock.ReadWriteLock_ThreadB1 rwl_wlb11 = new TestScmReadWriteLock.ReadWriteLock_ThreadB1(
                rwl_wl);
        TestScmReadWriteLock.ReadWriteLock_ThreadB1 rwl_wlb12 = new TestScmReadWriteLock.ReadWriteLock_ThreadB1(
                rwl_wl);
        rwl_wlb11.start();
        rwl_wlb12.start();
        rwl_wlb11.join();
        rwl_wlb12.join();

        List<Integer> myList = new ArrayList<>(200);
        for (int i = 0; i < 3; i++) {
            for (int j = 1; j < 101; j++) {
                myList.add(j);
            }
        }

        Assert.assertNotNull(TestScmReadWriteLock.resultList);
        Assert.assertEquals(myList.size(), TestScmReadWriteLock.resultList.size());
        for (int k = 0; k < myList.size(); k++) {
            Assert.assertEquals(myList.get(k), TestScmReadWriteLock.resultList.get(k));
        }
    }

    /**
     * use Read-Write/Write-Read lock()
     */
    @Test
    public void testReadWriteLock() throws InterruptedException {
        ScmLock rwl_rl = lockFactoryImpl.createReadWriteLock(lockPath).readLock();
        ScmLock rwl_wl = lockFactoryImpl.createReadWriteLock(lockPath).writeLock();

        TestScmReadWriteLock.ReadWriteLock_ThreadA1 rwl_rla1 = new TestScmReadWriteLock.ReadWriteLock_ThreadA1(
                rwl_rl);
        TestScmReadWriteLock.ReadWriteLock_ThreadB1 rwl_wlb1 = new TestScmReadWriteLock.ReadWriteLock_ThreadB1(
                rwl_wl);
        rwl_rla1.start();
        rwl_wlb1.start();
        rwl_rla1.join();
        rwl_wlb1.join();

        String myStrOne = "";
        String myStrTwo = "";
        List<Integer> myList = new ArrayList<>(200);

        for (int i = 1; i < 101; i++) {
            myStrOne += i + "";
        }
        for (int j = 0; j < 2; j++) {
            for (int k = 1; k < 101; k++) {
                myStrTwo += k + "";
            }
        }
        for (int m = 0; m < 2; m++) {
            for (int n = 1; n < 101; n++) {
                myList.add(n);
            }
        }

        boolean bl = true;
        boolean bla = false;
        boolean blb = false;

        for (int x = 0; x < myList.size(); x++) {
            if (myList.get(x) != resultList.get(x)) {
                bl = false;
                break;
            }
        }

        if (TestScmReadWriteLock.resultStr.length() == myStrOne.length()
                && TestScmReadWriteLock.resultStr.equals(myStrOne) && bl) {
            bla = true;
        }
        if (TestScmReadWriteLock.resultStr.length() == myStrTwo.length()
                && TestScmReadWriteLock.resultStr.equals(myStrTwo) && bl) {
            blb = true;
        }

        Assert.assertNotNull(TestScmReadWriteLock.resultList);
        Assert.assertEquals(myList.size(), TestScmReadWriteLock.resultList.size());
        Assert.assertTrue(bla || blb);
    }

    /**
     * use Read-Read lock(long waitTime, TimeUnit unit)
     */
    @Test
    public void testReadWriteLock_RR_Wait() throws InterruptedException {
        ScmLock rwl_rl = lockFactoryImpl.createReadWriteLock(lockPath).readLock();
        TestScmReadWriteLock.ReadWriteLock_ThreadA2 rwl_rla2 = new TestScmReadWriteLock.ReadWriteLock_ThreadA2(
                rwl_rl);
        TestScmReadWriteLock.ReadWriteLock_ThreadC rwl_rlc = new TestScmReadWriteLock.ReadWriteLock_ThreadC(
                rwl_rl);
        rwl_rla2.start();
        rwl_rlc.start();
        rwl_rla2.join();
        rwl_rlc.join();

        String myStr = "";
        for (int i = 0; i < 2; i++) {
            for (int j = 1; j < 101; j++) {
                myStr += j + "";
            }
        }

        // CuratorLockProperty.smallSleepTime >
        // CuratorLockProperty.lock_waitTime
        Assert.assertNotNull(TestScmReadWriteLock.resultStr);
        Assert.assertEquals(true, TestScmReadWriteLock.isLocked);
        Assert.assertEquals(myStr.length(), TestScmReadWriteLock.resultStr.length());
        Assert.assertNotEquals(myStr, TestScmReadWriteLock.resultStr);

        // CuratorLockProperty.smallSleepTime <
        // CuratorLockProperty.lock_waitTime
        // Assert.assertNotNull(TestScmReadWriteLock.resultStr);
        // Assert.assertEquals(true,TestScmReadWriteLock.isLocked);
        // Assert.assertEquals(myStr.length(),TestScmReadWriteLock.resultStr.length());
        // Assert.assertNotEquals(myStr,TestScmReadWriteLock.resultStr);
    }

    /**
     * use Write-Write lock(long waitTime, TimeUnit unit)
     */
    @Test
    public void testReadWriteLock_WW_Wait() throws InterruptedException {
        ScmLock rwl_wl = lockFactoryImpl.createReadWriteLock(lockPath).writeLock();
        TestScmReadWriteLock.ReadWriteLock_ThreadB2 rwl_rlb2 = new TestScmReadWriteLock.ReadWriteLock_ThreadB2(
                rwl_wl);
        TestScmReadWriteLock.ReadWriteLock_ThreadD rwl_rld = new TestScmReadWriteLock.ReadWriteLock_ThreadD(
                rwl_wl);
        rwl_rlb2.start();
        rwl_rld.start();
        rwl_rlb2.join();
        rwl_rld.join();

        List<Integer> myListOne = new ArrayList<>(200);
        List<Integer> myListTwo = new ArrayList<>(100);
        for (int i = 0; i < 2; i++) {
            for (int j = 1; j < 101; j++) {
                myListOne.add(j);
            }
        }
        for (int m = 0; m < 3; m++) {
            for (int n = 1; n < 101; n++) {
                myListTwo.add(n);
            }
        }

        // CuratorLockProperty.smallSleepTime >
        // CuratorLockProperty.lock_waitTime
        Assert.assertNotNull(TestScmReadWriteLock.resultList);
        Assert.assertEquals(false, TestScmReadWriteLock.isLocked);
        Assert.assertEquals(myListOne.size(), TestScmReadWriteLock.resultList.size());
        for (int k = 0; k < myListOne.size(); k++) {
            Assert.assertEquals(myListOne.get(k), TestScmReadWriteLock.resultList.get(k));
        }

        // CuratorLockProperty.smallSleepTime <
        // CuratorLockProperty.lock_waitTime
        // Assert.assertNotNull(TestScmReadWriteLock.resultList);
        // Assert.assertEquals(true,TestScmReadWriteLock.isLocked);
        // Assert.assertEquals(myListTwo.size(),TestScmReadWriteLock.resultList.size());
        // for (int k=0;k<myListTwo.size();k++) {
        // Assert.assertEquals(myListTwo.get(k),TestScmReadWriteLock.resultList.get(k));
        // }
    }

    /**
     * use Read-Write lock(long waitTime, TimeUnit unit)
     */
    @Test
    public void testReadWriteLock_RW_Wait() throws InterruptedException {
        ScmLock rwl_rl = lockFactoryImpl.createReadWriteLock(lockPath).readLock();
        ScmLock rwl_wl = lockFactoryImpl.createReadWriteLock(lockPath).writeLock();
        TestScmReadWriteLock.ReadWriteLock_ThreadA2 rwl_rla2 = new TestScmReadWriteLock.ReadWriteLock_ThreadA2(
                rwl_rl);
        TestScmReadWriteLock.ReadWriteLock_ThreadD rwl_rld = new TestScmReadWriteLock.ReadWriteLock_ThreadD(
                rwl_wl);
        rwl_rla2.start();
        rwl_rld.start();
        rwl_rla2.join();
        rwl_rld.join();

        String myStr = "";
        List<Integer> myListOne = new ArrayList<>(100);
        List<Integer> myListTwo = new ArrayList<>(200);

        for (int i = 1; i < 101; i++) {
            myStr += i + "";
            myListOne.add(i);
        }
        for (int j = 0; j < 2; j++) {
            for (int k = 1; k < 101; k++) {
                myListTwo.add(k);
            }
        }

        // CuratorLockProperty.smallSleepTime >
        // CuratorLockProperty.lock_waitTime
        Assert.assertNotNull(TestScmReadWriteLock.resultStr);
        Assert.assertNotNull(TestScmReadWriteLock.resultList);
        Assert.assertEquals(false, TestScmReadWriteLock.isLocked);
        Assert.assertEquals(myStr.length(), TestScmReadWriteLock.resultStr.length());
        Assert.assertEquals(myListOne.size(), TestScmReadWriteLock.resultList.size());
        Assert.assertEquals(myStr, TestScmReadWriteLock.resultStr);
        for (int m = 0; m < myListOne.size(); m++) {
            Assert.assertEquals(myListOne.get(m), TestScmReadWriteLock.resultList.get(m));
        }

        // CuratorLockProperty.smallSleepTime <
        // CuratorLockProperty.lock_waitTime
        // Assert.assertNotNull(TestScmReadWriteLock.resultStr);
        // Assert.assertNotNull(TestScmReadWriteLock.resultList);
        // Assert.assertEquals(true,TestScmReadWriteLock.isLocked);
        // Assert.assertEquals(myStr.length(),TestScmReadWriteLock.resultStr.length());
        // Assert.assertEquals(myListTwo.size(),TestScmReadWriteLock.resultList.size());
        // Assert.assertEquals(myStr,TestScmReadWriteLock.resultStr);
        // for (int m=0;m<myListTwo.size();m++) {
        // Assert.assertEquals(myListTwo.get(m),TestScmReadWriteLock.resultList.get(m));
        // }
    }

    /**
     * use Write-Read lock(long waitTime, TimeUnit unit)
     */
    @Test
    public void testReadWriteLock_WR_Wait() throws InterruptedException {
        ScmLock rwl_rl = lockFactoryImpl.createReadWriteLock(lockPath).readLock();
        ScmLock rwl_wl = lockFactoryImpl.createReadWriteLock(lockPath).writeLock();
        TestScmReadWriteLock.ReadWriteLock_ThreadB2 rwl_rlb2 = new TestScmReadWriteLock.ReadWriteLock_ThreadB2(
                rwl_wl);
        TestScmReadWriteLock.ReadWriteLock_ThreadC rwl_rlc = new TestScmReadWriteLock.ReadWriteLock_ThreadC(
                rwl_rl);
        rwl_rlb2.start();
        rwl_rlc.start();
        rwl_rlb2.join();
        rwl_rlc.join();

        String myStrOne = "";
        String myStrTwo = "";
        List<Integer> myList = new ArrayList<>(200);

        for (int i = 0; i < 2; i++) {
            for (int j = 1; j < 101; j++) {
                myStrTwo += j + "";
            }
        }
        for (int m = 0; m < 2; m++) {
            for (int n = 1; n < 101; n++) {
                myList.add(n);
            }
        }

        // CuratorLockProperty.smallSleepTime >
        // CuratorLockProperty.lock_waitTime
        Assert.assertTrue(TestScmReadWriteLock.resultStr.isEmpty());
        Assert.assertNotNull(TestScmReadWriteLock.resultList);
        Assert.assertEquals(false, TestScmReadWriteLock.isLocked);
        Assert.assertEquals(myStrOne.length(), TestScmReadWriteLock.resultStr.length());
        Assert.assertEquals(myList.size(), TestScmReadWriteLock.resultList.size());
        Assert.assertEquals(myStrOne, TestScmReadWriteLock.resultStr);
        for (int m = 0; m < myList.size(); m++) {
            Assert.assertEquals(myList.get(m), TestScmReadWriteLock.resultList.get(m));
        }

        // CuratorLockProperty.smallSleepTime <
        // CuratorLockProperty.lock_waitTime
        // Assert.assertNotNull(TestScmReadWriteLock.resultStr);
        // Assert.assertNotNull(TestScmReadWriteLock.resultList);
        // Assert.assertEquals(true,TestScmReadWriteLock.isLocked);
        // Assert.assertEquals(myStrTwo.length(),TestScmReadWriteLock.resultStr.length());
        // Assert.assertEquals(myList.size(),TestScmReadWriteLock.resultList.size());
        // Assert.assertEquals(myStrTwo,TestScmReadWriteLock.resultStr);
        // for (int m=0;m<myList.size();m++) {
        // Assert.assertEquals(myList.get(m),TestScmReadWriteLock.resultList.get(m));
        // }
    }

    /**
     * use Read-Read Tryock()
     */
    @Test
    public void testReadWriteLock_RR_Try() throws InterruptedException {
        ScmLock rwl_rl = lockFactoryImpl.createReadWriteLock(lockPath).readLock();
        TestScmReadWriteLock.ReadWriteLock_ThreadA2 rwl_rla2 = new TestScmReadWriteLock.ReadWriteLock_ThreadA2(
                rwl_rl);
        TestScmReadWriteLock.ReadWriteLock_ThreadE rwl_rle = new TestScmReadWriteLock.ReadWriteLock_ThreadE(
                rwl_rl);
        rwl_rla2.start();
        rwl_rle.start();
        rwl_rla2.join();
        rwl_rle.join();

        String myStr = "";
        for (int i = 0; i < 2; i++) {
            for (int j = 1; j < 101; j++) {
                myStr += j + "";
            }
        }

        // CuratorLockProperty.smallSleepTime >
        // CuratorLockProperty.tryLock_waitTime
        Assert.assertNotNull(TestScmReadWriteLock.resultStr);
        Assert.assertEquals(true, TestScmReadWriteLock.isLocked);
        Assert.assertEquals(myStr.length(), TestScmReadWriteLock.resultStr.length());
        Assert.assertNotEquals(myStr, TestScmReadWriteLock.resultStr);

        // CuratorLockProperty.smallSleepTime <
        // CuratorLockProperty.tryLock_waitTime
        // Assert.assertNotNull(TestScmReadWriteLock.resultStr);
        // Assert.assertEquals(true,TestScmReadWriteLock.isLocked);
        // Assert.assertEquals(myStr.length(),TestScmReadWriteLock.resultStr.length());
        // Assert.assertNotEquals(myStr,TestScmReadWriteLock.resultStr);
    }

    /**
     * use Write-Write Tryock()
     */
    @Test
    public void testReadWriteLock_WW_Try() throws InterruptedException {
        ScmLock rwl_wl = lockFactoryImpl.createReadWriteLock(lockPath).writeLock();
        TestScmReadWriteLock.ReadWriteLock_ThreadB2 rwl_rlb2 = new TestScmReadWriteLock.ReadWriteLock_ThreadB2(
                rwl_wl);
        TestScmReadWriteLock.ReadWriteLock_ThreadF rwl_rlf = new TestScmReadWriteLock.ReadWriteLock_ThreadF(
                rwl_wl);
        rwl_rlb2.start();
        rwl_rlf.start();
        rwl_rlb2.join();
        rwl_rlf.join();

        List<Integer> myListOne = new ArrayList<>(200);
        List<Integer> myListTwo = new ArrayList<>(100);
        for (int i = 0; i < 2; i++) {
            for (int j = 1; j < 101; j++) {
                myListOne.add(j);
            }
        }
        for (int m = 0; m < 3; m++) {
            for (int n = 1; n < 101; n++) {
                myListTwo.add(n);
            }
        }

        // CuratorLockProperty.smallSleepTime >
        // CuratorLockProperty.tryLock_waitTime
        Assert.assertNotNull(TestScmReadWriteLock.resultList);
        Assert.assertEquals(false, TestScmReadWriteLock.isLocked);
        Assert.assertEquals(myListOne.size(), TestScmReadWriteLock.resultList.size());
        for (int k = 0; k < myListOne.size(); k++) {
            Assert.assertEquals(myListOne.get(k), TestScmReadWriteLock.resultList.get(k));
        }

        // CuratorLockProperty.smallSleepTime <
        // CuratorLockProperty.tryLock_waitTime
        // Assert.assertNotNull(TestScmReadWriteLock.resultList);
        // Assert.assertEquals(true,TestScmReadWriteLock.isLocked);
        // Assert.assertEquals(myListTwo.size(),TestScmReadWriteLock.resultList.size());
        // for (int k=0;k<myListTwo.size();k++) {
        // Assert.assertEquals(myListTwo.get(k),TestScmReadWriteLock.resultList.get(k));
        // }
    }

    /**
     * use Read-Write Tryock()
     */
    @Test
    public void testReadWriteLock_RW_Try() throws InterruptedException {
        ScmLock rwl_rl = lockFactoryImpl.createReadWriteLock(lockPath).readLock();
        ScmLock rwl_wl = lockFactoryImpl.createReadWriteLock(lockPath).writeLock();
        TestScmReadWriteLock.ReadWriteLock_ThreadA2 rwl_rla2 = new TestScmReadWriteLock.ReadWriteLock_ThreadA2(
                rwl_rl);
        TestScmReadWriteLock.ReadWriteLock_ThreadF rwl_rlf = new TestScmReadWriteLock.ReadWriteLock_ThreadF(
                rwl_wl);
        rwl_rla2.start();
        rwl_rlf.start();
        rwl_rla2.join();
        rwl_rlf.join();

        String myStr = "";
        List<Integer> myListOne = new ArrayList<>(100);
        List<Integer> myListTwo = new ArrayList<>(200);

        for (int i = 1; i < 101; i++) {
            myStr += i + "";
            myListOne.add(i);
        }
        for (int j = 0; j < 2; j++) {
            for (int k = 1; k < 101; k++) {
                myListTwo.add(k);
            }
        }

        // CuratorLockProperty.smallSleepTime >
        // CuratorLockProperty.tryLock_waitTime
        Assert.assertNotNull(TestScmReadWriteLock.resultStr);
        Assert.assertNotNull(TestScmReadWriteLock.resultList);
        Assert.assertEquals(false, TestScmReadWriteLock.isLocked);
        Assert.assertEquals(myStr.length(), TestScmReadWriteLock.resultStr.length());
        Assert.assertEquals(myListOne.size(), TestScmReadWriteLock.resultList.size());
        Assert.assertEquals(myStr, TestScmReadWriteLock.resultStr);
        for (int m = 0; m < myListOne.size(); m++) {
            Assert.assertEquals(myListOne.get(m), TestScmReadWriteLock.resultList.get(m));
        }

        // CuratorLockProperty.smallSleepTime <
        // CuratorLockProperty.tryLock_waitTime
        // Assert.assertNotNull(TestScmReadWriteLock.resultStr);
        // Assert.assertNotNull(TestScmReadWriteLock.resultList);
        // Assert.assertEquals(true,TestScmReadWriteLock.isLocked);
        // Assert.assertEquals(myStr.length(),TestScmReadWriteLock.resultStr.length());
        // Assert.assertEquals(myListTwo.size(),TestScmReadWriteLock.resultList.size());
        // Assert.assertEquals(myStr,TestScmReadWriteLock.resultStr);
        // for (int m=0;m<myListTwo.size();m++) {
        // Assert.assertEquals(myListTwo.get(m),TestScmReadWriteLock.resultList.get(m));
        // }
    }

    /**
     * use Write-Read Tryock()
     */
    @Test
    public void testReadWriteLock_WR_Try() throws InterruptedException {
        ScmLock rwl_rl = lockFactoryImpl.createReadWriteLock(lockPath).readLock();
        ScmLock rwl_wl = lockFactoryImpl.createReadWriteLock(lockPath).writeLock();
        TestScmReadWriteLock.ReadWriteLock_ThreadB2 rwl_rlb2 = new TestScmReadWriteLock.ReadWriteLock_ThreadB2(
                rwl_wl);
        TestScmReadWriteLock.ReadWriteLock_ThreadE rwl_rle = new TestScmReadWriteLock.ReadWriteLock_ThreadE(
                rwl_rl);
        rwl_rlb2.start();
        rwl_rle.start();
        rwl_rlb2.join();
        rwl_rle.join();

        String myStrOne = "";
        String myStrTwo = "";
        List<Integer> myList = new ArrayList<>(200);

        for (int i = 0; i < 2; i++) {
            for (int j = 1; j < 101; j++) {
                myStrTwo += j + "";
            }
        }
        for (int m = 0; m < 2; m++) {
            for (int n = 1; n < 101; n++) {
                myList.add(n);
            }
        }

        // CuratorLockProperty.smallSleepTime >
        // CuratorLockProperty.tryLock_waitTime
        Assert.assertTrue(TestScmReadWriteLock.resultStr.isEmpty());
        Assert.assertNotNull(TestScmReadWriteLock.resultList);
        Assert.assertEquals(false, TestScmReadWriteLock.isLocked);
        Assert.assertEquals(myStrOne.length(), TestScmReadWriteLock.resultStr.length());
        Assert.assertEquals(myList.size(), TestScmReadWriteLock.resultList.size());
        Assert.assertEquals(myStrOne, TestScmReadWriteLock.resultStr);
        for (int m = 0; m < myList.size(); m++) {
            Assert.assertEquals(myList.get(m), TestScmReadWriteLock.resultList.get(m));
        }

        // CuratorLockProperty.smallSleepTime <
        // CuratorLockProperty.tryLock_waitTime
        // Assert.assertNotNull(TestScmReadWriteLock.resultStr);
        // Assert.assertNotNull(TestScmReadWriteLock.resultList);
        // Assert.assertEquals(true,TestScmReadWriteLock.isLocked);
        // Assert.assertEquals(myStrTwo.length(),TestScmReadWriteLock.resultStr.length());
        // Assert.assertEquals(myList.size(),TestScmReadWriteLock.resultList.size());
        // Assert.assertEquals(myStrTwo,TestScmReadWriteLock.resultStr);
        // for (int m=0;m<myList.size();m++) {
        // Assert.assertEquals(myList.get(m),TestScmReadWriteLock.resultList.get(m));
        // }
    }

    /* Define Implement Thread */
    static class CommonThread extends Thread {
        protected ScmLock scmLock;

        public CommonThread(ScmLock scmLock) {
            this.scmLock = scmLock;
        }

        public void get(List<Integer> list) {
            int getSize = list.size();
            for (int i = 0; i < getSize; i++) {
                if (i == getSize / 2) {
                    try {
                        Thread.sleep(SMALLSLEEPTIME);
                        ;
                    }
                    catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                }
                TestScmReadWriteLock.resultStr += list.get(i);
            }
        }

        public void add(List<Integer> list) {
            for (int j = 1; j < 101; j++) {
                if (j == 50) {
                    try {
                        Thread.sleep(SMALLSLEEPTIME);
                        ;
                    }
                    catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                }
                TestScmReadWriteLock.resultList.add(j);
            }
        }
    }

    static class ReadWriteLock_ThreadA1 extends CommonThread {

        public ReadWriteLock_ThreadA1(ScmLock lock) {
            super(lock);
        }

        @Override
        public void run() {
            TestScmReadWriteLock.startStep.decrementAndGet();

            while (TestScmReadWriteLock.startStep.get() != 0) {
            }
            try {
                this.scmLock.lock();
                get(TestScmReadWriteLock.resultList);
            }
            catch (Exception e) {
                e.printStackTrace();
            }
            finally {
                this.scmLock.unlock();
            }
        }
    }

    static class ReadWriteLock_ThreadA2 extends CommonThread {

        public ReadWriteLock_ThreadA2(ScmLock lock) {
            super(lock);
        }

        @Override
        public void run() {
            try {
                this.scmLock.lock();
                TestScmReadWriteLock.lockStep.decrementAndGet();
                get(TestScmReadWriteLock.resultList);
            }
            catch (Exception e) {
                e.printStackTrace();
            }
            finally {
                this.scmLock.unlock();
            }
        }
    }

    static class ReadWriteLock_ThreadB1 extends CommonThread {

        public ReadWriteLock_ThreadB1(ScmLock lock) {
            super(lock);
        }

        @Override
        public void run() {
            TestScmReadWriteLock.startStep.decrementAndGet();

            while (TestScmReadWriteLock.startStep.get() != 0) {
            }
            try {
                this.scmLock.lock();
                add(TestScmReadWriteLock.resultList);
            }
            catch (Exception e) {
                e.printStackTrace();
            }
            finally {
                this.scmLock.unlock();
            }
        }
    }

    static class ReadWriteLock_ThreadB2 extends CommonThread {

        public ReadWriteLock_ThreadB2(ScmLock lock) {
            super(lock);
        }

        @Override
        public void run() {
            try {
                this.scmLock.lock();
                TestScmReadWriteLock.lockStep.decrementAndGet();
                add(TestScmReadWriteLock.resultList);
            }
            catch (Exception e) {
                e.printStackTrace();
            }
            finally {
                this.scmLock.unlock();
            }
        }
    }

    static class ReadWriteLock_ThreadC extends CommonThread {

        public ReadWriteLock_ThreadC(ScmLock lock) {
            super(lock);
        }

        @Override
        public void run() {
            while (TestScmReadWriteLock.lockStep.get() != 0) {
            }
            boolean locked = false;
            try {
                locked = this.scmLock.lock(LOCK_WAITTIME, TimeUnit.MILLISECONDS);
                if (locked) {
                    TestScmReadWriteLock.isLocked = locked;
                    get(TestScmReadWriteLock.resultList);
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

    static class ReadWriteLock_ThreadD extends CommonThread {

        public ReadWriteLock_ThreadD(ScmLock lock) {
            super(lock);
        }

        @Override
        public void run() {
            while (TestScmReadWriteLock.lockStep.get() != 0) {
            }
            boolean locked = false;
            try {
                locked = this.scmLock.lock(LOCK_WAITTIME, TimeUnit.MILLISECONDS);
                if (locked) {
                    TestScmReadWriteLock.isLocked = locked;
                    add(TestScmReadWriteLock.resultList);
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

    static class ReadWriteLock_ThreadE extends CommonThread {

        public ReadWriteLock_ThreadE(ScmLock lock) {
            super(lock);
        }

        @Override
        public void run() {
            while (TestScmReadWriteLock.lockStep.get() != 0) {
            }
            boolean locked = false;
            try {
                locked = this.scmLock.tryLock();
                if (locked) {
                    TestScmReadWriteLock.isLocked = locked;
                    get(TestScmReadWriteLock.resultList);
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

    static class ReadWriteLock_ThreadF extends CommonThread {

        public ReadWriteLock_ThreadF(ScmLock lock) {
            super(lock);
        }

        @Override
        public void run() {
            while (TestScmReadWriteLock.lockStep.get() != 0) {
            }
            boolean locked = false;
            try {
                locked = this.scmLock.tryLock();
                if (locked) {
                    TestScmReadWriteLock.isLocked = locked;
                    add(TestScmReadWriteLock.resultList);
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
