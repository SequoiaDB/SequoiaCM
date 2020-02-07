package com.sequoiacm.infrastructure.lock.curator;

import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;
import java.util.concurrent.locks.ReentrantLock;

import org.apache.curator.framework.CuratorFramework;
import org.apache.zookeeper.KeeperException.NoNodeException;
import org.apache.zookeeper.KeeperException.NotEmptyException;
import org.apache.zookeeper.data.Stat;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class CuratorZKCleaner {
    private static final Logger logger = LoggerFactory.getLogger(CuratorZKCleaner.class);
    // buffer record clean path
    private CleanPathBuffer cleanPathBuffer;
    private boolean usePathBuffer = false;
    // clean task is single thread, have no need lock
    private static CuratorZKCleaner curatorZKCleaner = new CuratorZKCleaner();

    private CuratorZKCleaner() {
    }

    public static CuratorZKCleaner getInstance() {
        return curatorZKCleaner;
    }

    public void useBufferPath() {
        cleanPathBuffer = new CleanPathBuffer();
        usePathBuffer = true;
    }

    public void putPath(String path) {
        if (usePathBuffer) {
            cleanPathBuffer.put(path);
        }
    }

    public void cleanPathSetNode(CuratorFramework client, int maxChildNum, long maxResidualTime)
            throws Exception {
        if (usePathBuffer) {
            Set<String> cleanPathSet = cleanPathBuffer.getAndClear();
            try {
                Iterator<String> it = cleanPathSet.iterator();
                while (it.hasNext()) {
                    String path = it.next();
                    Stat ckStat = CuratorZKBase.exists(client, path);
                    if (ckStat == null) {
                        it.remove();
                    }
                    else {
                        cleanChildNode(client, ckStat, path, maxChildNum, maxResidualTime);
                    }
                }
            }
            finally {
                cleanPathBuffer.putSet(cleanPathSet);
            }

        }
    }

    private void cleanChildNode(CuratorFramework client, Stat ckStat, String path, int maxChildNum,
            long maxResidualTime) throws Exception {
        if (ckStat.getNumChildren() <= maxChildNum) {
            return;
        }
        long ephemeralOwner = ckStat.getEphemeralOwner();
        if (ephemeralOwner == 0) {
            List<String> childNodes = null;
            try {
                childNodes = CuratorZKBase.getChildren(client, path);
            }
            catch (NoNodeException e) {
                return;
            }
            for (String childNode : childNodes) {
                String childParentPath = path + CuratorLockProperty.LOCK_PATH_SEPERATOR + childNode;
                Stat childStat = CuratorZKBase.exists(client, childParentPath);
                if (childStat == null) {
                    continue;
                }
                if (childStat.getNumChildren() == 0) {
                    checkDeleteNode(client, childParentPath, maxResidualTime, childStat);
                }
            }
        }
    }

    private void checkDeleteNode(CuratorFramework client, String path, long maxResidualTime,
            Stat ckStat) throws Exception {
        if (ckStat.getEphemeralOwner() == 0) {
            long zkNodeMtimeMS = ckStat.getMtime();
            long sysTimeMS = System.currentTimeMillis();
            long nodeLifeTimeMS = sysTimeMS - zkNodeMtimeMS;
            if (nodeLifeTimeMS > maxResidualTime) {
                try {
                    CuratorZKBase.deleteNode(client, path);
                }
                catch (NoNodeException e) {
                    return;
                }
                catch (NotEmptyException e) {
                    return;
                }
            }
        }
    }

    public void clearResidualNode(CuratorFramework client, String path, long maxResidualTime)
            throws Exception {
        Stat ckStat = CuratorZKBase.exists(client, path);
        if (ckStat == null) {
            return;
        }
        if (ckStat.getNumChildren() == 0) {
            checkDeleteNode(client, path, maxResidualTime, ckStat);
        }
        else {
            List<String> childNodes = null;
            try {
                childNodes = CuratorZKBase.getChildren(client, path);
            }
            catch (NoNodeException e) {
                return;
            }
            for (String childNode : childNodes) {
                String childParentPath = path + CuratorLockProperty.LOCK_PATH_SEPERATOR + childNode;
                clearResidualNode(client, childParentPath, maxResidualTime);
            }
            checkDeleteNode(client, path, maxResidualTime, ckStat);
        }
    }

    class CleanPathBuffer {
        private Set<String> pathSet = new HashSet<String>();
        private ReentrantLock bufferLock = new ReentrantLock();

        public void put(String parentPath) {
            bufferLock.lock();
            try {
                pathSet.add(parentPath);
            }
            finally {
                bufferLock.unlock();
            }
        }

        public void putSet(Set<String> pathSet) {
            bufferLock.lock();
            try {
                pathSet.addAll(pathSet);
            }
            finally {
                bufferLock.unlock();
            }
        }

        public Set<String> getAndClear() {
            bufferLock.lock();
            try {
                Set<String> pathBuffer = new HashSet<String>(pathSet);
                pathSet.clear();
                return pathBuffer;
            }
            finally {
                bufferLock.unlock();
            }
        }
    }
}
