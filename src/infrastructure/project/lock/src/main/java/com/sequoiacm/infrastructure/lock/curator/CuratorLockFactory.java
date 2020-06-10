package com.sequoiacm.infrastructure.lock.curator;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

import org.apache.curator.framework.CuratorFramework;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.sequoiacm.infrastructure.common.timer.ScmTimer;
import com.sequoiacm.infrastructure.common.timer.ScmTimerFactory;
import com.sequoiacm.infrastructure.lock.LockFactory;
import com.sequoiacm.infrastructure.lock.ScmLock;
import com.sequoiacm.infrastructure.lock.ScmReadWriteLock;
import com.sequoiacm.infrastructure.lock.ZKCompaticify;

public class CuratorLockFactory implements LockFactory {
    private static final Logger logger = LoggerFactory.getLogger(CuratorLockFromReadWrite.class);
    private Random ran = new Random();
    private List<CuratorFramework> clientList = new ArrayList<>();
    private String zkConnStr;
    private boolean enableContainer;
    private ScmTimer t;

    public CuratorLockFactory(String zkUrl, int zkClientNum) throws Exception {
        this.zkConnStr = zkUrl;
        // SEQUOIACM-485:enableContainer is false, zk server version below 3.5
        enableContainer = ZKCompaticify.enableContainer(zkUrl);
        try {
            logger.info("start init zookeeper client: zkUrl={}, zkClientNum={}, enableContainer={}",
                    zkUrl, zkClientNum, enableContainer);
            for (int i = 0; i < zkClientNum; i++) {
                this.clientList.add(CuratorLockTools.createClient(zkUrl, enableContainer));
            }
        }
        catch (Exception e) {
            logger.error(
                    "Fail to init curator client:zkConnStr={}, zkClientNum={}, enableContainer",
                    zkUrl, zkClientNum, enableContainer);
            close();
            throw e;
        }
    }

    @Override
    public ScmLock createLock(String[] lockPath) {
        String path = CuratorLockTools.getLockPath(lockPath);
        return new CuratorLock(getCuratorClient(), path);
    }

    @Override
    public ScmReadWriteLock createReadWriteLock(String[] lockPath) {
        String path = CuratorLockTools.getLockPath(lockPath);
        return new CuratorReadWriteLock(getCuratorClient(), path);
    }

    public CuratorFramework getCuratorClient() {
        int idx = ran.nextInt(clientList.size());
        CuratorFramework client = clientList.get(idx);
        if (!client.getZookeeperClient().isConnected()) {
            // reselect client
            idx = ran.nextInt(clientList.size());
            client = clientList.get(idx);
        }
        return client;
    }

    @Override
    public void close() {
        for (int i = 0; i < this.clientList.size(); i++) {
            if (this.clientList.get(i) != null) {
                closeClient(clientList.get(i));
            }
        }

        clientList.clear();
    }

    private void closeClient(CuratorFramework client) {
        try {
            if (null != client) {
                client.close();
            }
        }
        catch (Exception e) {
            logger.warn("Fail to close curator client!", e);
        }
    }

    public List<CuratorFramework> getClientList() {
        return clientList;
    }

    @Override
    public void startCleanJob(long period, long maxResidualTime, int maxChildNum, int cleanCount) {
        if (!enableContainer) {
            if (null != t) {
                t.cancel();
                t = null;
            }

            t = ScmTimerFactory.createScmTimer();
            CuratorCleanJob task = new CuratorCleanJob(zkConnStr, enableContainer, maxResidualTime,
                    maxChildNum, cleanCount, true);
            t.schedule(task, 0, period);
            logger.info(
                    "start clean job cleanJobResidualTime={}, maxChildNum={},cleanJobPeriod={}, cleanAllCountPeriod={}",
                    maxResidualTime, maxChildNum, period, cleanCount);

        }
        else {
            // enableContainer is true, clean only once
            logger.info("clean all old zookeeper node");
            CuratorCleanJob task = new CuratorCleanJob(zkConnStr, enableContainer, 0, 0, 0, false);
            new Thread(task).start();

        }
    }

}
