package com.sequoiacm.cephs3.dataoperation;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.sequoiacm.cephs3.CephS3Exception;
import com.sequoiacm.cephs3.dataservice.CephS3ConnWrapper;
import com.sequoiacm.cephs3.dataservice.CephS3DataService;
import com.sequoiacm.datasource.dataoperation.ScmDataInfoFetcher;
import com.sequoiacm.datasource.dataservice.ScmService;
import com.sequoiacm.datasource.metadata.cephs3.CephS3DataLocation;
import com.sequoiacm.infrastructure.common.annotation.SlowLog;
import com.sequoiacm.infrastructure.common.annotation.SlowLogExtra;

public class CephS3DataInfoFetcher implements ScmDataInfoFetcher {
    private static final Logger logger = LoggerFactory.getLogger(CephS3DataInfoFetcher.class);
    private long size;

    @SlowLog(operation = "openFetcher", extras = {
            @SlowLogExtra(name = "readCephS3BucketName", data = "bucketName"),
            @SlowLogExtra(name = "readCephS3ObjectKey", data = "key") })
    public CephS3DataInfoFetcher(String bucketName, String key, ScmService service, CephS3DataLocation dataLocation)
            throws CephS3Exception {
        CephS3DataService dataService = (CephS3DataService) service;
        CephS3ConnWrapper conn = dataService.getConn(dataLocation.getPrimaryUserInfo(), dataLocation.getStandbyUserInfo());
        if (conn == null) {
            throw new CephS3Exception(
                    "construct CephS3DataInfoFetcher failed, cephs3 is down:bucketName="
                            + bucketName + ",key=" + key);
        }
        try {
            this.size = conn.getObjectMeta(bucketName, key).getContentLength();
        }
        catch (Exception e) {
            conn = dataService.releaseAndTryGetAnotherConn(conn, dataLocation.getPrimaryUserInfo(), dataLocation.getStandbyUserInfo());
            if (conn == null) {
                throw e;
            }
            logger.warn(
                    "get object info failed, get another ceph conn to try again:bucketName={}, key={}, conn={}",
                    bucketName, key, conn.getUrl(), e);
            this.size = conn.getObjectMeta(bucketName, key).getContentLength();
        }
        finally {
            dataService.releaseConn(conn);
        }
    }

    @Override
    public long getDataSize() {
        return size;
    }
}
