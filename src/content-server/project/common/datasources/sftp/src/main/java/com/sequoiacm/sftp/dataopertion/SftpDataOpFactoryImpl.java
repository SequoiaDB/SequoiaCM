package com.sequoiacm.sftp.dataopertion;

import com.sequoiacm.datasource.ScmDatasourceException;
import com.sequoiacm.datasource.dataoperation.*;
import com.sequoiacm.datasource.dataservice.ScmService;
import com.sequoiacm.datasource.metadata.ScmLocation;
import com.sequoiacm.datasource.metadata.sftp.SftpDataLocation;
import com.sequoiacm.sftp.SftpDataException;
import org.bson.BSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Date;
import java.util.List;

public class SftpDataOpFactoryImpl implements ScmDataOpFactory {
    private static final Logger logger = LoggerFactory.getLogger(SftpDataOpFactoryImpl.class);

    @Override
    public ScmDataWriter createWriter(int siteId, String wsName, ScmLocation location,
            ScmService service, ScmDataInfo dataInfo) throws SftpDataException {
        try {
            SftpDataLocation dataLocation = (SftpDataLocation) location;
            return new SftpDataWriterImpl(dataLocation.getFileDir(wsName, dataInfo.getCreateTime()),
                    dataInfo.getId(), service);
        }
        catch (SftpDataException e) {
            logger.error("build sftp writer failed:siteId=" + siteId + ",wsName=" + wsName
                    + ",fileId=" + dataInfo.getId());
            throw e;
        }
        catch (Exception e) {
            logger.error("build sftp writer failed:siteId=" + siteId + ",wsName=" + wsName
                    + ",fileId=" + dataInfo.getId());
            throw new SftpDataException("build sftp writer failed:siteId=" + siteId + ",wsName="
                    + wsName + ",fileId=" + dataInfo.getId(), e);
        }
    }

    @Override
    public ScmDataReader createReader(int siteId, String wsName, ScmLocation location,
            ScmService service, ScmDataInfo dataInfo) throws SftpDataException {
        try {
            SftpDataLocation dataLocation = (SftpDataLocation) location;
            return new SftpDataReaderImpl(dataLocation.getFileDir(wsName, dataInfo.getCreateTime()),
                    dataInfo.getId(), service);
        }
        catch (SftpDataException e) {
            logger.error("build sftp reader failed:siteId=" + siteId + ",wsName=" + wsName
                    + ",fileId=" + dataInfo.getId());
            throw e;
        }
        catch (Exception e) {
            logger.error("build sftp reader failed:siteId=" + siteId + ",wsName=" + wsName
                    + ",fileId=" + dataInfo.getId());
            throw new SftpDataException("build sftp reader failed:siteId=" + siteId + ",wsName="
                    + wsName + ",fileId=" + dataInfo.getId(), e);
        }
    }

    @Override
    public ScmDataDeletor createDeletor(int siteId, String wsName, ScmLocation location,
            ScmService service, ScmDataInfo dataInfo) throws SftpDataException {
        try {
            SftpDataLocation dataLocation = (SftpDataLocation) location;
            return new SftpDataDeletorImpl(
                    dataLocation.getFileDir(wsName, dataInfo.getCreateTime()), dataInfo.getId(),
                    service);
        }
        catch (SftpDataException e) {
            logger.error("build sftp deleter failed:siteId=" + siteId + ",wsName=" + wsName
                    + ",fileId=" + dataInfo.getId());
            throw e;
        }
        catch (Exception e) {
            logger.error("build sftp deleter failed:siteId=" + siteId + ",wsName=" + wsName
                    + ",fileId=" + dataInfo.getId());
            throw new SftpDataException("build sftp deleter failed:siteId=" + siteId + ",wsName="
                    + wsName + ",fileId=" + dataInfo.getId(), e);
        }
    }

    @Override
    public ScmBreakpointDataWriter createBreakpointWriter(ScmLocation location, ScmService service,
            String wsName, String fileName, String dataId, Date createTime, boolean createData,
            long writeOffset, BSONObject extraContext) throws SftpDataException {
        throw new SftpDataException(SftpDataException.SFTP_ERROR_OPERATION_UNSUPPORTED,
                "do not support breakpoint upload");
    }

    @Override
    public ScmSeekableDataWriter createSeekableDataWriter(ScmLocation location, ScmService service,
            String wsName, String fileName, String dataId, Date createTime, boolean createData,
            long writeOffset, BSONObject extraContext) throws ScmDatasourceException {
        throw new SftpDataException(SftpDataException.SFTP_ERROR_OPERATION_UNSUPPORTED,
                "do not support seekable upload");
    }

    @Override
    public ScmDataTableDeletor createDataTableDeletor(List<String> tableNames, ScmService service)
            throws ScmDatasourceException {
        return new SftpTableDeletorImpl(tableNames, service);
    }
}
