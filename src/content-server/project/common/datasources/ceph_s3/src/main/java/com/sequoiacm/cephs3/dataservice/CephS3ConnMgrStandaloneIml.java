package com.sequoiacm.cephs3.dataservice;

import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.sequoiacm.cephs3.CephS3Exception;
import com.sequoiacm.common.CephS3UrlInfo;
import com.sequoiacm.common.CephS3UserInfo;

/**
 * CephS3 Connection Single ManagerImpl class
 */
public class CephS3ConnMgrStandaloneIml implements CephS3ConnectionManager {
    private static final Logger logger = LoggerFactory
            .getLogger(CephS3ConnMgrStandaloneIml.class);

    private CephS3ConnectionGroup group;
    private CephS3HealthDetector detector;
    private CephS3ConnType type;

    public CephS3ConnMgrStandaloneIml(CephS3UrlInfo urlInfo, CephS3ConnType type, int siteId,
            Map<String, String> cephS3Conf) throws CephS3Exception {
        this.type = type;
        CephS3ConnectionConf connectionConf = new CephS3ConnectionConf(cephS3Conf);
        logger.info("cephs3 client conf:{}", connectionConf);
        try {
            detector = new CephS3HealthDetector(cephS3Conf);
            group = new CephS3ConnectionGroup(urlInfo, type, siteId, connectionConf, detector);
            if (group.getStatus() == ConnStatus.down) {
                throw new CephS3Exception(
                        "failed to connect ceph s3:siteId=" + siteId + "url=" + urlInfo.getUrl());
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
        if (type == CephS3ConnType.primary) {
            return group.getConnection(primaryInfo);
        }
        if (type == CephS3ConnType.standby) {
            return group.getConnection(standbyInfo);
        }
        throw new CephS3Exception("unknown connection type: " + type);
    }

    @Override
    public void release(CephS3ConnWrapper conn) {
        group.releaseConn(conn);
    }

    @Override
    public CephS3ConnWrapper releaseAndTryGetAnotherConn(CephS3ConnWrapper conn,
            CephS3UserInfo primaryInfo, CephS3UserInfo standbyInfo) throws CephS3Exception {
        group.releaseConn(conn);
        if (conn.hasFatalError() || conn.hasSignatureError()) {
            return getConn(primaryInfo, standbyInfo);
        }
        logger.warn(
                "refuse to try get another connection, cause by the released connection is normal");
        return null;
    }

    @Override
    public void shutdown() {
        if (group != null) {
            group.shutdown();
        }
        if (detector != null) {
            detector.shutdown();
        }
    }
}
