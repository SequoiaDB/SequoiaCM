package com.sequoiacm.client.core;

import com.sequoiacm.client.exception.ScmException;
import com.sequoiacm.exception.ScmError;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;
import java.util.concurrent.locks.ReentrantLock;

public class ScmWorkspaceCache {

    private static final Logger logger = LoggerFactory.getLogger(ScmWorkspaceCache.class);

    /**
     * keepAliveTime is in seconds.
     */
    private volatile int keepAliveTime = 30;
    private volatile int maxCacheSize = 500;
    private HashMap<String, Long> wsCacheMap = new HashMap<String, Long>();
    private Queue<String> wsCacheQueue = new LinkedList<String>();
    private ReentrantLock lock = new ReentrantLock();

    public ScmWorkspaceCache() {

    }

    /**
     * Set new keepAliveTime.
     *
     * @param keepAliveTime
     *            keepAliveTime is in seconds.
     * @throws ScmException
     *             if error happens.
     * @since 3.2.0
     */
    public void setKeepAliveTime(int keepAliveTime) throws ScmException {
        if (keepAliveTime < 0) {
            throw new ScmException(ScmError.INVALID_ARGUMENT,
                    "keepAliveTime cannot be less than zero");
        }
        this.keepAliveTime = keepAliveTime;
        logger.debug("update keepAliveTime is success, new keepAliveTime = {}", this.keepAliveTime);
    }

    /**
     * Set new maxCacheSize.
     *
     * @param maxCacheSize
     *            maxCacheSize is in number.
     * @throws ScmException
     *             if error happens.
     * @since 3.2.0
     */
    public void setMaxCacheSize(int maxCacheSize) throws ScmException {
        if (maxCacheSize < 0) {
            throw new ScmException(ScmError.INVALID_ARGUMENT,
                    "maxCacheSize cannot be less than zero");
        }
        this.maxCacheSize = maxCacheSize;
        logger.debug("update maxCacheSize is success, new maxCacheSize = {}", this.maxCacheSize);
    }

    /**
     * Determine whether the workspace with the specified name exists.
     *
     * @param name
     *            This is the name of the workspace.
     * @return
     * @since 3.2.0
     */
    public boolean contains(String name) {
        Long time = wsCacheMap.get(name);
        if (time == null) {
            return false;
        }
        if (System.currentTimeMillis() > time) {
            lock.lock();
            try {
                wsCacheMap.remove(name);
                wsCacheQueue.remove(name);
            }
            finally {
                lock.unlock();
            }
            return false;
        }
        return true;
    }

    /**
     * Store the specified workspace name in the cache.
     *
     * @param name
     *            This is the name of the workspace.
     * @since 3.2.0
     */
    public void put(String name) {
        if (keepAliveTime != 0 && maxCacheSize != 0) {
            lock.lock();
            try {
                int c = wsCacheQueue.size() - maxCacheSize;
                while (c-- >= 0) {
                    wsCacheMap.remove(wsCacheQueue.poll());
                }
                if (!wsCacheMap.containsKey(name)) {
                    wsCacheQueue.add(name);
                }
                wsCacheMap.put(name, (System.currentTimeMillis() + keepAliveTime * 1000));
            }
            finally {
                lock.unlock();
            }
            logger.debug("put new cache is success, new cache = {}", name);
        }
    }

}
