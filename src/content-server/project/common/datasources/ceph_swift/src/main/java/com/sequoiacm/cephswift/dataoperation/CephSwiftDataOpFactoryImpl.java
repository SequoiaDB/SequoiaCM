package com.sequoiacm.cephswift.dataoperation;

import com.sequoiacm.cephswift.CephSwiftException;
import com.sequoiacm.datasource.ScmDatasourceException;
import com.sequoiacm.datasource.dataoperation.*;
import com.sequoiacm.datasource.dataservice.ScmService;
import com.sequoiacm.datasource.metadata.ScmLocation;
import com.sequoiacm.datasource.metadata.cephswift.CephSwiftDataLocation;
import org.bson.BSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Date;
import java.util.List;

public class CephSwiftDataOpFactoryImpl implements ScmDataOpFactory {

    private static final Logger logger = LoggerFactory.getLogger(CephSwiftDataOpFactoryImpl.class);

    @Override
    public ScmDataWriter createWriter(int siteId, String wsName, ScmLocation location,
            ScmService service, ScmDataInfo dataInfo) throws CephSwiftException {
        try {
            CephSwiftDataLocation dataLocation = (CephSwiftDataLocation) location;
            return new CephSwiftDataWriterImpl(
                    dataLocation.getContainerName(wsName, dataInfo.getCreateTime()),
                    dataInfo.getId(), service);
        }
        catch (CephSwiftException e) {
            logger.error("build ceph swift writer failed:siteId=" + siteId + ",wsName=" + wsName
                    + ",fileId=" + dataInfo.getId());
            throw e;
        }
        catch (Exception e) {
            logger.error("build ceph swift writer failed:siteId=" + siteId + ",wsName=" + wsName
                    + ",fileId=" + dataInfo.getId());
            throw new CephSwiftException("build ceph swift writer failed:siteId=" + siteId
                    + ",wsName=" + wsName + ",fileId=" + dataInfo.getId(), e);
        }
    }

    @Override
    public ScmDataReader createReader(int siteId, String wsName, ScmLocation location,
            ScmService service, ScmDataInfo dataInfo) throws CephSwiftException {
        try {
            CephSwiftDataLocation dataLocation = (CephSwiftDataLocation) location;
            return new CephSwiftDataReaderImpl(
                    dataLocation.getContainerName(wsName, dataInfo.getCreateTime()),
                    dataInfo.getId(), service);
        }
        catch (CephSwiftException e) {
            logger.error("build ceph swift reader failed:siteId=" + siteId + ",wsName=" + wsName
                    + ",fileId=" + dataInfo.getId());
            throw e;
        }
        catch (Exception e) {
            logger.error("build ceph swift reader failed:siteId=" + siteId + ",wsName=" + wsName
                    + ",fileId=" + dataInfo.getId());
            throw new CephSwiftException("build ceph swift reader failed:siteId=" + siteId
                    + ",wsName=" + wsName + ",fileId=" + dataInfo.getId(), e);
        }
    }

    @Override
    public ScmDataDeletor createDeletor(int siteId, String wsName, ScmLocation location,
            ScmService service, ScmDataInfo dataInfo) throws CephSwiftException {
        try {
            CephSwiftDataLocation dataLocation = (CephSwiftDataLocation) location;
            return new CephSwiftDataDeletorImpl(
                    dataLocation.getContainerName(wsName, dataInfo.getCreateTime()),
                    dataInfo.getId(), service);
        }
        catch (CephSwiftException e) {
            logger.error("build ceph swift deletor failed:siteId=" + siteId + ",wsName=" + wsName
                    + ",fileId=" + dataInfo.getId());
            throw e;
        }
        catch (Exception e) {
            logger.error("build ceph swift deletor failed:siteId=" + siteId + ",wsName=" + wsName
                    + ",fileId=" + dataInfo.getId());
            throw new CephSwiftException("build ceph swift deletor failed:siteId=" + siteId
                    + ",wsName=" + wsName + ",fileId=" + dataInfo.getId(), e);
        }
    }

    @Override
    public ScmBreakpointDataWriter createBreakpointWriter(ScmLocation location, ScmService service,
            String wsName, String fileName, String dataId, Date createTime, boolean createData,
            long writeOffset, BSONObject extraContext) throws CephSwiftException {
        throw new CephSwiftException(CephSwiftException.ERR_OPERATION_UNSUPPORTED,
                "do not support breakpoint upload");
    }

    @Override
    public ScmSeekableDataWriter createSeekableDataWriter(ScmLocation location, ScmService service,
            String wsName, String fileName, String dataId, Date createTime, boolean createData,
            long writeOffset, BSONObject extraContext) throws ScmDatasourceException {
        throw new CephSwiftException(CephSwiftException.ERR_OPERATION_UNSUPPORTED,
                "do not support seekable upload");
    }

    @Override
    public ScmDataTableDeletor createDataTableDeletor(List<String> tableNames, ScmService service)
            throws ScmDatasourceException {
        return new CephSwiftDataTableDeletorImpl();
    }
}
