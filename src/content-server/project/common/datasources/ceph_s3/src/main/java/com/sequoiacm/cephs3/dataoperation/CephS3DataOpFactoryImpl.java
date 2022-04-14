package com.sequoiacm.cephs3.dataoperation;

import com.sequoiacm.cephs3.CephS3Exception;
import com.sequoiacm.cephs3.dataservice.CephS3BreakpointFileContext;
import com.sequoiacm.datasource.ScmDatasourceException;
import com.sequoiacm.datasource.dataoperation.*;
import com.sequoiacm.datasource.dataservice.ScmService;
import com.sequoiacm.datasource.metadata.ScmLocation;
import com.sequoiacm.datasource.metadata.cephs3.CephS3DataLocation;
import com.sequoiacm.exception.ScmError;
import org.bson.BSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Date;
import java.util.List;

public class CephS3DataOpFactoryImpl implements ScmDataOpFactory {
    private static final Logger logger = LoggerFactory.getLogger(CephS3DataOpFactoryImpl.class);

    @Override
    public ScmDataWriter createWriter(int siteId, String wsName, ScmLocation location,
            ScmService service, ScmDataInfo dataInfo) throws CephS3Exception {
        CephS3DataLocation dataLocation = (CephS3DataLocation) location;
        String bucketName = dataLocation.getBucketName(wsName, dataInfo.getCreateTime());
        try {
            boolean createBucketIfNotExist = dataLocation.getUserBucketName() == null
                    || dataLocation.getUserBucketName().isEmpty();
            return new CephS3DataWriterImpl(bucketName,
                    dataLocation.getObjectId(dataInfo.getId(), wsName, dataInfo.getCreateTime()),
                    service, createBucketIfNotExist);
        }
        catch (CephS3Exception e) {
            logger.error("build ceph s3 writer failed:siteId=" + siteId + ",wsName=" + wsName
                    + ",fileId=" + dataInfo.getId());
            if (e.getS3ErrorCode().equals(CephS3Exception.ERR_CODE_NO_SUCH_BUCKET)) {
                throw new CephS3Exception(e.getS3StatusCode(), e.getS3ErrorCode(),
                        "bucket is not exist:" + bucketName, ScmError.STORE_SPACE_IS_NOT_EXIST, e);
            }
            throw e;
        }
        catch (Exception e) {
            logger.error("build ceph s3 writer failed:siteId=" + siteId + ",wsName=" + wsName
                    + ",fileId=" + dataInfo.getId());
            throw new CephS3Exception("build ceph s3 writer failed:siteId=" + siteId + ",wsName="
                    + wsName + ",fileId=" + dataInfo.getId(), e);
        }
    }

    @Override
    public ScmDataReader createReader(int siteId, String wsName, ScmLocation location,
            ScmService service, ScmDataInfo dataInfo) throws CephS3Exception {
        try {
            CephS3DataLocation dataLocation = (CephS3DataLocation) location;
            return new CephS3DataReaderImpl(
                    dataLocation.getBucketName(wsName, dataInfo.getCreateTime()),
                    dataLocation.getObjectId(dataInfo.getId(), wsName, dataInfo.getCreateTime()),
                    service);
        }
        catch (CephS3Exception e) {
            logger.error("build ceph s3 reader failed:siteId=" + siteId + ",wsName=" + wsName
                    + ",fileId=" + dataInfo.getId());
            throw e;
        }
        catch (Exception e) {
            logger.error("build ceph s3 reader failed:siteId=" + siteId + ",wsName=" + wsName
                    + ",fileId=" + dataInfo.getId());
            throw new CephS3Exception("build ceph s3 reader failed:siteId=" + siteId + ",wsName="
                    + wsName + ",fileId=" + dataInfo.getId(), e);
        }
    }

    @Override
    public ScmDataDeletor createDeletor(int siteId, String wsName, ScmLocation location,
            ScmService service, ScmDataInfo dataInfo) throws CephS3Exception {
        try {
            CephS3DataLocation dataLocation = (CephS3DataLocation) location;
            return new CephS3DataDeletorImpl(
                    dataLocation.getBucketName(wsName, dataInfo.getCreateTime()),
                    dataLocation.getObjectId(dataInfo.getId(), wsName, dataInfo.getCreateTime()),
                    service);
        }
        catch (CephS3Exception e) {
            logger.error("build ceph s3 deletor failed:siteId=" + siteId + ",wsName=" + wsName
                    + ",fileId=" + dataInfo.getId());
            throw e;
        }
        catch (Exception e) {
            logger.error("build ceph s3 deletor failed:siteId=" + siteId + ",wsName=" + wsName
                    + ",fileId=" + dataInfo.getId());
            throw new CephS3Exception("build ceph s3 deletor failed:siteId=" + siteId + ",wsName="
                    + wsName + ",fileId=" + dataInfo.getId(), e);
        }
    }

    @Override
    public ScmBreakpointDataWriter createBreakpointWriter(ScmLocation location, ScmService service,
            String wsName, String fileName, String dataId, Date createTime, boolean createData,
            long writeOffset, BSONObject extraContext) throws CephS3Exception {
        CephS3DataLocation dataLocation = (CephS3DataLocation) location;
        boolean createBucketIfNotExist = dataLocation.getUserBucketName() == null
                || dataLocation.getUserBucketName().isEmpty();
        return new CephS3BreakpointDataWriter(dataLocation.getBucketName(wsName, createTime),
                dataLocation.getObjectId(dataId, wsName, createTime),
                new CephS3BreakpointFileContext(extraContext), service, writeOffset,
                createBucketIfNotExist);
    }

    @Override
    public ScmSeekableDataWriter createSeekableDataWriter(ScmLocation location, ScmService service,
            String wsName, String fileName, String dataId, Date createTime, boolean createData,
            long writeOffset, BSONObject extraContext) throws CephS3Exception {
        throw new CephS3Exception(CephS3Exception.ERR_CODE_OPERATION_UNSUPPORTED,
                "do not support seekable upload");
    }

    @Override
    public ScmDataTableDeletor createDataTableDeletor(List<String> tableNames, ScmService service)
            throws ScmDatasourceException {
        return new CephS3DataTableDeletor();
    }
}
