package com.sequoiacm.infrastructure.lock.curator;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

import org.apache.curator.framework.CuratorFramework;
import org.apache.zookeeper.data.Stat;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.sequoiacm.infrastructure.common.timer.ScmTimer;
import com.sequoiacm.infrastructure.common.timer.ScmTimerFactory;
import com.sequoiacm.infrastructure.lock.LockFactory;
import com.sequoiacm.infrastructure.lock.ScmLock;
import com.sequoiacm.infrastructure.lock.ScmReadWriteLock;

public class CuratorLockFactory implements LockFactory {
    private static final Logger logger = LoggerFactory.getLogger(CuratorLockFromReadWrite.class);
    private Random ran = new Random();
    private List<CuratorFramework> clientList = new ArrayList<>();
    private String zkConnStr;
    private ScmTimer t;

    public CuratorLockFactory(String zkConnStr, int zkClientNum) throws Exception {
        try {
            for (int i = 0; i < zkClientNum; i++) {
                this.clientList.add(CuratorLockTools.createClient(zkConnStr));
            }

            this.zkConnStr = zkConnStr;
        }
        catch (Exception e) {
            logger.error("Fail to init curator client:zkConnStr={},zkClientNum={}", zkConnStr,
                    zkClientNum);
            close();
            throw e;
        }
    }

    @Override
    public ScmLock createLock(String[] lockPath) {
        String path = CuratorLockTools.getLockPath(lockPath);
        return new CuratorLock(getCuratorClient(this.clientList), path);
    }

    @Override
    public ScmReadWriteLock createReadWriteLock(String[] lockPath) {
        String path = CuratorLockTools.getLockPath(lockPath);
        return new CuratorReadWriteLock(getCuratorClient(this.clientList), path);
    }

    public CuratorFramework getCuratorClient(List<CuratorFramework> clientList) {
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

    @Override
    public void clearNode(String[] lockPath) {
        String path = CuratorLockTools.getLockPath(lockPath);
        CuratorFramework client = getCuratorClient(this.clientList);
        try {
            Stat stat = client.checkExists().forPath(path);
            if (stat != null) {
                client.delete().guaranteed().deletingChildrenIfNeeded().withVersion(-1)
                        .inBackground().forPath(path);
            }
        }
        catch (Exception e) {
            logger.warn("Fail to clear curator cient node!", e);
        }
    }

    public List<CuratorFramework> getClientList() {
        return clientList;
    }

    @Override
    public void startCleanJob(long period, long maxResidualTime) {
        if (null != t) {
            t.cancel();
            t = null;
        }

        t = ScmTimerFactory.createScmTimer();
        CuratorCleanJob task = new CuratorCleanJob(zkConnStr, maxResidualTime);
        t.schedule(task, 0, period);
    }

}
