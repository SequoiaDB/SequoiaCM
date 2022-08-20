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
    private static int printCount = 10000;
    private int innerCounter = 0;
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

    public long cleanPathSetNode(CuratorFramework client, int maxChildNum, long maxResidualTime)
            throws Exception {
        long count = 0;
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
                        count += cleanChildNode(client, ckStat, path, maxChildNum, maxResidualTime);
                    }
                }
            }
            finally {
                cleanPathBuffer.putSet(cleanPathSet);
            }
        }

        return count;
    }

    private long cleanChildNode(CuratorFramework client, Stat ckStat, String path, int maxChildNum,
                                long maxResidualTime) throws Exception {
        if (ckStat.getNumChildren() <= maxChildNum) {
            return 0;
        }
        long ephemeralOwner = ckStat.getEphemeralOwner();
        if (ephemeralOwner == 0) {
            List<String> childNodes = null;
            try {
                childNodes = CuratorZKBase.getChildren(client, path);
            }
            catch (NoNodeException e) {
                return 0;
            }
            int count = 0;
            for (String childNode : childNodes) {
                String childParentPath = path + CuratorLockProperty.LOCK_PATH_SEPERATOR + childNode;
                Stat childStat = CuratorZKBase.exists(client, childParentPath);
                if (childStat == null) {
                    continue;
                }
                if (childStat.getNumChildren() == 0) {
                    if (checkDeleteNode(client, childParentPath, maxResidualTime, childStat)) {
                        count++;
                    }
                }
            }
            return count;
        }
        return 0;
    }

    private boolean checkDeleteNode(CuratorFramework client, String path, long maxResidualTime,
                                    Stat ckStat) throws Exception {
        if (ckStat.getEphemeralOwner() == 0) {
            long zkNodeMtimeMS = ckStat.getMtime();
            long sysTimeMS = System.currentTimeMillis();
            long nodeLifeTimeMS = sysTimeMS - zkNodeMtimeMS;
            if (nodeLifeTimeMS > maxResidualTime) {
                try {
                    CuratorZKBase.deleteNode(client, path);
                    innerCounter++;
                    if (innerCounter % printCount == 0) {
                        logger.info("zk cleaner delete 1w node count");
                    }
                    return true;
                }
                catch (NoNodeException e) {
                    return false;
                }
                catch (NotEmptyException e) {
                    return false;
                }
            }
        }
        return false;
    }

    public long clearResidualNode(CuratorFramework client, String path, long maxResidualTime)
            throws Exception {
        long count = 0;
        Stat ckStat = CuratorZKBase.exists(client, path);
        if (ckStat == null) {
            return count;
        }
        if (ckStat.getNumChildren() == 0) {
            if (checkDeleteNode(client, path, maxResidualTime, ckStat)) {
                count++;
            }
        }
        else {
            List<String> childNodes = null;
            try {
                childNodes = CuratorZKBase.getChildren(client, path);
            }
            catch (NoNodeException e) {
                return count;
            }
            for (String childNode : childNodes) {
                String childParentPath = path + CuratorLockProperty.LOCK_PATH_SEPERATOR + childNode;
                count += clearResidualNode(client, childParentPath, maxResidualTime);
            }
            if (checkDeleteNode(client, path, maxResidualTime, ckStat)) {
                count++;
            }
        }
        return count;
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
