package com.sequoiacm.infrastructure.lock;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import org.apache.zookeeper.CreateMode;
import org.apache.zookeeper.KeeperException;
import org.apache.zookeeper.KeeperException.ConnectionLossException;
import org.apache.zookeeper.WatchedEvent;
import org.apache.zookeeper.Watcher;
import org.apache.zookeeper.Watcher.Event.KeeperState;
import org.apache.zookeeper.ZooDefs.Ids;
import org.apache.zookeeper.ZooKeeper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.sequoiacm.infrastructure.lock.curator.CuratorLockProperty;

public class ZKCompaticify {
    private static final Logger logger = LoggerFactory.getLogger(ZKCompaticify.class);

    public static boolean enableContainer(String zkConnStr) throws Exception {
        ZooKeeper zk = null;
        try {
            ZKWatcher watcher = new ZKWatcher(CuratorLockProperty.ZK_CONNECTION_TIMEOUTMS);
            zk = new ZooKeeper(zkConnStr, CuratorLockProperty.SESSIONTIMEOUTMS, watcher);
            if (!watcher.connNotify()) {
                logger.warn("zookeeper connection timeout: zkURL={}", zkConnStr);
                throw new ConnectionLossException();
            }
            String path = "/tmp_container";
            zk.create(path, "".getBytes(), Ids.OPEN_ACL_UNSAFE, CreateMode.CONTAINER);
            zk.delete(path, -1);
        }
        catch (KeeperException e) {
            if (e.code() == KeeperException.Code.UNIMPLEMENTED) {
                logger.info("create container node failed, zookeeper server version below 3.5");
                return false;
            }
            if (e.code() == KeeperException.Code.NODEEXISTS) {
                return true;
            }
            logger.warn("create container node failed, do not known zookeeper server version");
            throw e;
        }
        catch (Exception e) {
            logger.warn("create container node failed, do not known zookeeper server version");
            throw e;
        }
        finally {
            try {
                if (zk != null) {
                    zk.close();
                }
            }
            catch (Exception e) {
                logger.warn("fail to close zookeeper client!", e);
            }
        }
        return true;
    }
}

class ZKWatcher implements Watcher {
    private CountDownLatch countDownLatch = new CountDownLatch(1);
    private long timeout;

    public ZKWatcher(long timeout) {
        this.timeout = timeout;
    }

    @Override
    public void process(WatchedEvent event) {
        if (event.getState() == KeeperState.SyncConnected) {
            countDownLatch.countDown();
        }
    }

    public boolean connNotify() throws InterruptedException {
        boolean isConnSuccess = countDownLatch.await(timeout, TimeUnit.MILLISECONDS);
        return isConnSuccess;
    }
}
