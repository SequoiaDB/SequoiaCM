package com.sequoiacm.contentserver.remote;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.sequoiacm.contentserver.dao.FileCommonOperator;
import com.sequoiacm.contentserver.datasourcemgr.ScmDataOpFactoryAssit;
import com.sequoiacm.contentserver.lock.ScmLockManager;
import com.sequoiacm.contentserver.lock.ScmLockPath;
import com.sequoiacm.contentserver.lock.ScmLockPathFactory;
import com.sequoiacm.contentserver.model.ScmWorkspaceInfo;
import com.sequoiacm.contentserver.site.ScmContentModule;
import com.sequoiacm.datasource.ScmDatasourceException;
import com.sequoiacm.datasource.common.ScmDataWriterContext;
import com.sequoiacm.datasource.dataoperation.ScmDataInfo;
import com.sequoiacm.datasource.dataoperation.ScmDataWriter;
import com.sequoiacm.exception.ScmError;
import com.sequoiacm.exception.ScmServerException;
import com.sequoiacm.infrastructure.lock.ScmLock;

public class ScmRemoteFileReaderWrapper extends ScmFileReader {
    private static final Logger logger = LoggerFactory.getLogger(ScmRemoteFileReaderWrapper.class);
    private ScmFileReader innerReader;
    private ScmLock fileContentLock;
    private ScmLock fileReadLock;

    public ScmRemoteFileReaderWrapper(String sessionId, String userDetail, int localSiteId,
            int remoteSiteId, ScmWorkspaceInfo wsInfo, String fileId, int majorVersion,
            int minorVersion, long size, ScmDataInfo dataInfo, int flag) throws ScmServerException {
        ScmDataWriter localFileWriter = null;
        try {
            ScmLockPath fileContentLockPath = ScmLockPathFactory.createFileContentLockPath(
                    wsInfo.getName(), ScmContentModule.getInstance().getLocalSiteInfo().getName(),
                    dataInfo.getId());
            ScmLockPath fileReadLockPath = ScmLockPathFactory.createFileLockPath(wsInfo.getName(),
                    fileId);
            fileReadLock = ScmLockManager.getInstance().acquiresReadLock(fileReadLockPath);
            fileContentLock = ScmLockManager.getInstance().tryAcquiresLock(fileContentLockPath);
            if (fileContentLock == null) {
                logger.debug(
                        "cache file failed because of try lock failed:ws={},fileId={},version={}.{}",
                        wsInfo.getName(), fileId, majorVersion, minorVersion);
                // get file content writeLock failed, no cache local, release
                // fileReadLock
                fileReadLock.unlock();
                fileReadLock = null;
                innerReader = new ScmRemoteFileReader(sessionId, userDetail, remoteSiteId, wsInfo,
                        fileId, majorVersion, minorVersion, flag);
                return;
            }

            // if data exist in local, 'createLocalFileWriter' will assign
            // innerReader as localFileReader
            ScmDataInfo localDataInfo = ScmDataInfo.forCreateNewData(dataInfo.getType(),
                    dataInfo.getId(), dataInfo.getCreateTime(), wsInfo.getVersion());
            ScmDataWriterContext localWriterContext = new ScmDataWriterContext();
            localFileWriter = createLocalFileWriter(localSiteId, wsInfo, fileId, majorVersion,
                    minorVersion, size, localDataInfo, localWriterContext);
            if (localFileWriter != null) {
                logger.debug(
                        "try lock success, read file from remote and cache local:ws={},fileId={},version={}.{}",
                        wsInfo.getName(), fileId, majorVersion, minorVersion);
                innerReader = new ScmRemoteFileReaderCacheLocal(sessionId, userDetail, localSiteId,
                        remoteSiteId, wsInfo, fileId, majorVersion, minorVersion, localFileWriter,
                        flag, localDataInfo, localWriterContext);
                return;
            }

            // no cache local, just release fileContentWriteLock and
            // fileReadLock
            fileContentLock.unlock();
            fileContentLock = null;
            fileReadLock.unlock();
            fileReadLock = null;

            if (innerReader == null) {
                // 'createLocalFileWriter' dose not assign innerReader as
                // localFileReader,
                // create remote file reader
                innerReader = new ScmRemoteFileReader(sessionId, userDetail, remoteSiteId, wsInfo,
                        fileId, majorVersion, minorVersion, flag);
            }
        }
        catch (Exception e) {
            FileCommonOperator.recordDataTableName(wsInfo.getName(), localFileWriter);
            if (localFileWriter != null) {
                localFileWriter.cancel();
            }
            if (fileContentLock != null) {
                fileContentLock.unlock();
            }
            if (fileReadLock != null) {
                fileReadLock.unlock();
            }
            throw e;
        }
    }

