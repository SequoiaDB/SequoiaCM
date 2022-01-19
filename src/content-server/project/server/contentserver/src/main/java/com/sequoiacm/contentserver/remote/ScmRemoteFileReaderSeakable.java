package com.sequoiacm.contentserver.remote;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.sequoiacm.contentserver.common.Const;
import com.sequoiacm.contentserver.dao.FileCommonOperator;
import com.sequoiacm.contentserver.datasourcemgr.ScmDataOpFactoryAssit;
import com.sequoiacm.exception.ScmServerException;
import com.sequoiacm.contentserver.lock.ScmLockManager;
import com.sequoiacm.contentserver.lock.ScmLockPath;
import com.sequoiacm.contentserver.lock.ScmLockPathFactory;
import com.sequoiacm.contentserver.model.ScmWorkspaceInfo;
import com.sequoiacm.contentserver.site.ScmContentModule;
import com.sequoiacm.datasource.ScmDatasourceException;
import com.sequoiacm.datasource.dataoperation.ScmDataInfo;
import com.sequoiacm.datasource.dataoperation.ScmDataWriter;
import com.sequoiacm.exception.ScmError;
import com.sequoiacm.infrastructure.lock.ScmLock;

public class ScmRemoteFileReaderSeakable extends ScmFileReader {
    private static final Logger logger = LoggerFactory.getLogger(ScmRemoteFileReaderSeakable.class);
    private ScmFileReader innerReader = null;

    public ScmRemoteFileReaderSeakable(String sessionId, String userDetail, int localSiteId,
            int remoteSiteId, ScmWorkspaceInfo wsInfo, String fileId, int majorVersion,
            int minorVersion, long size, ScmDataInfo dataInfo, int flag) throws ScmServerException {
        ScmLock fileContentLock = null;
        ScmLock fileReadLock = null;
        ScmDataWriter fileWriter = null;
        try {
            ScmLockPath fileContentLockPath = ScmLockPathFactory.createFileContentLockPath(
                    wsInfo.getName(), ScmContentModule.getInstance().getLocalSiteInfo().getName(),
                    dataInfo.getId());
            ScmLockPath fileReadLockPath = ScmLockPathFactory.createFileLockPath(wsInfo.getName(),
                    fileId);
            fileReadLock = ScmLockManager.getInstance().acquiresReadLock(fileReadLockPath);

            // we need cache local for seek, acquire localFileContent lock
            fileContentLock = ScmLockManager.getInstance().acquiresLock(fileContentLockPath);

            // if data exist in local, 'createLocalFileWriter' will assign
            // innerReader as localFileReader
            fileWriter = createLocalFileWriter(localSiteId, wsInfo, fileId, majorVersion,
                    minorVersion, size, dataInfo);

            if (fileWriter != null) {
                logger.debug("read file from remote and cache local:ws={},fileId={},version={}.{}",
                        wsInfo.getName(), fileId, majorVersion, minorVersion);
                cacheLocal(sessionId, userDetail, localSiteId, remoteSiteId, wsInfo, fileId,
                        majorVersion, minorVersion, flag, fileWriter);

                // cache local finish, just release fileContentLock and
                // fileReadLock
                fileContentLock.unlock();
                fileContentLock = null;
                fileReadLock.unlock();
                fileReadLock = null;

                innerReader = new ScmLocalFileReader(localSiteId, wsInfo, dataInfo);
                return;
            }

            // no cache local, just release fileContentLock and fileReadLock
            fileContentLock.unlock();
            fileContentLock = null;
            fileReadLock.unlock();
            fileReadLock = null;

            if (innerReader == null) {
                // 'createLocalFileWriter' dose not assign innerReader as
                // localFileReader, we need localFileReader for seek, throw
                // exception here
                throw new ScmServerException(ScmError.DATA_READ_ERROR,
                        "create seekable reader failed, cache local failed:workspace="
                                + wsInfo.getName() + ",fileId=" + fileId + ",version="
                                + majorVersion + "." + minorVersion);
            }
        }
        catch (Exception e) {
            if (fileWriter != null) {
                fileWriter.cancel();
            }
            throw e;
        }
        finally {
            FileCommonOperator.recordDataTableName(wsInfo.getName(), fileWriter);
            if (fileContentLock != null) {
                fileContentLock.unlock();
            }
            if (fileReadLock != null) {
                fileReadLock.unlock();
            }
        }
    }

    private void cacheLocal(String sessionId, String userDetail, int localSiteId, int remoteSiteId,
            ScmWorkspaceInfo wsInfo, String fileId, int majorVersion, int minorVersion, int flag,
            ScmDataWriter fileWriter) throws ScmServerException {
        ScmRemoteFileReaderCacheLocal preLoadReader = new ScmRemoteFileReaderCacheLocal(sessionId,
                userDetail, localSiteId, remoteSiteId, wsInfo, fileId, majorVersion, minorVersion,
                fileWriter, flag);
        byte[] garbageBuffer = new byte[Const.TRANSMISSION_LEN];
        try {
            while (preLoadReader.read(garbageBuffer, 0, Const.TRANSMISSION_LEN) != -1) {
            }
        }
        finally {
            preLoadReader.close();
        }
    }

    private ScmDataWriter createLocalFileWriter(int localSiteId, ScmWorkspaceInfo wsInfo,
            String fileId, int majorVersion, int minorVersion, long size, ScmDataInfo dataInfo)
            throws ScmServerException {
        ScmDataWriter fileWriter = null;
        try {
            fileWriter = ScmDataOpFactoryAssit.getFactory().createWriter(localSiteId,
                    wsInfo.getName(), wsInfo.getDataLocation(),
                    ScmContentModule.getInstance().getDataService(), dataInfo);
        }
        catch (ScmDatasourceException e) {
            ScmError errorCode = e.getScmError(ScmError.DATA_WRITE_ERROR);
            // DATA_IS_IN_USE : Read and write modes open lob file at the same time in SDB
            if (errorCode == ScmError.DATA_EXIST || errorCode == ScmError.DATA_IS_IN_USE) {
                if (FileCommonOperator.isDataExist(wsInfo, dataInfo, size)) {
                    // data exist, there are two situations:
                    // 1. local site already in the site list
                    // 2. residue data
                    // we have fileContentLock, just add it again.
                    FileCommonOperator.addSiteInfoToList(wsInfo, fileId, majorVersion, minorVersion,
                            localSiteId);
                    innerReader = new ScmLocalFileReader(localSiteId, wsInfo, dataInfo);
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
            logger.warn("create lob in local site failed:siteId={},wsName={},lobId={}",
                    localSiteId, wsInfo.getName(), dataInfo.getId(), e);
        }
        return fileWriter;
    }

    private ScmDataWriter createLocalWriter(int localSiteId, ScmWorkspaceInfo wsInfo,
            ScmDataInfo dataInfo) {
        ScmDataWriter fileWriter = null;
        try {
            fileWriter = ScmDataOpFactoryAssit.getFactory().createWriter(localSiteId,
                    wsInfo.getName(), wsInfo.getDataLocation(),
                    ScmContentModule.getInstance().getDataService(), dataInfo);
        }
        catch (Exception e) {
            logger.warn("create lob in local site failed:siteId={},wsName={},lobId={}",
                    localSiteId, wsInfo.getName(), dataInfo.getId(), e);
        }
        return fileWriter;
    }

    @Override
    public void close() {
        if (innerReader != null) {
            innerReader.close();
            innerReader = null;
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
