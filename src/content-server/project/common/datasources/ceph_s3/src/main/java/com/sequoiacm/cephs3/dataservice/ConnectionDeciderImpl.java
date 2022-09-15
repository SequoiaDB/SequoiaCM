package com.sequoiacm.cephs3.dataservice;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import com.sequoiacm.common.CephS3UrlInfo;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.sequoiacm.cephs3.CephS3Exception;

public class ConnectionDeciderImpl implements ConnectionDecider {
    private static final Logger logger = LoggerFactory.getLogger(ConnectionDeciderImpl.class);

    private final CephS3HealthDetector detector;
    private List<CephS3ConnContext> connectionContexts;
    private final int siteId;
    private CephS3ConnectionConf connectionConf;

    public ConnectionDeciderImpl(int siteId, Map<String, String> cephS3Conf,
            // 按参数顺序排序，靠前的地址具有更高的优先级
            CephS3UrlInfo... urlInfoArray) throws CephS3Exception {
        if (urlInfoArray == null || urlInfoArray.length <= 0) {
            throw new CephS3Exception("url is empty");
        }
        this.siteId = siteId;
        this.connectionConf = new CephS3ConnectionConf(cephS3Conf);
        logger.info("cephs3 client conf:{}", connectionConf);

        this.detector = new CephS3HealthDetector(cephS3Conf);

        this.connectionContexts = new ArrayList<>(urlInfoArray.length);
        for (CephS3UrlInfo urlInfo : urlInfoArray) {
            this.connectionContexts.add(new CephS3ConnContext(urlInfo));
        }

        for (CephS3ConnContext context : connectionContexts) {
            if (initConnection(context)) {
                return;
            }
        }

        // 所有地址都初始化失败了
        detector.shutdown();
        throw new CephS3Exception(
                "failed to connect ceph s3:siteId=" + siteId + ", url=" + connectionContexts);
    }

    private boolean initConnection(CephS3ConnContext connContext) {
        synchronized (connContext) {
            if (connContext.isDown()) {
                return false;
            }
            if (connContext.getConn() != null) {
                return true;
            }
            try {
                connContext.setConn(new CephS3Conn(siteId, connContext.getUrlInfo().getAccesskey(),
                        connContext.getUrlInfo().getSecretkey(), connContext.getUrlInfo().getUrl(),
                        connectionConf));
                return true;
            }
            catch (Exception e) {
                logger.warn("failed to create connection: siteId=" + siteId + ",url="
                        + connContext.getUrlInfo().getUrl(), e);
                connectionDownAndDetect(connContext);
                return false;
            }
        }
    }

    private void connectionDownAndDetect(CephS3ConnContext context) {
        synchronized (context) {
            if (context.isDown()) {
                return;
            }
            detector.addUrlToDetect(context.getUrlInfo(), () -> connectionUp(context));
            context.setDown(true);
        }
    }

    private void connectionUp(CephS3ConnContext context) throws CephS3Exception {
        synchronized (context) {
            if (!context.isDown()) {
                return;
            }
            if (context.getConn() != null) {
                context.getConn().shutdown();
            }
            context.setConn(new CephS3Conn(siteId, context.getUrlInfo().getAccesskey(),
                    context.getUrlInfo().getSecretkey(), context.getUrlInfo().getUrl(),
                    connectionConf));
            context.setDown(false);
        }
    }

    @Override
    public CephS3ConnWrapper getConn() {
        for (int i = 0; i < connectionContexts.size(); i++) {
            CephS3ConnContext context = connectionContexts.get(i);
            if (isConnectionAvailable(context)) {
                return new CephS3ConnWrapper(i, context.getConn());
            }
        }
        return null;
    }

    @Override
    public void release(CephS3ConnWrapper conn) {
        if (conn == null) {
            return;
        }
        if (!conn.hasFatalError()) {
            return;
        }

        CephS3ConnContext context = connectionContexts.get(conn.getId());
        if (!context.isDown()) {
            logger.warn("cephs3 down:url={}", context.getUrlInfo().getUrl());
            connectionDownAndDetect(context);
        }
    }

    private boolean isConnectionAvailable(CephS3ConnContext context) {
        if (context.isDown()) {
            return false;
        }

        if (context.getConn() != null) {
            return true;
        }
        return initConnection(context);
    }

    @Override
    public CephS3ConnWrapper releaseAndTryGetAnotherConn(CephS3ConnWrapper conn) {
        release(conn);

        if (!conn.hasFatalError()) {
            // 句柄对象没有监测到宕机错误，拒绝重试，返回null
            logger.warn(
                    "refuse to try get another connection, cause by the released connection is normal");
            return null;
        }
        return getConn();
    }

    @Override
    public void shutdown() {
        detector.shutdown();
        for (CephS3ConnContext context : connectionContexts) {
            if (context.getConn() != null) {
                context.getConn().shutdown();
            }
        }
    }

}
