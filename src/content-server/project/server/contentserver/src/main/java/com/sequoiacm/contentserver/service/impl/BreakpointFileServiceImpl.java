package com.sequoiacm.contentserver.service.impl;

import com.sequoiacm.common.checksum.ChecksumType;
import com.sequoiacm.contentserver.common.ScmSystemUtils;
import com.sequoiacm.contentserver.dao.BreakpointFileDeleter;
import com.sequoiacm.contentserver.dao.BreakpointFileMetaCorrector;
import com.sequoiacm.contentserver.dao.BreakpointFileUploader;
import com.sequoiacm.contentserver.datasourcemgr.ScmDataOpFactoryAssit;
import com.sequoiacm.contentserver.exception.ScmFileExistException;
import com.sequoiacm.contentserver.exception.ScmFileNotFoundException;
import com.sequoiacm.contentserver.exception.ScmInvalidArgumentException;
import com.sequoiacm.contentserver.exception.ScmOperationUnsupportedException;
import com.sequoiacm.contentserver.lock.ScmLockManager;
import com.sequoiacm.contentserver.lock.ScmLockPath;
import com.sequoiacm.contentserver.lock.ScmLockPathFactory;
import com.sequoiacm.contentserver.model.BreakpointFile;
import com.sequoiacm.contentserver.model.ScmWorkspaceInfo;
import com.sequoiacm.contentserver.service.IBreakpointFileService;
import com.sequoiacm.contentserver.site.ScmContentServer;
import com.sequoiacm.datasource.ScmDatasourceException;
import com.sequoiacm.datasource.dataoperation.ENDataType;
import com.sequoiacm.datasource.dataoperation.ScmBreakpointDataWriter;
import com.sequoiacm.datasource.dataoperation.ScmDataInfo;
import com.sequoiacm.exception.ScmError;
import com.sequoiacm.exception.ScmServerException;
import com.sequoiacm.infrastructure.lock.ScmLock;
import com.sequoiacm.infrastructure.monitor.FlowRecorder;
import org.bson.BSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.io.InputStream;
import java.util.Date;
import java.util.List;

@Service
public class BreakpointFileServiceImpl implements IBreakpointFileService {

    private static final Logger logger = LoggerFactory.getLogger(BreakpointFileServiceImpl.class);

    @Override
    public BreakpointFile getBreakpointFile(String workspaceName, String fileName)
            throws ScmServerException {
        ScmContentServer contentServer = ScmContentServer.getInstance();
        contentServer.getWorkspaceInfoChecked(workspaceName);

        ScmLock lock = xlock(workspaceName, fileName);
        try {
            BreakpointFile file = contentServer.getMetaService().getBreakpointFile(workspaceName,
                    fileName);
            if (file == null) {
                return null;
            }
            if (file.isCompleted()) {
                return file;
            }
            if (file.getSiteId() != contentServer.getLocalSite()) {
                return file;
            }

            // 元数据显示断点文件不完整，且断点文件数据在本地站点，
            // 此时检查断点文件数据是否还在，若不在则检查断点文件数据是否已经合并，若已合并修正断点文件元数据
            if (isBreakpointDataExist(file)) {
                return file;
            }
            BreakpointFileMetaCorrector corrector = new BreakpointFileMetaCorrector(file);
            return corrector.correct();
        }
        finally {
            lock.unlock();
        }
    }

    private boolean isBreakpointDataExist(BreakpointFile file) throws ScmServerException {
        ScmContentServer contentServer = ScmContentServer.getInstance();
        try {
            ScmBreakpointDataWriter writer = ScmDataOpFactoryAssit.getFactory()
                    .createBreakpointWriter(
                            contentServer.getWorkspaceInfoChecked(file.getWorkspaceName())
                                    .getDataLocation(),
                            ScmContentServer.getInstance().getDataService(),
                            file.getWorkspaceName(), file.getFileName(), file.getDataId(),
                            new Date(file.getCreateTime()), false, file.getUploadSize(),
                            file.getExtraContext());
            writer.close();
            return true;
        }
        catch (ScmDatasourceException e) {
            if (e.getScmError(ScmError.DATA_ERROR) == ScmError.DATA_NOT_EXIST) {
                return false;
            }
            throw new ScmServerException(e.getScmError(ScmError.DATA_ERROR),
                    "failed to check breakpoint data status: breakpointFile=" + file, e);
        }
    }

