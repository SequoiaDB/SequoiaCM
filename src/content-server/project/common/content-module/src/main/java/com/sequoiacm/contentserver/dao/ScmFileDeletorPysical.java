package com.sequoiacm.contentserver.dao;

import com.sequoiacm.contentserver.common.AsyncUtils;
import com.sequoiacm.contentserver.exception.ScmSystemException;
import com.sequoiacm.contentserver.listener.FileOperationListenerMgr;
import com.sequoiacm.contentserver.lock.ScmLockManager;
import com.sequoiacm.contentserver.lock.ScmLockPath;
import com.sequoiacm.contentserver.lock.ScmLockPathFactory;
import com.sequoiacm.contentserver.model.ScmWorkspaceInfo;
import com.sequoiacm.contentserver.pipeline.file.module.DeleteFileResult;
import com.sequoiacm.contentserver.pipeline.file.module.FileMeta;
import com.sequoiacm.contentserver.pipeline.file.FileMetaOperator;
import com.sequoiacm.contentserver.remote.ContentServerClient;
import com.sequoiacm.contentserver.remote.ContentServerClientFactory;
import com.sequoiacm.contentserver.site.ScmContentModule;
import com.sequoiacm.exception.ScmServerException;
import com.sequoiacm.infrastructure.common.ExceptionUtils;
import com.sequoiacm.infrastructure.lock.ScmLock;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.util.Assert;

public class ScmFileDeletorPysical implements ScmFileDeletor {
    private static final Logger logger = LoggerFactory.getLogger(ScmFileDeletorPysical.class);
    private final FileMetaOperator fileMetaOperator;

    ScmContentModule contentModule = ScmContentModule.getInstance();
    private ScmWorkspaceInfo wsInfo;
    private String fileId;

    private String sessionId;
    private String userDetail;
    private FileOperationListenerMgr listenerMgr;

    public ScmFileDeletorPysical(String sessionId, String userDetail, ScmWorkspaceInfo wsInfo,
            String fileId, FileOperationListenerMgr listenerMgr,
            FileMetaOperator fileMetaOperator) {
        this.wsInfo = wsInfo;
        this.fileId = fileId;
        this.sessionId = sessionId;
        this.userDetail = userDetail;
        this.listenerMgr = listenerMgr;
        this.fileMetaOperator = fileMetaOperator;
    }


    @Override
    public FileMeta delete() throws ScmServerException {
        if (contentModule.getMainSite() != contentModule.getLocalSite()) {
            forwardToMainSite();
        }
        else {
            // add file lock
            ScmLockPath fileLockPath = ScmLockPathFactory.createFileLockPath(wsInfo.getName(),
                    fileId);
            ScmLock wLock = ScmLockManager.getInstance().acquiresWriteLock(fileLockPath);
            try {
                deleteInMainSiteNoLock();
            }
            finally {
                wLock.unlock();
            }
        }
        return null;
    }

    private void forwardToMainSite() throws ScmServerException {
        Assert.notNull(sessionId, "sessionIdis null, forward mainSite failed");
        Assert.notNull(userDetail, "userDetail is null, forward mainSite failed");
        ScmContentModule contentModule = ScmContentModule.getInstance();
        String remoteSiteName = contentModule.getMainSiteName();
        try {
            ContentServerClient client = ContentServerClientFactory
                    .getFeignClientByServiceName(remoteSiteName);
            client.deleteFile(sessionId, userDetail, wsInfo.getName(), fileId, -1, -1, true);
        }
        catch (ScmServerException e) {
            throw e;
        }
        catch (Exception e) {
            if (ExceptionUtils.causedBySocketTimeout(e)) {
                throw new ScmSystemException(
                        "forwardToMainSite timed out: remote=" + remoteSiteName, e);
            }
            throw new ScmSystemException("forwardToMainSite failed: remote=" + remoteSiteName, e);
        }
    }


    public void deleteInMainSiteNoLock() throws ScmServerException {
        // delete file meta
        DeleteFileResult deleteRes = fileMetaOperator.deleteFileMeta(wsInfo.getName(), fileId);

        // delete file data async
        AsyncUtils.execute(new Runnable() {
            @Override
            public void run() {
                for (FileMeta fileRecord : deleteRes.getDeletedVersion()) {
                    if (!fileRecord.isDeleteMarker()) {
                        deleteData(fileRecord);
                    }
                }
            }
        });
        listenerMgr.postDelete(wsInfo, deleteRes.getDeletedVersion());
    }

    private void deleteData(FileMeta file) {
        ScmFileDataDeleterWrapper fileDataDeleterWrapper = new ScmFileDataDeleterWrapper(wsInfo, file);
        fileDataDeleterWrapper.deleteDataSilence();
    }


}
