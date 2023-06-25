package com.sequoiacm.sequoiadb.dataopertion;

import java.util.Date;
import java.util.List;
import java.util.Map;

import com.sequoiacm.infrastructure.common.ScmIdParser;
import org.bson.BSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.sequoiacm.datasource.ScmDatasourceException;
import com.sequoiacm.datasource.common.ScmDataWriterContext;
import com.sequoiacm.datasource.dataoperation.ScmBreakpointDataWriter;
import com.sequoiacm.datasource.dataoperation.ScmDataDeletor;
import com.sequoiacm.datasource.dataoperation.ScmDataInfo;
import com.sequoiacm.datasource.dataoperation.ScmDataOpFactory;
import com.sequoiacm.datasource.dataoperation.ScmDataReader;
import com.sequoiacm.datasource.dataoperation.ScmDataRemovingSpaceRecycler;
import com.sequoiacm.datasource.dataoperation.ScmDataSpaceRecycler;
import com.sequoiacm.datasource.dataoperation.ScmDataTableDeletor;
import com.sequoiacm.datasource.dataoperation.ScmDataWriter;
import com.sequoiacm.datasource.dataoperation.ScmSeekableDataWriter;
import com.sequoiacm.datasource.dataservice.ScmService;
import com.sequoiacm.datasource.metadata.ScmLocation;
import com.sequoiacm.datasource.metadata.sequoiadb.SdbDataLocation;
import com.sequoiacm.infrastructure.lock.ScmLockManager;
import com.sequoiacm.metasource.MetaSource;
import com.sequoiacm.sequoiadb.SequoiadbException;
import com.sequoiacm.sequoiadb.dataservice.SdbDataService;

public class SdbDataOpFactoryImpl implements ScmDataOpFactory {
    private static final Logger logger = LoggerFactory.getLogger(SdbDataOpFactoryImpl.class);

    private MetaSource metaSource;
    private ScmLockManager lockManager;

    // TODO:plugin
    @Override
    public ScmDataWriter createWriter(int siteId, String wsName, ScmLocation location,
            ScmService service, ScmDataInfo dataInfo) throws SequoiadbException {
        try {
            SdbDataLocation sdbLocation = (SdbDataLocation) location;
            String timezone = ScmIdParser.getTimezoneName(dataInfo.getId());
            String csName = sdbLocation.getDataCsName(wsName, dataInfo.getCreateTime(), timezone);
            String clName = sdbLocation.getDataClName(dataInfo.getCreateTime(), timezone);
            return new SdbDataWriterImpl(siteId, location, service, metaSource, csName, clName,
                    wsName, dataInfo.getType(), dataInfo.getId(), lockManager);
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
            String timezone = ScmIdParser.getTimezoneName(dataInfo.getId());
            String csName = sdbLocation.getDataCsName(wsName, dataInfo.getCreateTime(), timezone);
            String clName = sdbLocation.getDataClName(dataInfo.getCreateTime(), timezone);
            return new SdbDataReaderImpl(siteId, location.getSiteName(), csName, clName, wsName,
                    dataInfo.getType(), dataInfo.getId(), service, metaSource, lockManager);
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
            String timezone = ScmIdParser.getTimezoneName(dataInfo.getId());
            String csName = sdbLocation.getDataCsName(wsName, dataInfo.getCreateTime(), timezone);
            String clName = sdbLocation.getDataClName(dataInfo.getCreateTime(), timezone);
            return new SdbDataDeletorImpl(siteId, location.getSiteName(), csName, clName, wsName,
                    dataInfo.getId(), service, metaSource, lockManager);
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
            long writeOffset, BSONObject extraContext, ScmDataWriterContext writerContext)
            throws SequoiadbException {
        try {
            SdbDataLocation sdbLocation = (SdbDataLocation) location;
            String timezone = ScmIdParser.getTimezoneName(dataId);
            String csName = sdbLocation.getDataCsName(wsName, createTime, timezone);
            String clName = sdbLocation.getDataClName(createTime, timezone);
            return new SdbBreakpointDataWriter(sdbLocation, (SdbDataService) service, metaSource,
                    csName, clName, wsName, dataId, createData, writeOffset, lockManager);
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
            String wsName, String fileName, ScmDataInfo dataInfo, boolean createData,
            long writeOffset, BSONObject extraContext) throws ScmDatasourceException {
        try {
            SdbDataLocation sdbLocation = (SdbDataLocation) location;
            String timezone = ScmIdParser.getTimezoneName(dataInfo.getId());
            String csName = sdbLocation.getDataCsName(wsName, dataInfo.getCreateTime(), timezone);
            String clName = sdbLocation.getDataClName(dataInfo.getCreateTime(), timezone);
            return new SdbSeekableDataWriter(sdbLocation, (SdbDataService) service, metaSource,
                    csName, clName, wsName, dataInfo.getId(), createData, writeOffset, lockManager);
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

    @Override
    public ScmDataRemovingSpaceRecycler createDataRemovingSpaceRecycler(String wsName,
            String siteName, Map<Integer, ScmLocation> locations, ScmService service) throws SequoiadbException {
        try {
            return new SdbDataRemovingSpaceRecyclerImpl(metaSource, wsName, siteName,
                    locations, (SdbDataService) service, lockManager);
        }
        catch (Exception e) {
            String msg = String.format(
                    "build sdb DataRemovingSpaceRecycler failed:siteName=%s, wsName=%s", siteName,
                    wsName);
            logger.error(msg);
            throw new SequoiadbException(msg, e);

        }
    }

    @Override
    public ScmDataSpaceRecycler createScmDataSpaceRecycler(List<String> tableNames,
                                                           Date recycleBeginningTime, Date recycleEndingTime, String wsName, String siteName,
                                                           ScmService service) throws ScmDatasourceException {
        try {
            return new SdbDataSpaceRecyclerImpl(metaSource, tableNames, recycleBeginningTime,
                    recycleEndingTime, wsName, siteName,
                    (SdbDataService) service, lockManager);
        }
        catch (Exception e) {
            String msg = String.format(
                    "build sdb SdbDataSpaceRecycler failed:siteName=%s, wsName=%s, tableNames=%s",
                    siteName, wsName, tableNames);
            logger.error(msg);
            throw new SequoiadbException(msg, e);

        }
    }

    @Override
    public void init(int siteId, MetaSource metaSource, ScmLockManager lockManager) {
        this.metaSource = metaSource;
        this.lockManager = lockManager;
    }
}
