package com.sequoiacm.infrastructure.security.auth;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.locks.ReentrantReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock.ReadLock;
import java.util.concurrent.locks.ReentrantReadWriteLock.WriteLock;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.sequoiacm.infrastructrue.security.core.ScmUser;
import com.sequoiacm.infrastructure.common.timer.ScmTimer;
import com.sequoiacm.infrastructure.common.timer.ScmTimerFactory;
import com.sequoiacm.infrastructure.common.timer.ScmTimerTask;
import com.sequoiacm.infrastructure.feign.ScmFeignClient;
import com.sequoiacm.infrastructure.feign.ScmFeignException;

public class ScmSessionMgrWithSessionCache extends ScmSessionMgr {
    private static final Logger logger = LoggerFactory.getLogger(ScmSessionMgrWithSessionCache.class);
    private ReentrantReadWriteLock rwLock = new ReentrantReadWriteLock();
    private Map<String, SessionCache> sessionIdMapSessionCache = new HashMap<>();
    private Map<String, UserCache> userNameMapUserCache = new HashMap<>();
    private ScmTimer timer = ScmTimerFactory.createScmTimer();
    private long cacheTimeToLive;

    public ScmSessionMgrWithSessionCache(ScmFeignClient client, long cacheTimeToLive) {
        super(client);
        this.cacheTimeToLive = cacheTimeToLive;
        timer.schedule(new CleanCacheTask(this), 0, cacheTimeToLive / 2);
    }

    public void putCache(String sessionId, ScmUserWrapper userWrapper) {
        long timestamp = System.currentTimeMillis();
        ScmUser scmUser = userWrapper.getUser();
        SessionCache sessionCache = new SessionCache(cacheTimeToLive, timestamp,
                scmUser.getUsername());
        WriteLock wLock = rwLock.writeLock();
        wLock.lock();
        try {
            SessionCache oldSessionCache = sessionIdMapSessionCache.put(sessionId, sessionCache);
            UserCache userCache = userNameMapUserCache.get(scmUser.getUsername());
            if (oldSessionCache == null && userCache == null) {
                userCache = new UserCache(userWrapper);
                userCache.increaseRef();
                userNameMapUserCache.put(scmUser.getUsername(), userCache);
            }
            else if (oldSessionCache == null && userCache != null) {
                userCache.refresh(userWrapper);
                userCache.increaseRef();
            }
            else if (oldSessionCache != null && userCache != null) {
                userCache.refresh(userWrapper);
            }
            else {
                // oldSession != null && userCache == null
                logger.error("should not come here");
                userCache = new UserCache(userWrapper);
                userCache.increaseRef();
                userNameMapUserCache.put(scmUser.getUsername(), userCache);
            }
        }
        finally {
            wLock.unlock();
        }
    }

    private void removeCache(String sessionId) {
        WriteLock wLock = rwLock.writeLock();
        wLock.lock();
        try {
            SessionCache sessionCache = sessionIdMapSessionCache.remove(sessionId);
            if (sessionCache != null) {
                removeUserCache(sessionCache.getUserName());
            }
        }
        finally {
            wLock.unlock();
        }
    }

    private ScmUserWrapper getCache(String sessionId) {
        ReadLock rLock = rwLock.readLock();
        rLock.lock();
        try {
            SessionCache sessionCache = sessionIdMapSessionCache.get(sessionId);
            if (sessionCache == null) {
                return null;
            }
            if (sessionCache.isCacheTimeout()) {
                return null;
            }
            return userNameMapUserCache.get(sessionCache.getUserName()).getUser();
        }
        finally {
            rLock.unlock();
        }
    }

    public void purgeTimeoutCache() {
        Set<String> keys = null;
        ReadLock rLock = rwLock.readLock();
        rLock.lock();
        try {
            keys = new HashSet<>(sessionIdMapSessionCache.keySet());
        }
        finally {
            rLock.unlock();
        }

        WriteLock wLock = rwLock.writeLock();
        for (String key : keys) {
            SessionCache sessionCache = null;
            rLock.lock();
            try {
                sessionCache = sessionIdMapSessionCache.get(key);
            }
            finally {
                rLock.unlock();
            }

            if (sessionCache != null && sessionCache.isCacheTimeout()) {
                wLock.lock();
                try {
                    sessionCache = sessionIdMapSessionCache.get(key);
                    if (sessionCache != null && sessionCache.isCacheTimeout()) {
                        sessionIdMapSessionCache.remove(key);
                        removeUserCache(sessionCache.getUserName());
                    }
                }
                finally {
                    wLock.unlock();
                }

            }
        }
    }

    private void removeUserCache(String userName) {
        UserCache userCache = userNameMapUserCache.get(userName);
        if (userCache.decreaseRef() <= 0) {
            userNameMapUserCache.remove(userName);
        }
    }

    @Override
    public ScmUserWrapper getUserBySessionId(String sessionId) throws ScmFeignException {
        ScmUserWrapper userWrapper = getCache(sessionId);
        if (userWrapper == null) {
            userWrapper = super.getUserBySessionId(sessionId);
            putCache(sessionId, userWrapper);
        }
        return userWrapper;
    }

    @Override
    public void markSessionLogout(String sessionId) {
        removeCache(sessionId);
    }

    @Override
    public String toString() {
        return "sessionCacheSize=" + sessionIdMapSessionCache.size() + ",userCacheSize="
                + userNameMapUserCache.size();
    }

    @Override
    public void close() {
        super.close();
        this.timer.cancel();
    }
}

class UserCache {
    private long refCount;
    private ScmUserWrapper userWrapper;

    public UserCache(ScmUserWrapper userWrapper) {
        this.userWrapper = userWrapper;
    }

    public long increaseRef() {
        this.refCount++;
        return refCount;
    }

    public long decreaseRef() {
        this.refCount--;
        return refCount;
    }

    public void refresh(ScmUserWrapper userWrapper) {
        this.userWrapper = userWrapper;
    }

    public ScmUserWrapper getUser() {
        return userWrapper;
    }

    @Override
    public String toString() {
        return userWrapper.toString();
    }

}

class SessionCache {
    private long cacheTimeout;
    private String userName;
    private boolean isTimeout;
    private long createTime;

    public SessionCache(long cacheTimeout, long createTime, String userName) {
        this.createTime = createTime;
        this.cacheTimeout = cacheTimeout;
        this.userName = userName;
    }

    public boolean isCacheTimeout() {
        if (isTimeout) {
            return true;
        }

        if (System.currentTimeMillis() - createTime > cacheTimeout) {
            isTimeout = true;
            return true;
        }

        return false;
    }

    public String getUserName() {
        return userName;
    }

    @Override
    public String toString() {
        return userName;
    }

}

class CleanCacheTask extends ScmTimerTask {
    private ScmSessionMgrWithSessionCache cacheMgr;
    private static final Logger logger = LoggerFactory.getLogger(CleanCacheTask.class);

    public CleanCacheTask(ScmSessionMgrWithSessionCache cacheMgr) {
        this.cacheMgr = cacheMgr;
    }

    @Override
    public void run() {
        this.cacheMgr.purgeTimeoutCache();
        logger.debug(cacheMgr.toString());
    }

}
