package com.sequoiacm.cephs3.dataservice;

import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.sequoiacm.cephs3.CephS3Exception;
import com.sequoiacm.common.CephS3UrlInfo;
import com.sequoiacm.common.CephS3UserInfo;

public class CephS3ConnMgrFallbackImpl implements CephS3ConnectionManager {

    private static final Logger logger = LoggerFactory
            .getLogger(CephS3ConnMgrFallbackImpl.class);
    private final CephS3HealthDetector detector;

    private CephS3ConnectionGroup primaryGroup;

    private CephS3ConnectionGroup standbyGroup;

    public CephS3ConnMgrFallbackImpl(CephS3UrlInfo primaryUrlInfo, CephS3UrlInfo standbyUrlInfo,
            int siteId, Map<String, String> cephS3Conf)
            throws CephS3Exception {
        CephS3ConnectionConf connectionConf = new CephS3ConnectionConf(cephS3Conf);
        logger.info("cephs3 client conf:{}", connectionConf);

        try {
            detector = new CephS3HealthDetector(cephS3Conf);
            primaryGroup = new CephS3ConnectionGroup(primaryUrlInfo, CephS3ConnType.primary, siteId,
                    connectionConf, detector);
            standbyGroup = new CephS3ConnectionGroup(standbyUrlInfo, CephS3ConnType.standby, siteId,
                    connectionConf, detector);
            if (primaryGroup.getStatus() == ConnStatus.down
                    && standbyGroup.getStatus() == ConnStatus.down) {
                throw new CephS3Exception("failed to connect ceph s3:siteId=" + siteId + ", url= "
                        + primaryUrlInfo.getUrl() + " , " + standbyUrlInfo.getUrl());
            }
        }
        catch (Exception e) {
            shutdown();
            throw e;
        }
    }

    @Override
    public CephS3ConnWrapper getConn(CephS3UserInfo primaryInfo, CephS3UserInfo standbyInfo)
            throws CephS3Exception {
        CephS3ConnWrapper primaryConnection = primaryGroup.getConnection(primaryInfo);
        if (primaryConnection != null) {
            return primaryConnection;
        }
        CephS3ConnWrapper standbyConnection = standbyGroup.getConnection(standbyInfo);
        if (standbyConnection != null) {
            return standbyConnection;
        }

        return null;
    }

    @Override
    public void release(CephS3ConnWrapper conn) {
        if (conn == null) {
            return;
        }
        if (conn.getConnType() == CephS3ConnType.primary) {
            primaryGroup.releaseConn(conn);
        }
        if (conn.getConnType() == CephS3ConnType.standby) {
            standbyGroup.releaseConn(conn);
        }
    }

    @Override
    public CephS3ConnWrapper releaseAndTryGetAnotherConn(CephS3ConnWrapper conn,
            CephS3UserInfo primaryInfo, CephS3UserInfo standbyInfo) throws CephS3Exception {
        release(conn);
        if (conn.hasFatalError() || conn.hasSignatureError()) {
            return getConn(primaryInfo, standbyInfo);
        }
        logger.warn(
                "refuse to try get another connection, cause by the released connection is normal");
        return null;
    }

    @Override
    public void shutdown() {
        if (primaryGroup != null) {
            primaryGroup.shutdown();
        }
        if (standbyGroup != null) {
            standbyGroup.shutdown();
        }
        if (detector != null) {
            detector.shutdown();
        }
    }
}
