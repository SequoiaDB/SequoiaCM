package com.sequoiacm.cephs3.dataoperation;

import java.util.Date;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.sequoiacm.cephs3.CephS3Exception;
import com.sequoiacm.datasource.ScmDatasourceException;
import com.sequoiacm.datasource.dataoperation.ScmBreakpointDataWriter;
import com.sequoiacm.datasource.dataoperation.ScmDataDeletor;
import com.sequoiacm.datasource.dataoperation.ScmDataInfo;
import com.sequoiacm.datasource.dataoperation.ScmDataOpFactory;
import com.sequoiacm.datasource.dataoperation.ScmDataReader;
import com.sequoiacm.datasource.dataoperation.ScmDataTableDeletor;
import com.sequoiacm.datasource.dataoperation.ScmDataWriter;
import com.sequoiacm.datasource.dataservice.ScmService;
import com.sequoiacm.datasource.metadata.ScmLocation;
import com.sequoiacm.datasource.metadata.cephs3.CephS3DataLocation;

public class CephS3DataOpFactoryImpl implements ScmDataOpFactory {
    private static final Logger logger = LoggerFactory.getLogger(CephS3DataOpFactoryImpl.class);

    @Override
    public ScmDataWriter createWriter(int siteId, String wsName, ScmLocation location,
            ScmService service, ScmDataInfo dataInfo) throws CephS3Exception {
        try {
            CephS3DataLocation dataLocation = (CephS3DataLocation) location;

            return new CephS3DataWriterImpl(
                    dataLocation.getBucketName(wsName, dataInfo.getCreateTime()), dataInfo.getId(),
                    service);
        }
        catch (CephS3Exception e) {
            logger.error("build ceph s3 writer failed:siteId=" + siteId + ",wsName=" + wsName
                    + ",fileId=" + dataInfo.getId());
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
                    dataLocation.getBucketName(wsName, dataInfo.getCreateTime()), dataInfo.getId(),
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
                    dataLocation.getBucketName(wsName, dataInfo.getCreateTime()), dataInfo.getId(),
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
            String wsName, String fileName, String dataId, Date createTime, boolean createData)
                    throws CephS3Exception {
        throw new CephS3Exception(CephS3Exception.ERR_CODE_OPERATION_UNSUPPORTED,
                "do not support breakpoint upload");
    }

    @Override
    public ScmDataTableDeletor createDataTableDeletor(List<String> tableNames, ScmService service)
            throws ScmDatasourceException {
        return new CephS3DataTableDeletor();
    }
}
