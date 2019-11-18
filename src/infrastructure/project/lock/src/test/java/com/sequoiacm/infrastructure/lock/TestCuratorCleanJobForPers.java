//package com.sequoiacm.infrastructure.lock;
//
//import java.util.ArrayList;
//import java.util.List;
//
//import org.apache.curator.framework.CuratorFramework;
//import org.apache.zookeeper.data.Stat;
//import org.junit.AfterClass;
//import org.junit.Assert;
//import org.junit.Before;
//import org.junit.BeforeClass;
//import org.junit.Test;
//
//import com.sequoiacm.infrastructure.lock.curator.CuratorLockFactory;
//import com.sequoiacm.infrastructure.lock.curator.CuratorZKBase;
//
//public class TestCuratorCleanJobForPers {
//    private static String zkConnStr = "192.168.20.92:2181";
//    private static final String separator = "/";
//    private static String rootPath = "/scm/lock";
//    private static long period = 2 * 1000;
//    private static long maxResidualTime = 10 * 1000;
//    private static long creatNodeSleepTimeMS = 5 * 1000;
//    private static long lockThreadSleepTimeMS = 5 * 1000;
//    private static long sysTimeErrorMS = 12*1000;
//    private static CuratorLockFactory curatorLockFactory;
//    private static CuratorFramework client;
//    private static List<Integer> resultList;
//    private static List<String> ck1_childNodes;
//    private static List<String> ck2_childNodes;
//    private static List<String> ck1_childNodes_name;
//    private static List<String> ck2_childNodes_name;
//
//    @BeforeClass
//    public static void setUp() {
//        resultList = new ArrayList<Integer>();
//        ck1_childNodes_name = new ArrayList<String>();
//        ck2_childNodes_name = new ArrayList<String>();
//        
//        try {
//            curatorLockFactory = new CuratorLockFactory(zkConnStr, 1);
//            client = curatorLockFactory.getCuratorClient(curatorLockFactory.getClientList());
//            cleanNode(rootPath);
//        }
//        catch (Exception e) {
//            e.printStackTrace();
//        }
//    }
//
//    public static void cleanNode(String path) throws Exception{
//        List<String> childNodes = CuratorZKBase.getChildren(client, path);
//        if(!childNodes.isEmpty()){
//            String childPath = "";
//            for(String child : childNodes){
//                childPath = path + separator + child;
//                client.delete().deletingChildrenIfNeeded().forPath(childPath);
//            }
//        }
//    }
//
//    @Before
//    public void init() {
//        resultList.clear();
//        clear();
//    }
//
//    @AfterClass
//    public static void tearDown() {
//        if (null != client) {
//            client.close();
//        }
//    }
//    
//    public void clear(){
//        if (ck1_childNodes != null && ck1_childNodes.size() > 0) {
//            ck1_childNodes.clear();
//        }
//        if (ck2_childNodes != null && ck2_childNodes.size() > 0) {
//            ck2_childNodes.clear();
//        }
//        if (ck1_childNodes_name != null && ck1_childNodes_name.size() > 0) {
//            ck1_childNodes_name.clear();
//        }
//        if (ck2_childNodes_name != null && ck2_childNodes_name.size() > 0) {
//            ck2_childNodes_name.clear();
//        }
//    }
//
//    @Test
//    public void testPerNodeClean() {
//        try {
//            String ckPath1 = rootPath + separator + "dir";
//            String ckPath2 = rootPath + separator + "dir" + separator + "dir2_1";
//            String[] lockPath1 = { "dir", "dir2_1"};
//            String[] lockPath2 = { "dir", "dir2_1", "dir3"};
//            String[] lockPath3 = { "dir", "dir2_2"};
//            long createTime = System.currentTimeMillis();
//            ScmLock mtl = curatorLockFactory.createLock(lockPath1);
//            ScmLock mt2 = curatorLockFactory.createLock(lockPath2);
//            ScmLock mt3 = curatorLockFactory.createLock(lockPath3);
//            
//            MutexLock_ThreadA1 mtla11 = new MutexLock_ThreadA1(mtl);
//            mtla11.start();
//            Thread.sleep(creatNodeSleepTimeMS);
//            MutexLock_ThreadA1 mtla12 = new MutexLock_ThreadA1(mt2);
//            mtla12.start();
//            Thread.sleep(creatNodeSleepTimeMS);
//            MutexLock_ThreadA1 mtla13 = new MutexLock_ThreadA1(mt3);
//            mtla13.start();
//            
//            mtla11.join();
//            mtla12.join();
//            mtla13.join();
//            
//            ck1_childNodes_name.add("dir2_2");
//            ck1_childNodes_name.add("dir2_1");
//            ck2_childNodes_name.add("dir3");
//            ck1_childNodes = CuratorZKBase.getChildren(client, ckPath1);
//            ck2_childNodes = CuratorZKBase.getChildren(client, ckPath2);
//            Assert.assertEquals(2, ck1_childNodes.size());
//            Assert.assertEquals(1, ck2_childNodes.size());
//            Assert.assertEquals(ck1_childNodes_name, ck1_childNodes);
//            Assert.assertEquals(ck2_childNodes_name, ck2_childNodes);
//            
//            CleanThread cleanThread = new CleanThread();
//            cleanThread.start();
//            //cleanThread.join();
//            
//            //sysTimeErrorMS
//            Stat ckStat = CuratorZKBase.exists(client, ckPath2);
//            long zkNodeMtimeMS = ckStat.getMtime();
//            long timeError = zkNodeMtimeMS-createTime;
//            if(timeError>0){
//                Thread.sleep(timeError-period);
//            }
//            
//            //Thread.sleep(sysTimeErrorMS);
//            Thread.sleep(period * 3);
//            
//            clear();
//            
//            ck1_childNodes_name.add("dir2_2");
//            ck1_childNodes = CuratorZKBase.getChildren(client, ckPath1);
//            Assert.assertEquals(1, ck1_childNodes.size());
//            Assert.assertEquals(ck1_childNodes_name, ck1_childNodes);
//            
//            Thread.sleep(period * 3);
//            
//            clear();
//            
//            ck1_childNodes = CuratorZKBase.getChildren(client, rootPath);
//            Assert.assertEquals(0, ck1_childNodes.size());
//            
//            //Thread.sleep(mainThreadSleepTimeMS);
//        }
//        catch (Exception e) {
//            e.printStackTrace();
//        }
//    }
//
//    static class CleanThread extends Thread {
//
//        @Override
//        public void run() {
//            curatorLockFactory.startCleanJob(period, maxResidualTime);
//        }
//    }
//    
//    /* Define Implement Thread */
//    static class CommonThread extends Thread {
//        protected ScmLock scmLock;
//
//        public CommonThread(ScmLock scmLock) {
//            this.scmLock = scmLock;
//        }
//
//        public void dowork(List<Integer> list) {
//            for (int i = 1; i < 101; i++) {
//                if (i == 50) {
//                    try {
//                        Thread.sleep(lockThreadSleepTimeMS);
//                    }
//                    catch (InterruptedException e) {
//                        e.printStackTrace();
//                    }
//                }
//                list.add(i);
//            }
//        }
//    }
//
//    static class MutexLock_ThreadA1 extends CommonThread {
//
//        public MutexLock_ThreadA1(ScmLock lock) {
//            super(lock);
//        }
//
//        @Override
//        public void run() {
//            try {
//                this.scmLock.lock();
//                dowork(resultList);
//            }
//            catch (Exception e) {
//                e.printStackTrace();
//            }
//            finally {
//                this.scmLock.unlock();
//            }
//        }
//    }
//}
