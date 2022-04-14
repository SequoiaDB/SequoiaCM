package com.sequoiacm.sequoiadb.dataopertion;

import com.sequoiacm.datasource.ScmDatasourceException;
import com.sequoiacm.datasource.dataoperation.*;
import com.sequoiacm.datasource.dataservice.ScmService;
import com.sequoiacm.datasource.metadata.ScmLocation;
import com.sequoiacm.datasource.metadata.sequoiadb.SdbDataLocation;
import com.sequoiacm.sequoiadb.SequoiadbException;
import com.sequoiacm.sequoiadb.dataservice.SdbDataService;
import org.bson.BSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Date;
import java.util.List;

public class SdbDataOpFactoryImpl implements ScmDataOpFactory {
    private static final Logger logger = LoggerFactory.getLogger(SdbDataOpFactoryImpl.class);

    // TODO:plugin
    @Override
    public ScmDataWriter createWriter(int siteId, String wsName, ScmLocation location,
            ScmService service, ScmDataInfo dataInfo) throws SequoiadbException {
        try {
            SdbDataLocation sdbLocation = (SdbDataLocation) location;
            String csName = sdbLocation.getDataCsName(wsName, dataInfo.getCreateTime());
            String clName = sdbLocation.getDataClName(dataInfo.getCreateTime());
            return new SdbDataWriterImpl(siteId, location, service, csName, clName,
                    dataInfo.getType(), dataInfo.getId());
        }
        catch (SequoiadbException e) {
            logger.error("build sdb writer failed:siteId=" + siteId + ",wsName=" + wsName
                    + ",fileId=" + dataInfo.getId());
            throw e;
        }
        catch (Exception e) {
            logger.error("build sdb writer failed:siteId=" + siteId + ",wsName=" + wsName
                    + ",fileId=" + dataInfo.getId());
            throw new SequoiadbException("build sdb writer failed:siteId=" + siteId + ",wsName="
                    + wsName + ",fileId=" + dataInfo.getId(), e);
        }
    }

    @Override
    public ScmDataReader createReader(int siteId, String wsName, ScmLocation location,
            ScmService service, ScmDataInfo dataInfo) throws SequoiadbException {
        try {
            SdbDataLocation sdbLocation = (SdbDataLocation) location;
            String csName = sdbLocation.getDataCsName(wsName, dataInfo.getCreateTime());
            String clName = sdbLocation.getDataClName(dataInfo.getCreateTime());
            return new SdbDataReaderImpl(siteId, csName, clName, dataInfo.getType(),
                    dataInfo.getId(), service);
        }
        catch (SequoiadbException e) {
            logger.error("build sdb reader failed:siteId=" + siteId + ",wsName=" + wsName
                    + ",fileId=" + dataInfo.getId());
            throw e;
        }
        catch (Exception e) {
            logger.error("build sdb reader failed:siteId=" + siteId + ",wsName=" + wsName
                    + ",fileId=" + dataInfo.getId());
            throw new SequoiadbException("build sdb reader failed:siteId=" + siteId + ",wsName="
                    + wsName + ",fileId=" + dataInfo.getId(), e);
        }
    }

    @Override
    public ScmDataDeletor createDeletor(int siteId, String wsName, ScmLocation location,
            ScmService service, ScmDataInfo dataInfo) throws SequoiadbException {
        try {
            SdbDataLocation sdbLocation = (SdbDataLocation) location;
            String csName = sdbLocation.getDataCsName(wsName, dataInfo.getCreateTime());
            String clName = sdbLocation.getDataClName(dataInfo.getCreateTime());
            return new SdbDataDeletorImpl(siteId, csName, clName, dataInfo.getId(), service);
        }
        catch (SequoiadbException e) {
            logger.error("build sdb deleter failed:siteId=" + siteId + ",wsName=" + wsName
                    + ",fileId=" + dataInfo.getId());
            throw e;
        }
        catch (Exception e) {
            logger.error("build sdb deleter failed:siteId=" + siteId + ",wsName=" + wsName
                    + ",fileId=" + dataInfo.getId());
            throw new SequoiadbException("build sdb deleter failed:siteId=" + siteId + ",wsName="
                    + wsName + ",fileId=" + dataInfo.getId(), e);
        }
    }

    @Override
    public ScmBreakpointDataWriter createBreakpointWriter(ScmLocation location, ScmService service,
            String wsName, String fileName, String dataId, Date createTime, boolean createData,
            long writeOffset, BSONObject extraContext) throws SequoiadbException {
        try {
            SdbDataLocation sdbLocation = (SdbDataLocation) location;
            String csName = sdbLocation.getDataCsName(wsName, createTime);
            String clName = sdbLocation.getDataClName(createTime);
            return new SdbBreakpointDataWriter(sdbLocation, (SdbDataService) service, csName,
                    clName, dataId, createData, writeOffset);
        }
        catch (SequoiadbException e) {
            logger.error("build sdb breakpoint writer failed:siteId={}, wsName={}, fileName={}",
                    location.getSiteId(), wsName, fileName);
            throw e;
        }
        catch (Exception e) {
            String msg = String.format(
                    "build sdb breakpoint writer failed:siteId=%d, wsName=%s, fileName=%s",
                    location.getSiteId(), wsName, fileName);
            logger.error(msg);
            throw new SequoiadbException(msg, e);
        }
    }

    @Override
    public ScmSeekableDataWriter createSeekableDataWriter(ScmLocation location, ScmService service,
            String wsName, String fileName, String dataId, Date createTime, boolean createData,
            long writeOffset, BSONObject extraContext) throws ScmDatasourceException {
        try {
            SdbDataLocation sdbLocation = (SdbDataLocation) location;
            String csName = sdbLocation.getDataCsName(wsName, createTime);
            String clName = sdbLocation.getDataClName(createTime);
            return new SdbSeekableDataWriter(sdbLocation, (SdbDataService) service, csName, clName,
                    dataId, createData, writeOffset);
        }
        catch (SequoiadbException e) {
            logger.error("build sdb seekable writer failed:siteId={}, wsName={}, fileName={}",
                    location.getSiteId(), wsName, fileName);
            throw e;
        }
        catch (Exception e) {
            String msg = String.format(
                    "build sdb seekable writer failed:siteId=%d, wsName=%s, fileName=%s",
                    location.getSiteId(), wsName, fileName);
            logger.error(msg);
            throw new SequoiadbException(msg, e);
        }
    }

    @Override
    public ScmDataTableDeletor createDataTableDeletor(List<String> tableNames, ScmService service)
            throws ScmDatasourceException {
        return new SdbTableDeletorImpl(tableNames, service);
    }
}
