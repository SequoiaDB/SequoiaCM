//package com.sequoiacm.infrastructure.lock;
//
//import java.util.ArrayList;
//import java.util.List;
//
//import org.apache.curator.framework.CuratorFramework;
//import org.junit.AfterClass;
//import org.junit.Assert;
//import org.junit.Before;
//import org.junit.BeforeClass;
//import org.junit.Test;
//
//import com.sequoiacm.infrastructure.lock.curator.CuratorLockFactory;
//import com.sequoiacm.infrastructure.lock.curator.CuratorZKBase;
//
//public class TestCuratorCleanJobForEphem {
//    private static String zkConnStr = "192.168.20.92:2181";
//    private static final String separator = "/";
//    private static String rootPath = "/scm/lock";
//    private static long period = 2 * 1000;
//    private static long maxResidualTime = 5 * 1000;
//    private static long lockThreadSleepTimeMS = 20 * 1000;
//    private static CuratorLockFactory curatorLockFactory;
//    private static CuratorFramework client;
//    private static List<Integer> resultList;
//    private static List<String> ck1_childNodes;
//    private static List<String> ck2_childNodes;
//
//    @BeforeClass
//    public static void setUp() {
//        resultList = new ArrayList<Integer>();
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
//    }
//    
//    @Test
//    public void testPerNodeClean() {
//        try {
//            String ckPath = rootPath + separator + "dir" + separator + "dir2";
//            String[] lockPath = { "dir", "dir2"};
//            ScmLock mtl = curatorLockFactory.createLock(lockPath);
//            
//            CleanThread cleanThread = new CleanThread();
//            cleanThread.start();
//            
//            MutexLock_ThreadA1 mtla11 = new MutexLock_ThreadA1(mtl);
//            mtla11.start();
//            
//            Thread.sleep(period * 3);
//            
//            ck1_childNodes = CuratorZKBase.getChildren(client, ckPath);
//            Assert.assertEquals(1, ck1_childNodes.size());
//            
//            mtla11.join();
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