    @Override
    public List<BreakpointFile> getBreakpointFiles(String workspaceName, BSONObject filter)
            throws ScmServerException {
        ScmContentServer contentServer = ScmContentServer.getInstance();
        contentServer.getWorkspaceInfoChecked(workspaceName);
        return contentServer.getMetaService().listBreakpointFiles(workspaceName, filter);
    }

    @Override
    public BreakpointFile createBreakpointFile(String createUser, String workspaceName,
            String fileName, long createTime, ChecksumType checksumType, InputStream fileStream,
            boolean isLastContent, boolean isNeedMd5) throws ScmServerException {
        ScmContentServer contentServer = ScmContentServer.getInstance();
        if (!contentServer.getDataService().supportsBreakpointUpload()) {
            throw new ScmOperationUnsupportedException(String.format(
                    "Upload BreakpointFile is not support: /%s/%s", workspaceName, fileName));
        }

        ScmWorkspaceInfo workspaceInfo = contentServer.getWorkspaceInfoChecked(workspaceName);

        ScmLock lock = xlock(workspaceName, fileName);
        try {
            BreakpointFile file = contentServer.getMetaService().getBreakpointFile(workspaceName,
                    fileName);
            if (file != null) {
                throw new ScmFileExistException(String.format(
                        "BreakpointFile is already exist: /%s/%s", workspaceName, fileName));
            }

            file = contentServer.getMetaService().createBreakpointFile(createUser, workspaceName,
                    fileName, checksumType, createTime, isNeedMd5);

            if (fileStream != null) {
                BreakpointFileUploader uploader = new BreakpointFileUploader(createUser,
                        workspaceInfo, file, isLastContent);
                uploader.write(fileStream);
                return uploader.done();
            }
            else {
                return file;
            }
        }
        finally {
            lock.unlock();
        }
    }

    @Override
    public BreakpointFile uploadBreakpointFile(String uploadUser, String workspaceName,
            String fileName, InputStream fileStream, long offset, boolean isLastContent)
            throws ScmServerException {
        ScmContentServer contentServer = ScmContentServer.getInstance();
        if (!contentServer.getDataService().supportsBreakpointUpload()) {
            throw new ScmOperationUnsupportedException(String.format(
                    "Upload BreakpointFile is not support: /%s/%s", workspaceName, fileName));
        }

        ScmWorkspaceInfo workspaceInfo = contentServer.getWorkspaceInfoChecked(workspaceName);

        ScmLock lock = xlock(workspaceName, fileName);
        try {
            BreakpointFile file = contentServer.getMetaService().getBreakpointFile(workspaceName,
                    fileName);
            if (file == null) {
                throw new ScmFileNotFoundException(String
                        .format("BreakpointFile is not found: /%s/%s", workspaceName, fileName));
            }

            if (file.getSiteId() != contentServer.getLocalSite()) {
                throw new ScmInvalidArgumentException(
                        String.format("BreakpointFile[/%s/%s] should be uploaded in site[%s]",
                                workspaceName, fileName, file.getSiteName()));
            }

            if (file.isCompleted()) {
                throw new ScmInvalidArgumentException(
                        String.format("Completed BreakpointFile: /%s/%s", workspaceName, fileName));
            }

            if (offset != file.getUploadSize()) {
                throw new ScmInvalidArgumentException(
                        String.format("Invalid BreakpointFile offset: %d", offset));
            }

            BreakpointFileUploader uploader;
            try {
                uploader = new BreakpointFileUploader(uploadUser, workspaceInfo, file,
                        isLastContent);
            }
            catch (ScmServerException e) {
                if (e.getError() == ScmError.DATA_NOT_EXIST) {
                    BreakpointFileMetaCorrector corrector = new BreakpointFileMetaCorrector(file);
                    if (corrector.canCorrect()) {
                        throw new ScmServerException(ScmError.DATA_BREAKPOINT_WRITE_ERROR,
                                "breakpoint file may be completed, try reopen the file: fileName=" + fileName,
                                e);
                    }
                }
                throw e;
            }
            uploader.write(fileStream);
            BreakpointFile breakpointFile = uploader.done();

            long uploadSize = breakpointFile.getUploadSize();
            try {
                FlowRecorder.getInstance().addUploadSize(workspaceName, uploadSize);
            }
            catch (Exception e) {
                logger.error("add flow record failed", e);
            }
            return breakpointFile;
        }
        finally {
            lock.unlock();
        }
    }

