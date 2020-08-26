//package com.sequoiacm.server.lock.test;
//
//import java.io.IOException;
//import java.io.InputStream;
//import java.util.List;
//import java.util.Timer;
//
//import org.apache.zookeeper.KeeperException;
//import org.apache.zookeeper.ZooKeeper;
//import org.apache.zookeeper.data.Stat;
//import org.junit.Assert;
//import org.junit.BeforeClass;
//import org.junit.Test;
//
//import com.sequoiacm.exception.ScmServerException;
//import com.sequoiacm.server.lock.ScmLock;
//import com.sequoiacm.server.lock.zookeeper.ScmZKClearNodesJob;
//import com.sequoiacm.server.lock.zookeeper.ScmZKReadWriteLock;
//import com.sequoiacm.server.lock.zookeeper.ZKConnectionPool;
//import com.sequoiacm.server.util.PropertiesUtils;
//
//public class TestClearNodesJob {
//
//    private static final String ID_PREFIX = "node_";
//    private static final String TYPE = "file";
//    private static final String ROOT_PATH = "/scm";
//    private static final String PATH_SEPARATOR= "/";
//    private static final String TYPE_PATH = ROOT_PATH + PATH_SEPARATOR + TYPE;
//    private static final int NODES_NUM = 500;
//
//    private Timer timer;
//    private static long period;
//
//    @BeforeClass
//    public static void setUp() {
//        loadConfiguration();
//        period = PropertiesUtils.getZKClearNodePeriod();
//        clearEnv();
//    }
//
//    @Test
//    public void testClearAtBeginning() {
//        generateIdNodes(NODES_NUM);
//        checkIdNodesNums(NODES_NUM);
//
//        startClearNodesTask(period);
//        sleep(period);
//
//        checkIdNodesNums(0);
//        cancelClearNodesTast();
//    }
//
//    @Test
//    public void testClearAtMidWay() {
//        startClearNodesTask(period);
//
//        generateIdNodes(NODES_NUM);
//        checkIdNodesNums(NODES_NUM);
//
//        sleep(period);
//
//        checkIdNodesNums(0);
//        cancelClearNodesTast();
//    }
//
//    private static void loadConfiguration() {
//        final String SYSCONF = "sysconf.properties";
//        InputStream sysconfInputStream =
//                TestClearNodesJob.class.getClassLoader().getResourceAsStream(SYSCONF);
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
//    private void generateIdNodes(int nodeNums) {
//        for (int index = 0; index < nodeNums; index++) {
//            try {
//                ScmLock reader = new ScmZKReadWriteLock(ID_PREFIX + index, TYPE).readLock();
//                reader.lock();
//                reader.unlock();
//            }
//            catch (ScmServerException ine) {
//                ine.printStackTrace();
//            }
//        }
//    }
//
//    private void checkIdNodesNums(int expectNums) {
//        ZooKeeper zk = null;
//        try {
//            zk = ZKConnectionPool.getConnection();
//            Stat stat = zk.exists(TYPE_PATH, false);
//            if (null == stat) {
//                Assert.fail("idNode stat is null");
//            }
//            else {
//                Assert.assertEquals(expectNums, stat.getNumChildren());
//            }
//        }
//        catch (Exception e) {
//            Assert.fail(e.toString());
//        }
//        finally {
//            ZKConnectionPool.free(zk);
//        }
//    }
//
//    private void startClearNodesTask(long period) {
//        timer = new Timer();
//        ScmZKClearNodesJob task = new ScmZKClearNodesJob();
//        timer.schedule(task, 0, period);
//    }
//
//    private void cancelClearNodesTast() {
//        timer.cancel();
//    }
//
//    private void sleep(long time) {
//        try {
//            Thread.sleep(time);
//        }
//        catch (InterruptedException e) {
//            Assert.fail(e.toString());
//        }
//    }
//
//    private static void clearEnv() {
//        ZooKeeper zk = null;
//        try {
//            zk = ZKConnectionPool.getConnection();
//            Stat stat = zk.exists(TYPE_PATH, false);
//            if (null != stat && 0 != stat.getNumChildren()) {
//                List<String> idNodeList = zk.getChildren(TYPE_PATH, false);
//                for (String idNode : idNodeList) {
//                    zk.delete(TYPE_PATH + PATH_SEPARATOR + idNode, -1);
//                }
//            }
//        }
//        catch (KeeperException ke) {
//            Assert.fail(ke.toString());
//        }
//        catch (InterruptedException ie) {
//            Assert.fail(ie.toString());
//        }
//        finally {
//            ZKConnectionPool.free(zk);
//        }
//    }
//}
