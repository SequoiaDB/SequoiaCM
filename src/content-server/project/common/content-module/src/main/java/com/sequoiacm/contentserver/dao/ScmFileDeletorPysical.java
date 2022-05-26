package com.sequoiacm.contentserver.dao;

import com.sequoiacm.common.FieldName;
import com.sequoiacm.contentserver.bucket.BucketInfoManager;
import com.sequoiacm.contentserver.common.AsyncUtils;
import com.sequoiacm.contentserver.exception.ScmFileNotFoundException;
import com.sequoiacm.contentserver.exception.ScmSystemException;
import com.sequoiacm.contentserver.listener.FileOperationListenerMgr;
import com.sequoiacm.contentserver.lock.ScmLockManager;
import com.sequoiacm.contentserver.lock.ScmLockPath;
import com.sequoiacm.contentserver.lock.ScmLockPathFactory;
import com.sequoiacm.contentserver.model.ScmWorkspaceInfo;
import com.sequoiacm.contentserver.remote.ContentServerClient;
import com.sequoiacm.contentserver.remote.ContentServerClientFactory;
import com.sequoiacm.contentserver.site.ScmContentModule;
import com.sequoiacm.exception.ScmError;
import com.sequoiacm.exception.ScmServerException;
import com.sequoiacm.infrastructure.common.BsonUtils;
import com.sequoiacm.infrastructure.common.ExceptionUtils;
import com.sequoiacm.infrastructure.lock.ScmLock;
import com.sequoiacm.metasource.MetaCursor;
import com.sequoiacm.metasource.MetaFileHistoryAccessor;
import com.sequoiacm.metasource.ScmMetasourceException;
import org.bson.BSONObject;
import org.bson.BasicBSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.util.Assert;
import org.springframework.util.StringUtils;

import java.util.ArrayList;
import java.util.List;

public class ScmFileDeletorPysical implements ScmFileDeletor {
    private static final Logger logger = LoggerFactory.getLogger(ScmFileDeletorPysical.class);
    private final BucketInfoManager bucketInfoMgr;
    ScmContentModule contentModule = ScmContentModule.getInstance();
    private ScmWorkspaceInfo wsInfo;
    private String fileId;

    private String sessionId;
    private String userDetail;
    private FileOperationListenerMgr listenerMgr;

    public ScmFileDeletorPysical(String sessionId, String userDetail, ScmWorkspaceInfo wsInfo,
            String fileId, FileOperationListenerMgr listenerMgr,
            BucketInfoManager bucketInfoManager) {
        this.wsInfo = wsInfo;
        this.fileId = fileId;
        this.sessionId = sessionId;
        this.userDetail = userDetail;
        this.listenerMgr = listenerMgr;
        this.bucketInfoMgr = bucketInfoManager;
    }


    @Override
    public BSONObject delete() throws ScmServerException {
        if (contentModule.getMainSite() != contentModule.getLocalSite()) {
            forwardToMainSite();
        }
        else {
            // add file lock
            ScmLockPath fileLockPath = ScmLockPathFactory.createFileLockPath(wsInfo.getName(),
                    fileId);
            ScmLock wLock = ScmLockManager.getInstance().acquiresWriteLock(fileLockPath);
            try {
                deleteInMainSite();
            }
            finally {
                wLock.unlock();
            }
        }
        return null;
    }

    public BSONObject deleteNoLock() throws ScmServerException {
        if (contentModule.getMainSite() != contentModule.getLocalSite()) {
            forwardToMainSite();
        }
        else {
            deleteInMainSite();
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

    private void deleteInMainSite() throws ScmServerException {
        BSONObject currentFile = contentModule.getMetaService()
                .getFileInfo(wsInfo.getMetaLocation(), wsInfo.getName(), fileId, -1, -1);
        if (null == currentFile) {
            throw new ScmFileNotFoundException("file is unexist:workspace=" + wsInfo.getName()
                    + ",file=" + fileId);
        }

        // if the file belongs to a batch, the relationship needs to be detached
        // before deleting.
        String batchId = (String) currentFile.get(FieldName.FIELD_CLFILE_BATCH_ID);
        if (!StringUtils.isEmpty(batchId)) {
            throw new ScmServerException(ScmError.FILE_IN_ANOTHER_BATCH,
                    "file belongs to a batch, detach the relationship before deleting it:"
                            + "workspace=" + wsInfo.getName() + ",file=" + fileId + ",batch="
                            + batchId);
        }

        final List<BSONObject> allVersionFile = new ArrayList<>();
        allVersionFile.add(currentFile);
        if (!isFirstVersion(currentFile)) {
            addHistoryVersionRecord(allVersionFile, currentFile);
        }

        // delete file meta
        contentModule.getMetaService().deleteFile(wsInfo, currentFile, bucketInfoMgr);

        // delete file data async
        AsyncUtils.execute(new Runnable() {
            @Override
            public void run() {
                for (BSONObject fileRecord : allVersionFile) {
                    if (!BsonUtils.getBooleanOrElse(fileRecord,
                            FieldName.FIELD_CLFILE_DELETE_MARKER, false)) {
                        deleteData(fileRecord);
                    }
                }
            }
        });
        listenerMgr.postDelete(wsInfo, allVersionFile);
    }

    private void deleteData(BSONObject file) {
        ScmFileDataDeleterWrapper fileDataDeleterWrapper = new ScmFileDataDeleterWrapper(wsInfo, file);
        fileDataDeleterWrapper.deleteDataSilence();
    }

    private void addHistoryVersionRecord(List<BSONObject> allVersionFile, BSONObject currentFile)
            throws ScmServerException {
        BSONObject matcher = new BasicBSONObject();
        matcher.put(FieldName.FIELD_CLFILE_ID, fileId);
        matcher.put(FieldName.FIELD_CLFILE_INNER_CREATE_MONTH,
                currentFile.get(FieldName.FIELD_CLFILE_INNER_CREATE_MONTH));

        MetaFileHistoryAccessor historyAccessor = contentModule.getMetaService().getMetaSource()
                .getFileHistoryAccessor(wsInfo.getMetaLocation(), wsInfo.getName(), null);
        MetaCursor cursor = null;
        try {
            cursor = historyAccessor.query(matcher, null);
            while (cursor.hasNext()) {
                allVersionFile.add(cursor.getNext());
            }
        }
        catch (ScmMetasourceException e) {
            throw new ScmServerException(e.getScmError(),
                    "Failed to add history version record:fileid=" + fileId, e);
        }
        finally {
            if (cursor != null) {
                cursor.close();
            }
        }
    }

    private boolean isFirstVersion(BSONObject file) {
        if ((int) file.get(FieldName.FIELD_CLFILE_MAJOR_VERSION) == 1
                && (int) file.get(FieldName.FIELD_CLFILE_MINOR_VERSION) == 0) {
            return true;
        }
        return false;
    }
}
