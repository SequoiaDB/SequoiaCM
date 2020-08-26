//package com.sequoiacm.server.lock.test;
//
//import java.io.IOException;
//import java.io.InputStream;
//import java.util.ArrayList;
//import java.util.List;
//import java.util.concurrent.atomic.AtomicInteger;
//
//import org.apache.zookeeper.KeeperException;
//import org.apache.zookeeper.ZooKeeper;
//import org.junit.AfterClass;
//import org.junit.Assert;
//import org.junit.Before;
//import org.junit.Test;
//
//import com.sequoiacm.exception.ScmServerException;
//import com.sequoiacm.server.lock.ScmLock;
//import com.sequoiacm.server.lock.ScmReadWriteLock;
//import com.sequoiacm.server.lock.zookeeper.ScmZKReadWriteLock;
//import com.sequoiacm.server.lock.zookeeper.ZKConnectionPool;
//import com.sequoiacm.server.util.PropertiesUtils;
//
//public class TestReadWriteLock {
//
//    private static final String ID = "node";
//    private static final String TYPE = "file";
//    private static final int RANDOM_NUM = 50;
//
//    private static final String PATH_SEPARATOR = "/";
//    private static final String NODE_SEPARATOR = "id_";
//    private static final String rootPath = "/scm";
//    private static final String basePath = rootPath + PATH_SEPARATOR + TYPE; // /scm/file
//    private static final String nodePath = basePath + PATH_SEPARATOR + NODE_SEPARATOR + ID; // /scm/file/id_node
//
//    @Before
//    public void setUp() {
//        loadConfiguration();
//        clearDirectory();
//    }
//
//    private void loadConfiguration() {
//        final String SYSCONF = "sysconf.properties";
//        InputStream sysconfInputStream = this.getClass().getClassLoader().getResourceAsStream(SYSCONF);
//        PropertiesUtils.configure(sysconfInputStream);
//
//        try {
//            sysconfInputStream.close();
//        }
//        catch (IOException ie) {
//            Assert.fail(ie.toString());
//        }
//    }
//
//    private void clearDirectory() {
//        ZooKeeper zk = ZKConnectionPool.getConnection();
//        try {
//            if (null != zk.exists(rootPath, null)) {
//                recurDelete(zk, rootPath);
//            }
//        }
//        catch (Exception e) {
//            Assert.fail("clean environment error" + e.toString());
//        }
//        finally {
//            ZKConnectionPool.free(zk);
//        }
//    }
//
//    @AfterClass
//    public static void tearDown() {
//        ZKConnectionPool.close();;
//    }
//
//    private void recurDelete(ZooKeeper zk, String path)
//            throws KeeperException, InterruptedException {
//
//        List<String> nodeList = zk.getChildren(path, null);
//        if (0 == nodeList.size()) {
//            zk.delete(path, -1);
//            return ;
//        }
//        else {
//            for (String subNode : nodeList) {
//                recurDelete(zk, path + PATH_SEPARATOR + subNode);
//            }
//        }
//    }
//
//
//    /**
//     * Read-Read, use lock().
//     */
//    @Test
//    public void testRR_Lock() throws InterruptedException {
//        ScmReadWriteLock rwa = new ScmZKReadWriteLock(ID, TYPE);
//        ScmReadWriteLock rwb = new ScmZKReadWriteLock(ID, TYPE);
//
//        ScmLock readera = rwa.readLock();
//        ScmLock readerb = rwb.readLock();
//
//        NonMutexThreadA_Lock nmta = new NonMutexThreadA_Lock(readera);
//        NonMutexThreadB_Lock nmtb = new NonMutexThreadB_Lock(readerb);
//
//        nmta.start();
//        nmtb.start();
//
//        nmta.join();
//        nmtb.join();
//
//        if (0 < nmta.errInfo.size()) {
//            Assert.fail(nmta.errInfo.get(0));
//        }
//
//        if (0 < nmtb.errInfo.size()) {
//            Assert.fail(nmtb.errInfo.get(0));
//        }
//    }
//
//    /**
//     * Read-Write, use lock().
//     */
//    @Test
//    public void testRW_Lock() throws InterruptedException {
//        ScmReadWriteLock rw = new ScmZKReadWriteLock(ID, TYPE);
//
//        ScmLock ra = rw.readLock();
//        ScmLock wb = rw.writeLock();
//
//        MutexThreadA_Lock mta = new MutexThreadA_Lock(ra);
//        MutexThreadB_Lock mtb = new MutexThreadB_Lock(wb);
//
//        mta.start();
//        mtb.start();
//
//        mta.join();
//        mtb.join();
//
//        if (0 < mta.errInfo.size()) {
//            Assert.fail(mta.errInfo.get(0));
//        }
//
//        if (0 < mtb.errInfo.size()) {
//            Assert.fail(mtb.errInfo.get(0));
//        }
//    }
//
//    /**
//     * Write-Read, use lock()
//     */
//    @Test
//    public void testWR_Lock() throws InterruptedException {
//        ScmReadWriteLock rw = new ScmZKReadWriteLock(ID, TYPE);
//
//        ScmLock wa = rw.writeLock();
//        ScmLock rb = rw.readLock();
//
//        MutexThreadA_Lock mta = new MutexThreadA_Lock(wa);
//        MutexThreadB_Lock mtb = new MutexThreadB_Lock(rb);
//
//        mta.start();
//        mtb.start();
//
//        mta.join();
//        mtb.join();
//
//        if (0 < mta.errInfo.size()) {
//            Assert.fail(mta.errInfo.get(0));
//        }
//
//        if (0 < mtb.errInfo.size()) {
//            Assert.fail(mtb.errInfo.get(0));
//        }
//    }
//
//    /**
//     * Write-Write, use lock()
//     */
//    @Test
//    public void testWW_Lock() throws InterruptedException {
//        ScmReadWriteLock rwa = new ScmZKReadWriteLock(ID, TYPE);
//        ScmReadWriteLock rwb = new ScmZKReadWriteLock(ID, TYPE);
//
//        ScmLock wa = rwa.writeLock();
//        ScmLock wb = rwb.writeLock();
//
//        MutexThreadA_Lock mta = new MutexThreadA_Lock(wa);
//        MutexThreadB_Lock mtb = new MutexThreadB_Lock(wb);
//
//        mta.start();
//        mtb.start();
//
//        mta.join();
//        mtb.join();
//
//        if (0 < mta.errInfo.size()) {
//            Assert.fail(mta.errInfo.get(0));
//        }
//
//        if (0 < mtb.errInfo.size()) {
//            Assert.fail(mtb.errInfo.get(0));
//        }
//    }
//
//    /**
//     * Random, use lock()
//     */
//    @Test
//    public void testRandom_Lock() throws InterruptedException {
//        int readerCount = (int)(Math.random() * RANDOM_NUM);
//        int writerCount = (int)(Math.random() * RANDOM_NUM);
//
//        List<RandomThread_Lock> rtList = new ArrayList<RandomThread_Lock>(readerCount + writerCount);
//        for (int index = 0; index < readerCount; index++) {
//            rtList.add(new RandomThread_Lock(new ScmZKReadWriteLock(ID, TYPE).readLock()));
//        }
//        for (int index = 0; index < writerCount; index++) {
//            rtList.add(new RandomThread_Lock(new ScmZKReadWriteLock(ID, TYPE).writeLock()));
//        }
//
//        for (RandomThread_Lock rt : rtList) {
//            rt.start();
//        }
//
//        for (RandomThread_Lock rt : rtList) {
//            rt.join();
//        }
//
//        boolean noException = true;
//        List<String> errInfoList = new ArrayList<String>();
//        for (RandomThread_Lock rt : rtList) {
//            if (0 != rt.errInfo.size()) {
//                errInfoList.add(rt.errInfo.get(0));
//                noException = false;
//            }
//        }
//
//        if (!noException) {
//            StringBuffer buf = new StringBuffer();
//            buf.append("testRandom_Lock() readerCount:" + readerCount + ", writerCount:" + writerCount + "\n");
//            for (String errMsg : errInfoList) {
//                buf.append(errMsg);
//                buf.append("\n");
//            }
//            Assert.fail(buf.toString());
//        }
//    }
//
//    /**
//     * Reader-Reader, use tryLock().
//     */
//    @Test
//    public void testRR_TryLock() throws InterruptedException {
//        ScmReadWriteLock rwa = new ScmZKReadWriteLock(ID, TYPE);
//        ScmReadWriteLock rwb = new ScmZKReadWriteLock(ID, TYPE);
//
//        ScmLock ra = rwa.readLock();
//        ScmLock rb = rwb.readLock();
//
//        NonMutexThreadA_TryLock nmta = new NonMutexThreadA_TryLock(ra);
//        NonMutexThreadB_TryLock nmtb = new NonMutexThreadB_TryLock(rb);
//
//        nmta.start();
//        nmtb.start();
//
//        nmta.join();
//        nmtb.join();
//
//        if (0 < nmta.errInfo.size()) {
//            Assert.fail(nmta.errInfo.get(0));
//        }
//
//        if (0 < nmtb.errInfo.size()) {
//            Assert.fail(nmtb.errInfo.get(0));
//        }
//    }
//
//    /**
//     * Read-Write, use tryLock().
//     */
//    @Test
//    public void testRW_TryLock() throws InterruptedException {
//        ScmReadWriteLock rw = new ScmZKReadWriteLock(ID, TYPE);
//        ScmLock ra = rw.readLock();
//        ScmLock wb = rw.writeLock();
//
//        MutexThreadA_TryLock mta = new MutexThreadA_TryLock(ra);
//        MutexThreadB_TryLock mtb = new MutexThreadB_TryLock(wb);
//
//        mta.start();
//        mtb.start();
//
//        mta.join();
//        mtb.join();
//
//        if (0 < mta.errInfo.size()) {
//            Assert.fail(mta.errInfo.get(0));
//        }
//
//        if (0 < mtb.errInfo.size()) {
//            Assert.fail(mtb.errInfo.get(0));
//        }
//    }
//
//    /**
//     * Write-Read, use tryLock()
//     */
//    @Test
//    public void testWR_TryLock() throws InterruptedException {
//        ScmReadWriteLock rw = new ScmZKReadWriteLock(ID, TYPE);
//
//        ScmLock wa = rw.writeLock();
//        ScmLock rb = rw.readLock();
//
//        MutexThreadA_TryLock mta = new MutexThreadA_TryLock(wa);
//        MutexThreadB_TryLock mtb = new MutexThreadB_TryLock(rb);
//
//        mta.start();
//        mtb.start();
//
//        mta.join();
//        mtb.join();
//
//        if (0 < mta.errInfo.size()) {
//            Assert.fail(mta.errInfo.get(0));
//        }
//
//        if (0 < mtb.errInfo.size()) {
//            Assert.fail(mtb.errInfo.get(0));
//        }
//    }
//
//    /**
//     * Write-Write, use tryLock()
//     */
//    @Test
//    public void testWW_TryLock() throws InterruptedException {
//        ScmReadWriteLock rwa = new ScmZKReadWriteLock(ID, TYPE);
//        ScmReadWriteLock rwb = new ScmZKReadWriteLock(ID, TYPE);
//
//        ScmLock wa = rwa.writeLock();
//        ScmLock wb = rwb.writeLock();
//
//        MutexThreadA_TryLock mta = new MutexThreadA_TryLock(wa);
//        MutexThreadB_TryLock mtb = new MutexThreadB_TryLock(wb);
//
//        mta.start();
//        mtb.start();
//
//        mta.join();
//        mtb.join();
//
//        if (0 < mta.errInfo.size()) {
//            Assert.fail(mta.errInfo.get(0));
//        }
//
//        if (0 < mtb.errInfo.size()) {
//            Assert.fail(mtb.errInfo.get(0));
//        }
//    }
//
//    /**
//     * Random, use tryLock()
//     */
//    @Test
//    public void testRandom_TryLock() throws InterruptedException {
//        int readerCount = (int) (Math.random() * RANDOM_NUM);
//        int writerCount = (int) (Math.random() * RANDOM_NUM);
//
//        List<RandomThread_TryLock> rtList = new ArrayList<RandomThread_TryLock>(readerCount + writerCount);
//        for (int index = 0; index < readerCount; index++) {
//            rtList.add(new RandomThread_TryLock(new ScmZKReadWriteLock(ID, TYPE).readLock()));
//        }
//
//        for (int index = 0; index < writerCount; index++) {
//            rtList.add(new RandomThread_TryLock(new ScmZKReadWriteLock(ID, TYPE).writeLock()));
//        }
//
//        for (RandomThread_TryLock rt : rtList) {
//            rt.start();
//        }
//
//        for (RandomThread_TryLock rt : rtList) {
//            rt.join();
//        }
//
//        boolean noException = true;
//        List<String> errInfoList = new ArrayList<String>();
//        for (RandomThread_TryLock rt : rtList) {
//            if (0 < rt.errInfo.size()) {
//                errInfoList.add(rt.errInfo.get(0));
//                noException = false;
//            }
//        }
//
//        if (!noException) {
//            StringBuffer buf = new StringBuffer();
//            buf.append("testRandom_TryLock readerCount:" + readerCount + ", writerCount:" + writerCount + "\n");
//            for (String errMsg : errInfoList) {
//                buf.append(errMsg);
//                buf.append("\n");
//            }
//            Assert.fail(buf.toString());
//        }
//    }
//
//    /**
//     * Read-Read, use lock(Time)
//     */
//    @Test
//    public void testRR_LockWithTime() throws InterruptedException {
//        ScmReadWriteLock rwa = new ScmZKReadWriteLock(ID, TYPE);
//        ScmReadWriteLock rwb = new ScmZKReadWriteLock(ID, TYPE);
//
//        ScmLock ra = rwa.readLock();
//        ScmLock rb = rwb.readLock();
//
//        NonMutexThreadA_LockWithTime nmta = new NonMutexThreadA_LockWithTime(ra);
//        NonMutexThreadB_LockWithTime nmtb = new NonMutexThreadB_LockWithTime(rb);
//
//        nmta.start();
//        nmtb.start();
//
//        nmta.join();
//        nmtb.join();
//
//        if (0 < nmta.errInfo.size()) {
//            Assert.fail(nmta.errInfo.get(0));
//        }
//        if (0 < nmtb.errInfo.size()) {
//            Assert.fail(nmtb.errInfo.get(0));
//        }
//    }
//
//
//    /**
//     * Read-Write, use lock(Time)
//     */
//    @Test
//    public void testRW_LockWithTime() throws InterruptedException {
//        ScmReadWriteLock rw = new ScmZKReadWriteLock(ID, TYPE);
//
//        ScmLock ra = rw.readLock();
//        ScmLock wb = rw.writeLock();
//
//        MutexThreadA_LockWithTime mta = new MutexThreadA_LockWithTime(ra);
//        MutexThreadB_LockWithTime mtb = new MutexThreadB_LockWithTime(wb);
//
//        mta.start();
//        mtb.start();
//
//        mta.join();
//        mtb.join();
//
//        if (0 < mta.errInfo.size()) {
//            Assert.fail(mta.errInfo.get(0));
//        }
//        if (0 < mtb.errInfo.size()) {
//            Assert.fail(mtb.errInfo.get(0));
//        }
//    }
//
//    /**
//     * Write-Read, use lock(Time)
//     */
//    @Test
//    public void testWR_LockWithTime() throws InterruptedException {
//        ScmReadWriteLock rw = new ScmZKReadWriteLock(ID, TYPE);
//
//        ScmLock wa = rw.writeLock();
//        ScmLock rb = rw.readLock();
//
//        MutexThreadA_LockWithTime mta = new MutexThreadA_LockWithTime(wa);
//        MutexThreadB_LockWithTime mtb = new MutexThreadB_LockWithTime(rb);
//
//        mta.start();
//        mtb.start();
//
//        mta.join();
//        mtb.join();
//
//        if (0 < mta.errInfo.size()) {
//            Assert.fail(mta.errInfo.get(0));
//        }
//        if (0 < mtb.errInfo.size()) {
//            Assert.fail(mtb.errInfo.get(0));
//        }
//    }
//
//    /**
//     * Write-Write, use lock(Time)
//     */
//    @Test
//    public void testWW_LockWithTime() throws InterruptedException {
//        ScmReadWriteLock rwa = new ScmZKReadWriteLock(ID, TYPE);
//        ScmReadWriteLock rwb = new ScmZKReadWriteLock(ID, TYPE);
//
//        ScmLock wa = rwa.writeLock();
//        ScmLock wb = rwb.writeLock();
//
//        MutexThreadA_LockWithTime mta = new MutexThreadA_LockWithTime(wa);
//        MutexThreadB_LockWithTime mtb = new MutexThreadB_LockWithTime(wb);
//
//        mta.start();
//        mtb.start();
//
//        mta.join();
//        mtb.join();
//
//        if (0 < mta.errInfo.size()) {
//            Assert.fail(mta.errInfo.get(0));
//        }
//        if (0 < mtb.errInfo.size()) {
//            Assert.fail(mtb.errInfo.get(0));
//        }
//    }
//
//    /**
//     * Random, use lock(Time)
//     */
//    public void testRandom_LockWithTime() throws InterruptedException {
//        int readerCount = (int) (Math.random() * RANDOM_NUM);
//        int writerCount = (int) (Math.random() * RANDOM_NUM);
//
//        List<RandomThread_LockWithTime> rtList =
//                new ArrayList<RandomThread_LockWithTime>(readerCount + writerCount);
//        for (int index = 0; index < readerCount; index++) {
//            rtList.add(new RandomThread_LockWithTime(new ScmZKReadWriteLock(ID, TYPE).readLock()));
//        }
//        for (int index = 0; index < writerCount; index++) {
//            rtList.add(new RandomThread_LockWithTime(new ScmZKReadWriteLock(ID, TYPE).writeLock()));
//        }
//
//        for (RandomThread_LockWithTime rt : rtList) {
//            rt.start();
//        }
//
//        for (RandomThread_LockWithTime rt : rtList) {
//            rt.join();
//        }
//
//        boolean noException = true;
//        List<String> errInfoList = new ArrayList<String>();
//        for (RandomThread_LockWithTime rt : rtList) {
//            if (0 < rt.errInfo.size()) {
//                errInfoList.add(rt.errInfo.get(0));
//                noException = false;
//            }
//        }
//
//        if (!noException) {
//            StringBuffer buf = new StringBuffer();
//            buf.append("RandomThread_LockWithTime readerCount:" + readerCount + ", writerCount:" + writerCount + "\n");
//            for (String errMsg : errInfoList) {
//                buf.append(errMsg);
//                buf.append("\n");
//            }
//            Assert.fail(buf.toString());
//        }
//    }
//}
//
//
///* Define Implement Thread*/
//class CommonThread extends Thread {
//    protected static AtomicInteger step = new AtomicInteger();
//    protected static final long smallSleepTime = 1000; // 100
//    protected static final long bigSleepTime = 10 * smallSleepTime;
//    private static final int MAX_WAIT_NUM = 10;
//    protected ScmLock lock;
//
//    public List<String> errInfo = new ArrayList<String>();
//
//    protected CommonThread(ScmLock lock) {
//        this.lock = lock;
//    }
//
//    protected static boolean checkOtherHealth(int waitNum) {
//        if (waitNum > MAX_WAIT_NUM) {
//            step.set(-1);
//            return false;
//        }
//
//        boolean health;
//        if (-1 == step.get()) {
//            health = false;
//        }
//        else {
//            health = true;
//        }
//        return health;
//    }
//}
//
//class NonMutexThreadA_Lock extends CommonThread {
//
//    public NonMutexThreadA_Lock(ScmLock lock) {
//        super(lock);
//    }
//
//    @Override
//    public void run() {
//        try {
//            int waitNum;
//            step.set(0);
//            lock.lock();
//            step.incrementAndGet(); // 1
//
//            Thread.sleep(bigSleepTime);
//
//            waitNum = 1;
//            while (2 != step.get()) {
//                if (!checkOtherHealth(waitNum)) {
//                    errInfo.add("NonMutexThreadB_Lock error");
//                    Assert.fail("NonMutexThreadB_Lock error");
//                }
//                ++waitNum;
//                Thread.sleep(smallSleepTime);
//            }
//
//            lock.unlock();
//            step.incrementAndGet();  // 3
//            Thread.sleep(bigSleepTime);
//
//            waitNum = 1;
//            while (4 != step.get()) {
//                if (!checkOtherHealth(waitNum)) {
//                    errInfo.add("NonMutexThreadB_Lock error");
//                    Assert.fail("NonMutexThreadB_Lock error");
//                }
//                ++waitNum;
//                Thread.sleep(smallSleepTime);
//            }
//        }
//        catch (InterruptedException e) {
//            errInfo.add("NonMutexThreadA_Lock-InterruptedException:errormsg=" + e.getMessage());
//            Assert.fail("NonMutexThreadA_Lock-InterruptedException:errormsg=" + e.getMessage());
//        }
//        catch (ScmServerException e) {
//            errInfo.add("NonMutexThreadA_Lock-ScmServerException:errorCode=" + e.getError() +
//                    ", errormsg=" + e.getMessage());
//            Assert.fail("NonMutexThreadA_Lock-ScmServerException:errorCode=" + e.getError() +
//                    ", errormsg=" + e.getMessage());
//        }
//        finally {
//            step.set(-1);
//        }
//    }
//}
//
//class NonMutexThreadB_Lock extends CommonThread {
//
//    public NonMutexThreadB_Lock(ScmLock lock) {
//        super(lock);
//    }
//
//    @Override
//    public void run() {
//        try {
//            int waitNum;
//            waitNum = 1;
//            while (1 != step.get()) {
//                if (!checkOtherHealth(waitNum)) {
//                    errInfo.add("NonMutexThreadA_Lock error");
//                    Assert.fail("NonMutexThreadA_Lock error");
//                }
//                ++waitNum;
//                Thread.sleep(smallSleepTime);
//            }
//
//            lock.lock();
//            step.incrementAndGet();  // 2
//
//            Thread.sleep(bigSleepTime);
//
//            waitNum = 1;
//            while (3 != step.get()) {
//                if (!checkOtherHealth(waitNum)) {
//                    errInfo.add("NonMutexThreadA_Lock error");
//                    Assert.fail("NonMutexThreadA_Lock error");
//                }
//                ++waitNum;
//                Thread.sleep(smallSleepTime);
//            }
//
//            lock.unlock();
//            step.incrementAndGet(); // 4
//
//            Thread.sleep(bigSleepTime);
//        }
//        catch (InterruptedException e) {
//            errInfo.add("NonMutexThreadB_Lock-InterruptedException:errormsg=" + e.getMessage());
//            Assert.fail("NonMutexThreadB_Lock-InterruptedException:errormsg=" + e.getMessage());
//        }
//        catch (ScmServerException e) {
//            errInfo.add("NonMutexThreadB_Lock-ScmServerException:errorCode=" + e.getError() +
//                    ", errormsg=" + e.getMessage());
//            Assert.fail("NonMutexThreadB_Lock-ScmServerException:errorCode=" + e.getError() +
//                    ", errormsg=" + e.getMessage());
//        }
//        finally {
//            step.set(-1);
//        }
//    }
//}
//
//
//class MutexThreadA_Lock extends CommonThread {
//    public MutexThreadA_Lock(ScmLock lock) {
//        super(lock);
//    }
//
//    @Override
//    public void run() {
//        try {
//            int waitNum;
//            step.set(0);
//            lock.lock();
//            step.incrementAndGet(); // 1
//
//            Thread.sleep(bigSleepTime);
//
//            if (1 != step.get()) {
//                errInfo.add("MutexThreadA_Lock should not get lock");
//                Assert.fail("MutexThreadA_Lock should not get lock");
//            }
//
//            lock.unlock();
//            step.incrementAndGet(); // 2
//
//            Thread.sleep(bigSleepTime);
//
//            waitNum = 1;
//            while (4 != step.get()) {
//                if (!checkOtherHealth(waitNum)) {
//                    errInfo.add("MutexThreadB_Lock error");
//                    Assert.fail("MutexThreadB_Lock error");
//                }
//                Thread.sleep(smallSleepTime);
//                ++waitNum;
//            }
//        }
//        catch (InterruptedException e) {
//            errInfo.add("MutexThreadA_Lock-InterruptedException:errormsg=" + e.getMessage());
//            Assert.fail("MutexThreadA_Lock-InterruptedException:errormsg=" + e.getMessage());
//        }
//        catch (ScmServerException e) {
//            errInfo.add("MutexThreadA_Lock-ScmServerException:errorCode=" + e.getError() +
//                    ", errormsg=" + e.getMessage());
//            Assert.fail("MutexThreadA_Lock-ScmServerException:errorCode=" + e.getError() +
//                    ", errormsg=" + e.getMessage());
//        }
//        finally {
//            step.set(-1);
//        }
//    }
//}
//
//
//class MutexThreadB_Lock extends CommonThread {
//
//    public MutexThreadB_Lock(ScmLock lock) {
//        super(lock);
//    }
//
//    @Override
//    public void run() {
//        try {
//            int waitNum;
//            waitNum = 1;
//            while (1 != step.get()) {
//                if (!checkOtherHealth(waitNum)) {
//                    errInfo.add("MutexThreadA_Lock error");
//                    Assert.fail("MutexThreadA_Lock error");
//                }
//                ++waitNum;
//                Thread.sleep(smallSleepTime);
//            }
//
//            lock.lock();
//            step.incrementAndGet(); // 3
//            lock.unlock();
//            step.incrementAndGet(); // 4
//
//            Thread.sleep(bigSleepTime);
//        }
//        catch (InterruptedException e) {
//            errInfo.add("MutexThreadB_Lock-InterruptedException:errormsg=" + e.getMessage());
//            Assert.fail("MutexThreadB_Lock-InterruptedException:errormsg=" + e.getMessage());
//        }
//        catch (ScmServerException e) {
//            errInfo.add("MutexThreadB_Lock-ScmServerException:errorCode=" + e.getError() +
//                    ", errormsg=" + e.getMessage());
//            Assert.fail("MutexThreadB_Lock-ScmServerException:errorCode=" + e.getError() +
//                    ", errormsg=" + e.getMessage());
//        }
//        finally {
//            step.set(-1);
//        }
//    }
//}
//
//class RandomThread_Lock extends CommonThread {
//
//    public RandomThread_Lock(ScmLock lock) {
//        super(lock);
//    }
//
//    @Override
//    public void run() {
//        try {
//            this.lock.lock();
//        }
//        catch (ScmServerException ie) {
//            errInfo.add("RandomThread" + Thread.currentThread().getId() + "_Lock-ScmServerException:errorCode=" +
//                    ie.getError() + ", errormsg=" + ie.getMessage());
//            Assert.fail("RandomThread" + Thread.currentThread().getId() + "_Lock-ScmServerException:errorCode=" +
//                    ie.getError() + ", errormsg=" + ie.getMessage());
//        }
//        finally {
//            this.lock.unlock();
//        }
//    }
//}
//
//class NonMutexThreadA_TryLock extends CommonThread {
//    public NonMutexThreadA_TryLock(ScmLock lock) {
//        super(lock);
//    }
//
//    @Override
//    public void run() {
//
//        try {
//            int waitNum;
//            step.set(0);
//
//            if(lock.tryLock()) {
//                step.incrementAndGet(); // 1
//
//                waitNum = 0;
//                while (2 != step.get()) {
//                    if (!checkOtherHealth(waitNum)) {
//                        Assert.fail("NonMutexThreadB_TryLock error");
//                        errInfo.add("NonMutexThreadB_TryLock error");
//                    }
//                    ++waitNum;
//                    Thread.sleep(smallSleepTime);
//                }
//
//                lock.unlock();
//                step.incrementAndGet(); // 3
//
//                waitNum = 0;
//                while (4 != step.get()) {
//                    if (!checkOtherHealth(waitNum)) {
//                        Assert.fail("NonMutexThreadB_TryLock error");
//                        errInfo.add("NonMutexThreadB_TryLock error");
//                    }
//                    ++waitNum;
//                    Thread.sleep(smallSleepTime);
//                }
//            }
//        }
//        catch (InterruptedException e) {
//            errInfo.add("NonMutexThreadA_TryLock-InterruptedException:errormsg=" + e.getMessage());
//            Assert.fail("NonMutexThreadA_TryLock-InterruptedException:errormsg=" + e.getMessage());
//        }
//        catch (ScmServerException e) {
//            errInfo.add("NonMutexThreadA_TryLock-ScmServerException:errorCode=" + e.getError() +
//                    ", errormsg=" + e.getMessage());
//            Assert.fail("NonMutexThreadA_TryLock-ScmServerException:errorCode=" + e.getError() +
//                    ", errormsg=" + e.getMessage());
//        }
//        finally {
//            step.set(-1);
//        }
//    }
//}
//
//class NonMutexThreadB_TryLock extends CommonThread {
//    public NonMutexThreadB_TryLock(ScmLock lock) {
//        super(lock);
//    }
//
//    @Override
//    public void run() {
//        try {
//            int waitNum;
//
//            waitNum = 1;
//            while (1 != step.get()) {
//                if (!checkOtherHealth(waitNum)) {
//                    Assert.fail("NonMutexThreadA_TryLock error");
//                    errInfo.add("NonMutexThreadA_TryLock error");
//                }
//                ++waitNum;
//                Thread.sleep(smallSleepTime);
//            }
//
//            if (lock.tryLock()) {
//                step.incrementAndGet(); // 2
//
//                waitNum = 1;
//                while (3 != step.get()) {
//                    if (!checkOtherHealth(waitNum)) {
//                        Assert.fail("NonMutexThreadA_TryLock error");
//                        errInfo.add("NonMutexThreadA_TryLock error");
//                    }
//                    ++waitNum;
//                    Thread.sleep(smallSleepTime);
//                }
//
//                lock.unlock();
//
//                step.incrementAndGet(); // 4
//                Thread.sleep(bigSleepTime);
//            }
//        }
//        catch (InterruptedException e) {
//            errInfo.add("NonMutexThreadB_TryLock-InterruptedException:errormsg=" + e.getMessage());
//            Assert.fail("NonMutexThreadB_TryLock-InterruptedException:errormsg=" + e.getMessage());
//        }
//        catch (ScmServerException e) {
//            errInfo.add("NonMutexThreadB_TryLock-ScmServerException:errorCode=" + e.getError() +
//                    ", errormsg=" + e.getMessage());
//            Assert.fail("NonMutexThreadB_TryLock-ScmServerException:errorCode=" + e.getError() +
//                    ", errormsg=" + e.getMessage());
//        }
//        finally {
//            step.set(-1);
//        }
//    }
//}
//
//class MutexThreadA_TryLock extends CommonThread {
//
//    protected MutexThreadA_TryLock(ScmLock lock) {
//        super(lock);
//    }
//
//    @Override
//    public void run() {
//
//        try {
//            int waitNum;
//            step.set(0);
//            if (lock.tryLock()) {
//                step.incrementAndGet(); // 1
//                Thread.sleep(bigSleepTime);
//
//                if (2 != step.get()) {
//                    errInfo.add("MutexThreadB_TryLock error");
//                    Assert.fail("MutexThreadB_TryLock error");
//                }
//                lock.unlock();
//            }
//        }
//        catch (InterruptedException e) {
//            errInfo.add("MutexThreadA_TryLock-InterruptedException:errormsg=" + e.getMessage());
//            Assert.fail("MutexThreadA_TryLock-InterruptedException:errormsg=" + e.getMessage());
//        }
//        catch (ScmServerException e) {
//            errInfo.add("MutexThreadA_TryLock-ScmServerException:errorCode=" + e.getError() +
//                    ", errormsg=" + e.getMessage());
//            Assert.fail("MutexThreadA_TryLock-ScmServerException:errorCode=" + e.getError() +
//                    ", errormsg=" + e.getMessage());
//        }
//        finally {
//            step.set(-1);
//        }
//    }
//}
//
//class MutexThreadB_TryLock extends CommonThread {
//
//    protected MutexThreadB_TryLock(ScmLock lock) {
//        super(lock);
//    }
//
//    @Override
//    public void run() {
//
//        try {
//            int waitNum;
//            waitNum = 1;
//            while (1 != step.get()) {
//                if (!checkOtherHealth(waitNum)) {
//                    Assert.fail("NonMutexThreadA_TryLock error");
//                    errInfo.add("NonMutexThreadA_TryLock error");
//                }
//                ++waitNum;
//                Thread.sleep(smallSleepTime);
//            }
//
//            if (lock.tryLock()) {
//                step.set(3); // 3;
//                lock.unlock();
//            }
//            else {
//                step.set(2); // 2;
//            }
//
//            Thread.sleep(bigSleepTime);
//        }
//        catch (InterruptedException e) {
//            errInfo.add("MutexThreadB_TryLock-InterruptedException:errormsg=" + e.getMessage());
//            Assert.fail("MutexThreadB_TryLock-InterruptedException:errormsg=" + e.getMessage());
//        }
//        catch (ScmServerException e) {
//            errInfo.add("MutexThreadB_TryLock-ScmServerException:errorCode=" + e.getError() +
//                    ", errormsg=" + e.getMessage());
//            Assert.fail("MutexThreadB_TryLock-ScmServerException:errorCode=" + e.getError() +
//                    ", errormsg=" + e.getMessage());
//        }
//        finally {
//            step.set(-1);
//        }
//    }
//}
//
//class RandomThread_TryLock extends CommonThread {
//
//    public RandomThread_TryLock(ScmLock lock) {
//        super(lock);
//    }
//
//    @Override
//    public void run() {
//        try {
//            if (lock.tryLock()) {
//                lock.unlock();
//            }
//        }
//        catch (ScmServerException ie) {
//            errInfo.add("RandomThread" + Thread.currentThread().getId() + "_TryLock-ScmServerException:errorCode="
//                    + ie.getError() + ", errormsg=" + ie.getMessage());
//            Assert.fail("RandomThread" + Thread.currentThread().getId() + "_TryLock-ScmServerException:errorCode="
//                    + ie.getError() + ", errormsg=" + ie.getMessage());
//        }
//    }
//}
//
//class NonMutexThreadA_LockWithTime extends CommonThread {
//
//    protected NonMutexThreadA_LockWithTime(ScmLock lock) {
//        super(lock);
//    }
//
//    @Override
//    public void run() {
//        try {
//            int waitNum;
//            step.set(0);
//
//            if (lock.lock(smallSleepTime)) {
//                step.incrementAndGet(); // 1
//
//                waitNum = 1;
//                while (2 != step.get()) {
//                    if (!checkOtherHealth(waitNum)) {
//                        Assert.fail("NonMutexThreadB_LockWithLongTime error");
//                        errInfo.add("NonMutexThreadB_LockWithLongTime error");
//                    }
//                    ++waitNum;
//                    Thread.sleep(smallSleepTime);
//                }
//
//                lock.unlock();
//                step.incrementAndGet(); // 3
//
//                waitNum = 0;
//                while (4 != step.get()) {
//                    if (!checkOtherHealth(waitNum)) {
//                        Assert.fail("NonMutexThreadB_TryLock error");
//                        errInfo.add("NonMutexThreadB_TryLock error");
//                    }
//                    ++waitNum;
//                    Thread.sleep(smallSleepTime);
//                }
//            }
//        }
//        catch (InterruptedException e) {
//            errInfo.add("NonMutexThreadA_LockWithLongTime-InterruptedException:errormsg=" + e.getMessage());
//            Assert.fail("NonMutexThreadA_LockWithLongTime-InterruptedException:errormsg=" + e.getMessage());
//        }
//        catch (ScmServerException e) {
//            errInfo.add("NonMutexThreadA_LockWithLongTime-ScmServerException:errorCode=" +
//                    e.getError() + ", errormsg=" + e.getMessage());
//            Assert.fail("NonMutexThreadA_LockWithLongTime-ScmServerException:errorCode=" +
//                    e.getError() + ", errormsg=" + e.getMessage());
//        }
//        finally {
//            step.set(-1);
//        }
//    }
//}
//
//class NonMutexThreadB_LockWithTime extends CommonThread {
//
//    protected NonMutexThreadB_LockWithTime(ScmLock lock) {
//        super(lock);
//    }
//
//    @Override
//    public void run() {
//
//        try {
//            int waitNum;
//
//            waitNum = 1;
//            while (1 != step.get()) {
//                if (!checkOtherHealth(waitNum)) {
//                    errInfo.add("NonMutexThreadA_LockWithLongTime error");
//                    Assert.fail("NonMutexThreadA_LockWithLongTime error");
//                }
//                ++waitNum;
//                Thread.sleep(smallSleepTime);
//            }
//
//            if (lock.lock(bigSleepTime)) {
//                step.incrementAndGet(); // 2
//
//                while (3 != step.get()) {
//                    if (!checkOtherHealth(waitNum)) {
//                        errInfo.add("NonMutexThreadA_LockWithLongTime error");
//                        Assert.fail("NonMutexThreadA_LockWithLongTime error");
//                    }
//                    ++waitNum;
//                    Thread.sleep(smallSleepTime);
//                }
//
//                lock.unlock();
//                step.incrementAndGet(); // 4
//
//                Thread.sleep(bigSleepTime);
//            }
//        }
//        catch (InterruptedException e) {
//            errInfo.add("NonMutexThreadB_LockWithLongTime-InterruptedException:errormsg=" + e.getMessage());
//            Assert.fail("NonMutexThreadB_LockWithLongTime-InterruptedException:errormsg=" + e.getMessage());
//        }
//        catch (ScmServerException e) {
//            errInfo.add("NonMutexThreadB_LockWithLongTime-ScmServerException:errorCode=" +
//                    e.getError() + ", errormsg=" + e.getMessage());
//            Assert.fail("NonMutexThreadB_LockWithLongTime-ScmServerException:errorCode=" +
//                    e.getError() + ", errormsg=" + e.getMessage());
//        }
//        finally {
//            step.set(-1);
//        }
//    }
//}
//
//class MutexThreadA_LockWithTime extends CommonThread {
//
//    protected MutexThreadA_LockWithTime(ScmLock lock) {
//        super(lock);
//    }
//
//    @Override
//    public void run() {
//
//        try {
//            int waitNum;
//            step.set(0);
//
//            if (lock.lock(smallSleepTime)) {
//                step.incrementAndGet(); // 1
//
//                waitNum = 1;
//                while (2 != step.get()) {
//                    if (!checkOtherHealth(waitNum)) {
//                        errInfo.add("MutexThreadB_LockWithTime error");
//                        Assert.fail("MutexThreadB_LockWithTime error");
//                    }
//                    ++waitNum;
//                    Thread.sleep(smallSleepTime);
//                }
//
//                lock.unlock();
//            }
//        }
//        catch (InterruptedException e) {
//            errInfo.add("MutexThreadA_LockWithTime-InterruptedException:errormsg=" + e.getMessage());
//            Assert.fail("MutexThreadA_LockWithTime-InterruptedException:errormsg=" + e.getMessage());
//        }
//        catch (ScmServerException e) {
//            errInfo.add("MutexThreadA_LockWithTime-ScmServerException:errorCode=" +
//                    e.getError() + ", errormsg=" + e.getMessage());
//            Assert.fail("MutexThreadA_LockWithTime-ScmServerException:errorCode=" +
//                    e.getError() + ", errormsg=" + e.getMessage());
//        }
//        finally {
//            step.set(-1);
//        }
//    }
//}
//
//class MutexThreadB_LockWithTime extends CommonThread {
//    protected MutexThreadB_LockWithTime(ScmLock lock) {
//        super(lock);
//    }
//
//    @Override
//    public void run() {
//        try {
//            int waitNum;
//
//            waitNum = 1;
//            while (1 != step.get()) {
//                if (!checkOtherHealth(waitNum)) {
//                    errInfo.add("MutexThreadA_LockWithTime error");
//                    Assert.fail("MutexThreadA_LockWithTime error");
//                }
//                ++waitNum;
//                Thread.sleep(smallSleepTime);
//            }
//
//            if (lock.lock(smallSleepTime)) {
//                step.set(3);
//                lock.unlock();
//            }
//            else {
//                step.set(2);
//            }
//
//            Thread.sleep(bigSleepTime);
//        }
//        catch (InterruptedException e) {
//            errInfo.add("MutexThreadB_LockWithTime-InterruptedException:errormsg=" + e.getMessage());
//            Assert.fail("MutexThreadB_LockWithTime-InterruptedException:errormsg=" + e.getMessage());
//        }
//        catch (ScmServerException e) {
//            errInfo.add("MutexThreadB_LockWithTime-ScmServerException:errorCode=" +
//                    e.getError() + ", errormsg=" + e.getMessage());
//            Assert.fail("MutexThreadB_LockWithTime-ScmServerException:errorCode=" +
//                    e.getError() + ", errormsg=" + e.getMessage());
//        }
//        finally {
//            step.set(-1);
//        }
//    }
//}
//
//class RandomThread_LockWithTime extends CommonThread {
//
//    protected RandomThread_LockWithTime(ScmLock lock) {
//        super(lock);
//    }
//
//    @Override
//    public void run() {
//        try {
//            if(lock.lock(smallSleepTime)) {
//                lock.unlock();
//            }
//        }
//        catch (ScmServerException ie) {
//            errInfo.add("RandomThread" + Thread.currentThread().getId()
//                    + "_LockWithTime-ScmServerException:errorCode="
//                    + ie.getError() + ", errormsg=" + ie.getMessage());
//            Assert.fail("RandomThread" + Thread.currentThread().getId()
//                    + "_LockWithTime-ScmServerException:errorCode="
//                    + ie.getError() + ", errormsg=" + ie.getMessage());
//        }
//    }
//}