    private ScmDataWriter createLocalFileWriter(int localSiteId, ScmWorkspaceInfo wsInfo,
            String fileId, int majorVersion, int minorVersion, long size, ScmDataInfo dataInfo,
            ScmDataWriterContext context)
            throws ScmServerException {
        // build local file writer
        ScmDataWriter fileWriter = null;
        try {
            fileWriter = ScmDataOpFactoryAssit.getFactory().createWriter(localSiteId,
                    wsInfo.getName(), wsInfo.getDataLocation(dataInfo.getWsVersion()),
                    ScmContentModule.getInstance().getDataService(), dataInfo, context);
        }
        catch (ScmDatasourceException e) {
            ScmError errorCode = e.getScmError(ScmError.DATA_WRITE_ERROR);
            // DATA_IS_IN_USE : Read and write modes open lob file at the same time in SDB
            if (errorCode == ScmError.DATA_EXIST || errorCode == ScmError.DATA_IS_IN_USE) {
                dataInfo.setTableName(context.getTableName());
                if (FileCommonOperator.isDataExist(wsInfo, dataInfo, size)) {
                    // data exist, there are two situations:
                    // 1. local site already in the site list
                    // 2. residue data
                    // we have fileContentLock, just add it again.
                    FileCommonOperator.addSiteInfoToList(wsInfo, fileId, majorVersion, minorVersion,
                            localSiteId, dataInfo.getWsVersion(), context);
                    dataInfo.setTableName(context.getTableName());
                    innerReader = new ScmLocalFileReader(localSiteId, wsInfo, dataInfo);
                    return null;
                }
                else {
                    if (!FileCommonOperator.deleteLocalResidulFile(wsInfo, localSiteId, dataInfo)) {
                        return null;
                    }
                    fileWriter = createLocalWriter(localSiteId, wsInfo, dataInfo);
                }
            }
            else {
                logger.warn("create lob in local site failed:siteId={},wsName={},lobId={}",
                        localSiteId, wsInfo.getName(), dataInfo.getId(), e);
            }
        }
        catch (Exception e) {
            logger.warn("create lob in local site failed:siteId={},wsName={},lobId={}", localSiteId,
                    wsInfo.getName(), dataInfo.getId(), e);
        }
        return fileWriter;
    }

    private ScmDataWriter createLocalWriter(int localSiteId, ScmWorkspaceInfo wsInfo,
            ScmDataInfo dataInfo) {
        ScmDataWriter fileWriter = null;
        ScmDataWriterContext context = new ScmDataWriterContext();
        try {
            fileWriter = ScmDataOpFactoryAssit.getFactory().createWriter(localSiteId,
                    wsInfo.getName(), wsInfo.getDataLocation(dataInfo.getWsVersion()),
                    ScmContentModule.getInstance().getDataService(), dataInfo, context);
        }
        catch (Exception e) {
            logger.warn("create lob in local site failed:siteId={},wsName={},lobId={}", localSiteId,
                    wsInfo.getName(), dataInfo.getId(), e);
        }
        return fileWriter;
    }

    @Override
    public void close() {
        if (innerReader != null) {
            innerReader.close();
        }
        if (fileContentLock != null) {
            fileContentLock.unlock();
        }
        if (fileReadLock != null) {
            fileReadLock.unlock();
        }
    }

    @Override
    public int read(byte[] buff, int offset, int len) throws ScmServerException {
        return innerReader.read(buff, offset, len);
    }

    @Override
    public void seek(long size) throws ScmServerException {
        innerReader.seek(size);
    }

    @Override
    public boolean isEof() {
        return innerReader.isEof();
    }

    @Override
    public long getSize() throws ScmServerException {
        return innerReader.getSize();
    }
}
