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
import com.sequoiacm.contentserver.privilege.ScmFileServicePriv;
import com.sequoiacm.contentserver.service.IBreakpointFileService;
import com.sequoiacm.contentserver.site.ScmContentModule;
import com.sequoiacm.datasource.ScmDatasourceException;
import com.sequoiacm.datasource.dataoperation.ENDataType;
import com.sequoiacm.datasource.dataoperation.ScmBreakpointDataWriter;
import com.sequoiacm.datasource.dataoperation.ScmDataInfo;
import com.sequoiacm.exception.ScmError;
import com.sequoiacm.exception.ScmServerException;
import com.sequoiacm.infrastructrue.security.core.ScmUser;
import com.sequoiacm.infrastructrue.security.privilege.ScmPrivilegeDefine;
import com.sequoiacm.infrastructure.audit.ScmAudit;
import com.sequoiacm.infrastructure.audit.ScmAuditType;
import com.sequoiacm.infrastructure.lock.ScmLock;
import com.sequoiacm.infrastructure.monitor.FlowRecorder;
import org.bson.BSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.io.InputStream;
import java.util.Date;
import java.util.List;

@Service
public class BreakpointFileServiceImpl implements IBreakpointFileService {

    private static final Logger logger = LoggerFactory.getLogger(BreakpointFileServiceImpl.class);
    @Autowired
    private ScmAudit audit;

