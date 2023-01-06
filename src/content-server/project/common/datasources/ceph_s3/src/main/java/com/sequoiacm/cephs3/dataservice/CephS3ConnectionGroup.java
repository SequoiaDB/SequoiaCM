package com.sequoiacm.cephs3.dataservice;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.amazonaws.SdkClientException;
import com.amazonaws.services.s3.model.AmazonS3Exception;
import com.sequoiacm.cephs3.CephS3Exception;
import com.sequoiacm.common.CephS3UrlInfo;
import com.sequoiacm.common.CephS3UserInfo;
import com.sequoiacm.infrastructure.common.timer.ScmTimer;
import com.sequoiacm.infrastructure.common.timer.ScmTimerFactory;
import com.sequoiacm.infrastructure.common.timer.ScmTimerTask;
import com.sequoiacm.infrastructure.crypto.AuthInfo;
import com.sequoiacm.infrastructure.crypto.ScmFilePasswordParser;

/**
 * 代表主库或者备库的连接组（不同的用户在这个库上会有一个独立的链接句柄）
 */
public class CephS3ConnectionGroup {
    private static final Logger logger = LoggerFactory.getLogger(CephS3ConnectionGroup.class);
    private final ScmTimer timer;

    private ReadWriteLock lockForKickIdleConnection = new ReentrantReadWriteLock();

    private final ConcurrentHashMap<CephS3UserInfo, CephS3Conn> connMap = new ConcurrentHashMap<>();
    private volatile ConnStatus status;

    private CephS3ConnType connType;

    private CephS3UrlInfo urlInfo;

    private CephS3ConnectionConf connectionConf;

    private CephS3HealthDetector detector;

    private final int siteId;

    public ConnStatus getStatus() {
        return status;
    }

    public CephS3ConnectionGroup(CephS3UrlInfo urlInfo,
            CephS3ConnType connType, int siteId, CephS3ConnectionConf connectionConf,
            CephS3HealthDetector detector) throws CephS3Exception {
        this.urlInfo = urlInfo;
        this.connType = connType;
        this.detector = detector;
        this.siteId = siteId;
        this.connectionConf = connectionConf;
        this.status = ConnStatus.up;
        try {
            createConn(urlInfo.getUserInfo());
            timer = ScmTimerFactory.createScmTimer();
            timer.schedule(new ScmTimerTask() {
                @Override
                public void run() {
                    try {
                        kickIdleConnection();
                    }
                    catch (Exception e) {
                        logger.warn("failed to clean ideal connection", e);
                    }
                }
            }, connectionConf.getIdleClientCleanInterval(),
                    connectionConf.getIdleClientCleanInterval());
        }
        catch (Exception e) {
            shutdown();
            throw e;
        }
    }

    private void kickIdleConnection() {
        Map<CephS3UserInfo, CephS3Conn> kickedConnection = new HashMap<>();
        Lock writeLock = lockForKickIdleConnection.writeLock();
        writeLock.lock();
        try {
            long now = System.currentTimeMillis();
            Iterator<Map.Entry<CephS3UserInfo, CephS3Conn>> it = connMap.entrySet().iterator();
            while (it.hasNext()) {
                Map.Entry<CephS3UserInfo, CephS3Conn> entry = it.next();
                if (entry.getValue().getUsingCount() <= 0
                        && now - entry.getValue().getLastAccessTime() > connectionConf
                                .getIdleClientTimeout()) {
                    it.remove();
                    kickedConnection.put(entry.getKey(), entry.getValue());
                }
            }
        }
        finally {
            writeLock.unlock();
        }
        for (Map.Entry<CephS3UserInfo, CephS3Conn> entry : kickedConnection.entrySet()) {
            logger.info("shutdown idle cephs3 client: user={}, url={}", entry.getKey(),
                    urlInfo.getUrl());
            entry.getValue().shutdownSilence();
        }
    }

    private boolean tryReloadPasswordFile(CephS3UserInfo userInfo) {
        AuthInfo authInfo = ScmFilePasswordParser.parserFile(userInfo.getSecretKeyFilePath());
        String newEncryptedPassword = authInfo.getEncryptedPassword();
        String oldEncryptedPassword = userInfo.getEncryptedSecretKey();
        if (!newEncryptedPassword.equals(oldEncryptedPassword)) {
            userInfo.setEncryptedSecretKey(newEncryptedPassword);
            logger.info("password file reload success: accesskey={}, passwordFile={}",
                    userInfo.getAccessKey(), userInfo.getSecretKeyFilePath());
            return true;
        }
        logger.warn(
                "password file is not change, ignore reload password file: accesskey={}, passwordFile={}",
                userInfo.getAccessKey(), userInfo.getSecretKeyFilePath());
        return false;
    }