    @Override
    public void deleteBreakpointFile(String workspaceName, String fileName)
            throws ScmServerException {
        ScmContentServer contentServer = ScmContentServer.getInstance();
        if (!contentServer.getDataService().supportsBreakpointUpload()) {
            throw new ScmOperationUnsupportedException(String.format(
                    "Upload BreakpointFile is not support: /%s/%s", workspaceName, fileName));
        }

        ScmLock lock = xlock(workspaceName, fileName);
        try {
            BreakpointFile file = contentServer.getMetaService().getBreakpointFile(workspaceName,
                    fileName);
            if (file == null) {
                throw new ScmFileNotFoundException(String
                        .format("BreakpointFile is not found: /%s/%s", workspaceName, fileName));
            }

            if (file.getSiteId() != contentServer.getLocalSite()) {
                throw new ScmInvalidArgumentException(
                        String.format("BreakpointFile[/%s/%s] should be uploaded in site[%s]",
                                workspaceName, fileName, file.getSiteName()));
            }

            ScmWorkspaceInfo workspaceInfo = contentServer.getWorkspaceInfoChecked(workspaceName);
            BreakpointFileDeleter fileDeleter = new BreakpointFileDeleter(workspaceInfo, file);
            fileDeleter.delete();
        }
        finally {
            lock.unlock();
        }
    }

    private static ScmLock xlock(String workspaceName, String fileName) throws ScmServerException {
        ScmLockPath lockPath = ScmLockPathFactory.createBPLockPath(workspaceName, fileName);
        return ScmLockManager.getInstance().acquiresLock(lockPath);
    }

    @Override
    public String calcBreakpointFileMd5(String workspaceName, String fileName)
            throws ScmServerException {
        ScmContentServer contentServer = ScmContentServer.getInstance();
        ScmWorkspaceInfo workspaceInfo = contentServer.getWorkspaceInfoChecked(workspaceName);

        ScmLock lock = xlock(workspaceName, fileName);
        try {
            BreakpointFile file = contentServer.getMetaService().getBreakpointFile(workspaceName,
                    fileName);
            if (file == null) {
                throw new ScmFileNotFoundException(String
                        .format("BreakpointFile is not found: /%s/%s", workspaceName, fileName));
            }

            if (file.getSiteId() != contentServer.getLocalSite()) {
                throw new ScmInvalidArgumentException(
                        String.format("BreakpointFile[/%s/%s] should be uploaded in site[%s]",
                                workspaceName, fileName, file.getSiteName()));
            }

            if (!file.isCompleted()) {
                throw new ScmInvalidArgumentException(String
                        .format("BreakpointFile is not complete: /%s/%s", workspaceName, fileName));
            }

            if (file.getMd5() != null) {
                return file.getMd5();
            }

            ScmDataInfo dataInfo = new ScmDataInfo(ENDataType.Normal.getValue(), file.getDataId(),
                    new Date(file.getCreateTime()));
            String md5 = ScmSystemUtils.calcMd5(workspaceInfo, dataInfo);
            file.setMd5(md5);
            file.setNeedMd5(true);
            ScmContentServer.getInstance().getMetaService().updateBreakpointFile(file);
            return md5;
        }
        finally {
            lock.unlock();
        }
    }
}
