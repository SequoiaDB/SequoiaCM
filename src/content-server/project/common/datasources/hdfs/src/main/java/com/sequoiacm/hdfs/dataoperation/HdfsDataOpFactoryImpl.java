package com.sequoiacm.hdfs.dataoperation;

import java.util.Date;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

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
import com.sequoiacm.datasource.metadata.hdfs.HdfsDataLocation;
import com.sequoiacm.hdfs.HdfsException;

public class HdfsDataOpFactoryImpl implements ScmDataOpFactory {
    private static final Logger logger = LoggerFactory.getLogger(HdfsDataOpFactoryImpl.class);

    @Override
    public ScmDataWriter createWriter(int siteId, String wsName, ScmLocation location,
            ScmService service, ScmDataInfo dataInfo) throws HdfsException {
        try {
            HdfsDataLocation dataLocation = (HdfsDataLocation) location;
            return new HdfsDataWriterImpl(wsName, dataLocation, service, dataInfo);
        }
        catch (HdfsException e) {
            logger.error("build hdfs writer failed:siteId=" + siteId + ",wsName=" + wsName
                    + ",fileId=" + dataInfo.getId());
            throw e;
        }
        catch (Exception e) {
            throw new HdfsException(
                    "build hdfs writer failed:siteId=" + siteId + ",wsName=" + wsName + ",fileId="
                            + dataInfo.getId(), e);
        }
    }

    @Override
    public ScmDataReader createReader(int siteId, String wsName, ScmLocation location,
            ScmService service, ScmDataInfo dataInfo) throws HdfsException {
        try {
            HdfsDataLocation dataLocation = (HdfsDataLocation) location;
            return new HdfsDataReaderImpl(wsName, dataLocation, service, dataInfo);
        }
        catch (HdfsException e) {
            logger.error("build hdfs reader failed:siteId=" + siteId + ",wsName=" + wsName
                    + ",fileId=" + dataInfo.getId());
            throw e;
        }
        catch (Exception e) {
            throw new HdfsException(
                    "build hdfs reader failed:siteId=" + siteId + ",wsName=" + wsName + ",fileId="
                            + dataInfo.getId(), e);
        }
    }

    @Override
    public ScmDataDeletor createDeletor(int siteId, String wsName, ScmLocation location,
            ScmService service, ScmDataInfo dataInfo) throws HdfsException {
        try {
            HdfsDataLocation dataLocation = (HdfsDataLocation) location;
            return new HdfsDataDeleterImpl(wsName, dataLocation, service, dataInfo);
        }
        catch (HdfsException e) {
            logger.error("build hdfs deleter failed:siteId=" + siteId + ",wsName=" + wsName
                    + ",fileId=" + dataInfo.getId());
            throw e;
        }
        catch (Exception e) {
            throw new HdfsException(
                    "build hdfs deleter failed:siteId=" + siteId + ",wsName=" + wsName + ",fileId="
                            + dataInfo.getId(), e);
        }
    }

    @Override
    public ScmBreakpointDataWriter createBreakpointWriter(ScmLocation location, ScmService service,
            String wsName, String fileName, String dataId, Date createTime, boolean createData)
                    throws HdfsException {
        throw new HdfsException(HdfsException.HDFS_ERROR_OPERATION_UNSUPPORTED,
                "do not support breakpoint upload");
    }

    @Override
    public ScmDataTableDeletor createDataTableDeletor(List<String> tableNames, ScmService service)
            throws ScmDatasourceException {
        return new HdfsDataTableDeletor(tableNames, service);
    }
}
