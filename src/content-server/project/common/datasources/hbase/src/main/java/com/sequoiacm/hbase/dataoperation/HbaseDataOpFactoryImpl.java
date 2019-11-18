package com.sequoiacm.hbase.dataoperation;

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
import com.sequoiacm.datasource.metadata.hbase.HbaseDataLocation;
import com.sequoiacm.hbase.HbaseException;

public class HbaseDataOpFactoryImpl implements ScmDataOpFactory {
    private static final Logger logger = LoggerFactory.getLogger(HbaseDataOpFactoryImpl.class);

    @Override
    public ScmDataWriter createWriter(int siteId, String wsName, ScmLocation location,
            ScmService service, ScmDataInfo dataInfo) throws HbaseException {
        try {
            HbaseDataLocation dataLocation = (HbaseDataLocation) location;
            return new HbaseDataWriterImpl(dataLocation.getTableName(wsName,
                    dataInfo.getCreateTime()), dataInfo.getId(),
                    service);
        }
        catch (HbaseException e) {
            logger.error("build hbase writer failed:siteId=" + siteId + ",wsName="
                    + wsName + ",fileId=" + dataInfo.getId());
            throw e;
        }
        catch (Exception e) {
            logger.error("build hbase writer failed:siteId=" + siteId + ",wsName="
                    + wsName + ",fileId=" + dataInfo.getId());
            throw new HbaseException(
                    "build hbase deleter failed:siteId=" + siteId + ",wsName=" + wsName
                    + ",fileId=" + dataInfo.getId(), e);
        }
    }

    @Override
    public ScmDataReader createReader(int siteId, String wsName, ScmLocation location,
            ScmService service, ScmDataInfo dataInfo) throws HbaseException {
        try {
            HbaseDataLocation dataLocation = (HbaseDataLocation)location;
            return new HbaseDataReaderImpl(siteId, dataLocation.getTableName(wsName,
                    dataInfo.getCreateTime()), dataInfo.getId(),
                    service);
        }
        catch (HbaseException e) {
            logger.error("build hbase reader failed:siteId=" + siteId + ",wsName="
                    + wsName + ",fileId=" + dataInfo.getId());
            throw e;
        }
        catch (Exception e) {
            logger.error("build hbase reader failed:siteId=" + siteId + ",wsName="
                    + wsName + ",fileId=" + dataInfo.getId());
            throw new HbaseException(
                    "build hbase deleter failed:siteId=" + siteId + ",wsName=" + wsName
                    + ",fileId=" + dataInfo.getId(), e);
        }
    }

    @Override
    public ScmDataDeletor createDeletor(int siteId, String wsName, ScmLocation location,
            ScmService service, ScmDataInfo dataInfo) throws HbaseException {
        try {
            HbaseDataLocation dataLocation = (HbaseDataLocation) location;
            return new HbaseDataDeleterImpl(siteId, dataInfo.getId(), dataLocation.getTableName(
                    wsName, dataInfo.getCreateTime()), service);
        }
        catch (HbaseException e) {
            logger.error("build hbase deleter failed:siteId=" + siteId + ",wsName="
                    + wsName + ",fileId=" + dataInfo.getId());
            throw e;
        }
        catch (Exception e) {
            logger.error("build hbase deleter failed:siteId=" + siteId + ",wsName="
                    + wsName + ",fileId=" + dataInfo.getId());
            throw new HbaseException(
                    "build hbase deleter failed:siteId=" + siteId + ",wsName=" + wsName
                    + ",fileId=" + dataInfo.getId(), e);
        }
    }

    @Override
    public ScmBreakpointDataWriter createBreakpointWriter(ScmLocation location, ScmService service, String wsName, String fileName, String dataId, Date createTime, boolean createData) throws HbaseException {
        throw new HbaseException(HbaseException.HBASE_ERROR_OPERATION_UNSUPPORTED,
                "do not support breakpoint upload");
    }

    @Override
    public ScmDataTableDeletor createDataTableDeletor(List<String> tableNames, ScmService service)
            throws ScmDatasourceException {
        return new HbaseDataTableDeletor(tableNames, service);
    }
}