    // 没有发生异常返回创建的链接；发生无法连接、响应不识别异常返回 null；发生其它异常直接抛出
    private synchronized CephS3Conn createConn(CephS3UserInfo userInfo) throws CephS3Exception {
        CephS3Conn conn = connMap.get(userInfo);
        if (conn != null && !conn.isShouldBeDiscard()) {
            return conn;
        }

        if (conn != null) {
            // conn.isShouldBeDiscard() == true 表示该连接上次回池的时候被打上标记，需要被淘汰
            connMap.remove(userInfo);
            conn.shutdownSilence();
        }

        try {
            conn = new CephS3Conn(siteId, userInfo.getAccessKey(), userInfo.getSecretKey(),
                    urlInfo.getUrl(), connectionConf);
            connMap.put(userInfo, conn);
            return conn;
        }
        catch (Exception e) {
            if (e instanceof AmazonS3Exception) {
                AmazonS3Exception amzException = (AmazonS3Exception) e;
                if (Objects.equals(amzException.getErrorCode(),
                        CephS3Exception.ERR_CODE_SIGNATURE_DOES_NOT_MATCH)) {
                    logger.warn(
                            "failed to create connection cause by signature error, reload password file and create connection again: url="
                                    + urlInfo.getUrl() + ", accesskey=" + userInfo.getAccessKey()
                                    + ", passwordFile=" + userInfo.getSecretKeyFilePath(),
                            e);
                    boolean isReload = tryReloadPasswordFile(userInfo);
                    if (!isReload) {
                        throw new CephS3Exception(
                                "failed to create connection, signature error:url="
                                        + urlInfo.getUrl() + ", accesskey="
                                        + userInfo.getAccessKey() + ", passwordFile="
                                        + userInfo.getSecretKeyFilePath(),
                                e);
                    }
                    conn = new CephS3Conn(siteId, userInfo.getAccessKey(), userInfo.getSecretKey(),
                            urlInfo.getUrl(), connectionConf);
                    connMap.put(userInfo, conn);
                    return conn;
                }
            }

            // 无法连接，或响应不识别
            if (e.getClass().equals(SdkClientException.class)) {
                logger.warn("failed to create connection: siteId=" + siteId + ",url="
                        + urlInfo.getUrl() + ", accesskey=" + userInfo.getAccessKey(), e);
                connectionDownAndDetect();
                return null;
            }

            throw new CephS3Exception("failed to create connection: siteId=" + siteId + ",url="
                    + urlInfo.getUrl() + ", accesskey=" + userInfo.getAccessKey(), e);
        }
    }

    // 调用者需要在 sync 代码块中
    private void connectionDownAndDetect() {
        if (status == ConnStatus.down) {
            return;
        }
        logger.warn("assume cephs3 is down, start detecting job: url={}", urlInfo.getUrl());
        detector.addUrlToDetect(urlInfo, () -> connectionUp());
        status = ConnStatus.down;
    }

    public CephS3ConnWrapper getConnection(CephS3UserInfo userInfo) throws CephS3Exception {
        Lock readLock = lockForKickIdleConnection.readLock();
        readLock.lock();
        try {
            if (status == ConnStatus.up) {
                if (userInfo == null) {
                    userInfo = urlInfo.getUserInfo();
                }
                CephS3Conn conn = connMap.get(userInfo);
                if (conn == null || conn.isShouldBeDiscard()) {
                    conn = createConn(userInfo);
                    if (conn == null) {
                        return null;
                    }
                }
                conn.incUsingCount();
                return new CephS3ConnWrapper(conn, connType, userInfo);
            }
            return null;
        }
        finally {
            readLock.unlock();
        }
    }

    public void releaseConn(CephS3ConnWrapper connWrapper) {
        if (connWrapper == null) {
            return;
        }
        connWrapper.getConn().decUsingCount();

        if (!connWrapper.hasFatalError() && !connWrapper.hasSignatureError()) {
            return;
        }
        synchronized (this) {
            if (connWrapper.hasSignatureError()) {
                boolean isReload = tryReloadPasswordFile(connWrapper.getUserInfo());
                if (isReload) {
                    connWrapper.getConn().setShouldBeDiscard(true);
                }
                return;
            }
            if (connWrapper.hasFatalError()) {
                connectionDownAndDetect();
            }
        }
    }

    private synchronized void connectionUp() throws Exception {
        if (status == ConnStatus.up) {
            return;
        }

        // 释放之前的所有链接
        clearConnection();

        // 重建一个站点用户的链接
        CephS3Conn connection = createConn(urlInfo.getUserInfo());
        if (connection == null) {
            throw new CephS3Exception(
                    "failed to create connection, cephs3 is down: url=" + urlInfo.getUrl());
        }

        status = ConnStatus.up;
        logger.info("cephs3 is up: url={}", urlInfo.getUrl());
    }

    public void shutdown() {
        if (timer != null) {
            timer.cancel();
        }
        clearConnection();
    }

    private void clearConnection() {
        for (Map.Entry<CephS3UserInfo, CephS3Conn> next : connMap.entrySet()) {
            next.getValue().shutdownSilence();
        }
        connMap.clear();
    }

}

enum ConnStatus {
    up,
    down
}