package com.sequoiacm.om.omserver.session;

import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;

import javax.annotation.PreDestroy;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import com.sequoiacm.infrastructure.common.timer.ScmTimer;
import com.sequoiacm.infrastructure.common.timer.ScmTimerFactory;
import com.sequoiacm.infrastructure.common.timer.ScmTimerTask;
import com.sequoiacm.om.omserver.config.ScmOmServerConfig;

// TODO:考虑容器自带的session管理
@Component
public class ScmOmSessionMgr {
    private static final Logger logger = LoggerFactory.getLogger(ScmOmSessionMgr.class);
    private Map<String, ScmOmSession> sessions = new HashMap<>();
    private ReadWriteLock rwLock = new ReentrantReadWriteLock();
    private int sessionMaxInactiveInterval; // ms
    private ScmTimer timer;

    @Autowired
    public ScmOmSessionMgr(ScmOmServerConfig omserverConfig) {
        timer = ScmTimerFactory.createScmTimer();
        sessionMaxInactiveInterval = omserverConfig.getSessionKeepAliveTime() * 1000;
        int period = sessionMaxInactiveInterval / 2;
        timer.schedule(new ScmTimerTask() {
            @Override
            public void run() {
                cleanExpireSession();
            }
        }, period, period);
    }

    ScmOmSession _getSession(String sessionId) {
        Lock rLock = rwLock.readLock();
        rLock.lock();
        try {
            return sessions.get(sessionId);
        }
        finally {
            rLock.unlock();
        }
    }

    public ScmOmSession getSession(String sessionId) {
        ScmOmSession session = _getSession(sessionId);
        if (session != null) {
            session.touch();
        }
        return session;
    }

    public void saveSession(ScmOmSession session) {
        Lock wLock = rwLock.writeLock();
        wLock.lock();
        try {
            sessions.put(session.getSessionId(), session);
        }
        finally {
            wLock.unlock();
        }
    }

    public void deleteSession(ScmOmSession session) {
        Lock wLock = rwLock.writeLock();
        wLock.lock();
        try {
            sessions.remove(session.getSessionId());
        }
        finally {
            wLock.unlock();
        }
        session.close();
    }

    private void cleanExpireSession() {
        HashSet<String> sessionIds = null;
        Lock rLock = rwLock.readLock();
        rLock.lock();
        try {
            sessionIds = new HashSet<>(sessions.keySet());
        }
        finally {
            rLock.unlock();
        }

        long now = System.currentTimeMillis();
        for (String sessionId : sessionIds) {
            ScmOmSession session = _getSession(sessionId);
            if (session != null && now - session.getLastAccessTime() > sessionMaxInactiveInterval) {
                logger.info("close expired session:{},now={}, lastAccessTime={}", sessionId,
                        new Date(now), new Date(session.getLastAccessTime()));
                deleteSession(session);
            }
        }
    }

    @PreDestroy
    public void destroy() {
        timer.cancel();
        Lock wLock = rwLock.writeLock();
        wLock.lock();
        try {
            for (ScmOmSession s : sessions.values()) {
                s.close();
            }
        }
        finally {
            sessions = null;
            wLock.unlock();
        }
    }
}
