package com.sequoiacm.infrastructure.security.auth;

import com.sequoiacm.infrastructrue.security.core.ScmUser;
import com.sequoiacm.infrastructure.common.timer.ScmTimer;
import com.sequoiacm.infrastructure.common.timer.ScmTimerFactory;
import com.sequoiacm.infrastructure.common.timer.ScmTimerTask;
import com.sequoiacm.infrastructure.feign.ScmFeignClient;
import com.sequoiacm.infrastructure.feign.ScmFeignException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.locks.ReentrantReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock.ReadLock;
import java.util.concurrent.locks.ReentrantReadWriteLock.WriteLock;

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
            sessionIdMapSessionCache.put(sessionId, sessionCache);
            UserCache userCache = userNameMapUserCache.get(scmUser.getUsername());
            if (userCache == null) {
                userCache = new UserCache(userWrapper, cacheTimeToLive);
                userNameMapUserCache.put(scmUser.getUsername(), userCache);
            }
            else {
                userCache.refresh(userWrapper);
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
            sessionIdMapSessionCache.remove(sessionId);
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
            if (sessionCache == null || sessionCache.isCacheTimeout()) {
                return null;
            }
            UserCache userCache = userNameMapUserCache.get(sessionCache.getUserName());
            if (userCache == null || userCache.isCacheTimeout()) {
                return null;
            }
            return userCache.getUser();
        }
        finally {
            rLock.unlock();
        }
    }

    public void purgeTimeoutCache() {
        Set<String> sessionIdSet;
        Set<String> userNameSet;
        ReadLock rLock = rwLock.readLock();
        rLock.lock();
        try {
            sessionIdSet = new HashSet<>(sessionIdMapSessionCache.keySet());
            userNameSet = new HashSet<>(userNameMapUserCache.keySet());
        }
        finally {
            rLock.unlock();
        }

        for (String sessionId : sessionIdSet) {
            SessionCache sessionCache;
            rLock.lock();
            try {
                sessionCache = sessionIdMapSessionCache.get(sessionId);
            }
            finally {
                rLock.unlock();
            }

            if (sessionCache != null && sessionCache.isCacheTimeout()) {
                removeCache(sessionId);
            }
        }

        for (String userName : userNameSet) {
            UserCache userCache;
            rLock.lock();
            try {
                userCache = userNameMapUserCache.get(userName);
            }
            finally {
                rLock.unlock();
            }

            if (userCache != null && userCache.isCacheTimeout()) {
                removeUserCache(userName);
            }
        }
    }

    public void removeUserCache(String userName) {
        WriteLock wLock = rwLock.writeLock();
        wLock.lock();
        try {
            userNameMapUserCache.remove(userName);
        }
        finally {
            wLock.unlock();
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
    private long cacheTimeout;
    private long updateTime;
    private ScmUserWrapper userWrapper;

    public UserCache(ScmUserWrapper userWrapper, long cacheTimeout) {
        this.userWrapper = userWrapper;
        this.updateTime = System.currentTimeMillis();
        this.cacheTimeout = cacheTimeout;
    }

    public void refresh(ScmUserWrapper userWrapper) {
        this.userWrapper = userWrapper;
        this.updateTime = System.currentTimeMillis();
    }

    public boolean isCacheTimeout() {
        return System.currentTimeMillis() - updateTime > cacheTimeout;
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
