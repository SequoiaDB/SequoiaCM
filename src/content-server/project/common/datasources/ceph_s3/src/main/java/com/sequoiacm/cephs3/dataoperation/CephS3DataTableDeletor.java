package com.sequoiacm.cephs3.dataoperation;

import com.sequoiacm.cephs3.CephS3Exception;
import com.sequoiacm.cephs3.dataservice.CephS3ConnWrapper;
import com.sequoiacm.cephs3.dataservice.CephS3DataService;
import com.sequoiacm.common.CephS3UserInfo;
import com.sequoiacm.datasource.dataservice.ScmService;
import com.sequoiacm.datasource.metadata.cephs3.CephS3DataLocation;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.sequoiacm.datasource.ScmDatasourceException;
import com.sequoiacm.datasource.dataoperation.ScmDataTableDeletor;

import java.util.List;

public class CephS3DataTableDeletor implements ScmDataTableDeletor {
    private static final Logger logger = LoggerFactory.getLogger(CephS3DataTableDeletor.class);

    private CephS3DataService dataService;

    private CephS3UserInfo primaryUserInfo;

    private CephS3UserInfo standbyUserInfo;

    private List<String> tableNames;

    private String wsName;

    private int siteId;

    private final CephS3BucketManager bucketManager;

    public CephS3DataTableDeletor(CephS3DataLocation cephS3DataLocation, ScmService service,
                                  List<String> tableNames, String wsName) {
        this.wsName = wsName;
        this.dataService = (CephS3DataService) service;
        this.tableNames = tableNames;
        this.bucketManager = CephS3BucketManager.getInstance();
        this.siteId = cephS3DataLocation.getSiteId();
        this.primaryUserInfo = cephS3DataLocation.getPrimaryUserInfo();
        this.standbyUserInfo = cephS3DataLocation.getStandbyUserInfo();
    }


    @Override
    public void delete() throws ScmDatasourceException {
        CephS3ConnWrapper conn = dataService.getConn(primaryUserInfo, standbyUserInfo);
        if (conn == null) {
            throw new CephS3Exception("delete dataTable failed:siteId= " + siteId + ",wsName= " + wsName);
        }
        try {
            // 删除表中记录和缓存中记录
            bucketManager.deleteActiveBucketMapping(wsName);
            for (String bucketName : tableNames) {
                try {
                    conn.clearAndDeleteBucket(bucketName);
                }
                catch (CephS3Exception e) {
                    if (!e.getS3ErrorCode().equals(CephS3Exception.ERR_CODE_NO_SUCH_BUCKET)) {
                        logger.warn("delete bucket failed:bucketName={}", bucketName, e);
                    }
                }
            }
        }
        catch (Exception e) {
            throw new ScmDatasourceException("delete bucket failed:buckets=" + tableNames, e);
        }
        finally {
            dataService.releaseConn(conn);
        }
    }

}