    @Override
    public BreakpointFile getBreakpointFile(ScmUser user, String workspaceName, String fileName)
            throws ScmServerException {
        ScmFileServicePriv.getInstance().checkWsPriority(user, workspaceName,
                ScmPrivilegeDefine.READ, "find breakpoint file");
        ScmContentModule contentModule = ScmContentModule.getInstance();
        contentModule.getWorkspaceInfoCheckLocalSite(workspaceName);

        ScmLock lock = xlock(workspaceName, fileName);
        try {
            BreakpointFile file = contentModule.getMetaService().getBreakpointFile(workspaceName,
                    fileName);
            if (file == null) {
                return null;
            }
            audit.info(ScmAuditType.FILE_DQL, user, workspaceName, 0,
                    "find breakpoint file by file name=" + fileName);
            if (file.isCompleted()) {
                return file;
            }
            if (file.getSiteId() != contentModule.getLocalSite()) {
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
        ScmContentModule contentModule = ScmContentModule.getInstance();
        try {
            ScmBreakpointDataWriter writer = ScmDataOpFactoryAssit.getFactory()
                    .createBreakpointWriter(
                            contentModule.getWorkspaceInfoCheckLocalSite(file.getWorkspaceName())
                                    .getDataLocation(),
                            ScmContentModule.getInstance().getDataService(),
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
    public List<BreakpointFile> getBreakpointFiles(ScmUser user, String workspaceName,
            BSONObject filter) throws ScmServerException {
        ScmFileServicePriv.getInstance().checkWsPriority(user, workspaceName,
                ScmPrivilegeDefine.READ, "list breakpoint files");
        ScmContentModule contentModule = ScmContentModule.getInstance();
        contentModule.getWorkspaceInfoCheckLocalSite(workspaceName);
        List<BreakpointFile> ret = contentModule.getMetaService().listBreakpointFiles(workspaceName,
                filter);
        String message = "list breakpoint files";
        if (null != filter) {
            message += " by filter=" + filter.toString();
        }
        audit.info(ScmAuditType.FILE_DQL, user, workspaceName, 0, message);
        return ret;
    }

    @Override
    public BreakpointFile createBreakpointFile(ScmUser user, String workspaceName, String fileName,
            long createTime, ChecksumType checksumType, InputStream fileStream,
            boolean isLastContent, boolean isNeedMd5) throws ScmServerException {

        ScmFileServicePriv.getInstance().checkWsPriority(user, workspaceName,
                ScmPrivilegeDefine.CREATE, "create breakpoint file");

        ScmContentModule contentModule = ScmContentModule.getInstance();
        if (!contentModule.getDataService().supportsBreakpointUpload()) {
            throw new ScmOperationUnsupportedException(String.format(
                    "Upload BreakpointFile is not support: /%s/%s", workspaceName, fileName));
        }

        ScmWorkspaceInfo workspaceInfo = contentModule.getWorkspaceInfoCheckLocalSite(workspaceName);

        ScmLock lock = xlock(workspaceName, fileName);
        try {
            BreakpointFile file = contentModule.getMetaService().getBreakpointFile(workspaceName,
                    fileName);
            if (file != null) {
                throw new ScmFileExistException(String.format(
                        "BreakpointFile is already exist: /%s/%s", workspaceName, fileName));
            }

            file = contentModule.getMetaService().createBreakpointFile(user.getUsername(),
                    workspaceName, fileName, checksumType, createTime, isNeedMd5);

            if (fileStream != null) {
                BreakpointFileUploader uploader = new BreakpointFileUploader(user.getUsername(),
                        workspaceInfo, file, isLastContent);
                uploader.write(fileStream);
                file = uploader.done();
            }
            audit.info(ScmAuditType.CREATE_FILE, user, workspaceName, 0,
                    "create breakpoint file, fileName=" + fileName);
            return file;
        }
        finally {
            lock.unlock();
        }
    }

    @Override
    public BreakpointFile uploadBreakpointFile(ScmUser user, String workspaceName, String fileName,
            InputStream fileStream, long offset, boolean isLastContent) throws ScmServerException {

        ScmFileServicePriv.getInstance().checkWsPriority(user, workspaceName,
                ScmPrivilegeDefine.CREATE, "upload breakpoint file");
        ScmContentModule contentModule = ScmContentModule.getInstance();
        if (!contentModule.getDataService().supportsBreakpointUpload()) {
            throw new ScmOperationUnsupportedException(String.format(
                    "Upload BreakpointFile is not support: /%s/%s", workspaceName, fileName));
        }

        ScmWorkspaceInfo workspaceInfo = contentModule.getWorkspaceInfoCheckLocalSite(workspaceName);

        ScmLock lock = xlock(workspaceName, fileName);
        try {
            BreakpointFile file = contentModule.getMetaService().getBreakpointFile(workspaceName,
                    fileName);
            if (file == null) {
                throw new ScmFileNotFoundException(String
                        .format("BreakpointFile is not found: /%s/%s", workspaceName, fileName));
            }

            if (file.getSiteId() != contentModule.getLocalSite()) {
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
                uploader = new BreakpointFileUploader(user.getUsername(), workspaceInfo, file,
                        isLastContent);
            }
            catch (ScmServerException e) {
                if (e.getError() == ScmError.DATA_NOT_EXIST) {
                    BreakpointFileMetaCorrector corrector = new BreakpointFileMetaCorrector(file);
                    if (corrector.canCorrect()) {
                        throw new ScmServerException(ScmError.DATA_BREAKPOINT_WRITE_ERROR,
                                "breakpoint file may be completed, try reopen the file: fileName="
                                        + fileName,
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

            audit.info(ScmAuditType.CREATE_FILE, user, workspaceName, 0,
                    "upload breakpoint file, fileName=" + fileName);
            return breakpointFile;
        }
        finally {
            lock.unlock();
        }
    }

    @Override
    public void deleteBreakpointFile(ScmUser user, String workspaceName, String fileName)
            throws ScmServerException {
        ScmFileServicePriv.getInstance().checkWsPriority(user, workspaceName,
                ScmPrivilegeDefine.DELETE, "delete breakpoint file");
        ScmContentModule contentModule = ScmContentModule.getInstance();
        if (!contentModule.getDataService().supportsBreakpointUpload()) {
            throw new ScmOperationUnsupportedException(String.format(
                    "Upload BreakpointFile is not support: /%s/%s", workspaceName, fileName));
        }

        ScmLock lock = xlock(workspaceName, fileName);
        try {
            BreakpointFile file = contentModule.getMetaService().getBreakpointFile(workspaceName,
                    fileName);
            if (file == null) {
                throw new ScmFileNotFoundException(String
                        .format("BreakpointFile is not found: /%s/%s", workspaceName, fileName));
            }

            if (file.getSiteId() != contentModule.getLocalSite()) {
                throw new ScmInvalidArgumentException(
                        String.format("BreakpointFile[/%s/%s] should be uploaded in site[%s]",
                                workspaceName, fileName, file.getSiteName()));
            }

            ScmWorkspaceInfo workspaceInfo = contentModule.getWorkspaceInfoCheckLocalSite(workspaceName);
            BreakpointFileDeleter fileDeleter = new BreakpointFileDeleter(workspaceInfo, file);
            fileDeleter.delete();

            audit.info(ScmAuditType.DELETE_FILE, user, workspaceName, 0,
                    "delete breakpoint file, fileName=" + fileName);
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
    public String calcBreakpointFileMd5(ScmUser user, String workspaceName, String fileName)
            throws ScmServerException {

        ScmFileServicePriv.getInstance().checkWsPriority(user, workspaceName,
                ScmPrivilegeDefine.UPDATE, "calculate breakpoint file md5");
        ScmContentModule contentModule = ScmContentModule.getInstance();
        ScmWorkspaceInfo workspaceInfo = contentModule.getWorkspaceInfoCheckLocalSite(workspaceName);

        ScmLock lock = xlock(workspaceName, fileName);
        try {
            BreakpointFile file = contentModule.getMetaService().getBreakpointFile(workspaceName,
                    fileName);
            if (file == null) {
                throw new ScmFileNotFoundException(String
                        .format("BreakpointFile is not found: /%s/%s", workspaceName, fileName));
            }

            if (file.getSiteId() != contentModule.getLocalSite()) {
                throw new ScmInvalidArgumentException(
                        String.format("BreakpointFile[/%s/%s] should be uploaded in site[%s]",
                                workspaceName, fileName, file.getSiteName()));
            }

            if (!file.isCompleted()) {
                throw new ScmInvalidArgumentException(String
                        .format("BreakpointFile is not complete: /%s/%s", workspaceName, fileName));
            }

            if (file.getMd5() != null) {
                audit.info(ScmAuditType.UPDATE_FILE, user, workspaceName, 0,
                        "calculate breakpoint file md5, fileName=" + fileName);
                return file.getMd5();
            }

            ScmDataInfo dataInfo = new ScmDataInfo(ENDataType.Normal.getValue(), file.getDataId(),
                    new Date(file.getCreateTime()));
            String md5 = ScmSystemUtils.calcMd5(workspaceInfo, dataInfo);
            file.setMd5(md5);
            file.setNeedMd5(true);
            ScmContentModule.getInstance().getMetaService().updateBreakpointFile(file);
            audit.info(ScmAuditType.UPDATE_FILE, user, workspaceName, 0,
                    "calculate breakpoint file md5, fileName=" + fileName);
            return md5;
        }
        finally {
            lock.unlock();
        }
    }
}
