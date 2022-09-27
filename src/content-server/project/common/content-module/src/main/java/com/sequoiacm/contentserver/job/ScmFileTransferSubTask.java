package com.sequoiacm.contentserver.job;

import com.sequoiacm.common.FieldName;
import com.sequoiacm.contentserver.dao.FileTransferDao;
import com.sequoiacm.contentserver.dao.FileTransferInterrupter;
import com.sequoiacm.contentserver.lock.ScmLockManager;
import com.sequoiacm.contentserver.lock.ScmLockPath;
import com.sequoiacm.contentserver.lock.ScmLockPathFactory;
import com.sequoiacm.contentserver.model.ScmWorkspaceInfo;
import com.sequoiacm.contentserver.site.ScmContentModule;
import com.sequoiacm.exception.ScmError;
import com.sequoiacm.exception.ScmServerException;
import com.sequoiacm.infrastructure.lock.ScmLock;
import org.bson.BSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ScmFileTransferSubTask extends ScmFileSubTask {

    private static final Logger logger = LoggerFactory.getLogger(ScmFileTransferSubTask.class);

    private final ScmTaskTransferFile parent;

    public ScmFileTransferSubTask(BSONObject fileInfo, ScmWorkspaceInfo wsInfo,
            ScmTaskInfoContext taskInfoContext, ScmTaskTransferFile scmTaskTransferFile) {
        super(fileInfo, wsInfo, taskInfoContext);
        this.parent = scmTaskTransferFile;
    }

    @Override
    protected void doTask() throws ScmServerException {
        String fileId = (String) fileInfo.get(FieldName.FIELD_CLFILE_ID);
        String dataId = (String) fileInfo.get(FieldName.FIELD_CLFILE_FILE_DATA_ID);
        int majorVersion = (int) fileInfo.get(FieldName.FIELD_CLFILE_MAJOR_VERSION);
        int minorVersion = (int) fileInfo.get(FieldName.FIELD_CLFILE_MINOR_VERSION);

        int remoteSiteId = (int) getTask().getTaskInfo().get(FieldName.Task.FIELD_TARGET_SITE);
        ScmLockPath fileContentLockPath = ScmLockPathFactory.createFileContentLockPath(
                getWorkspaceInfo().getName(),
                ScmContentModule.getInstance().getSiteInfo(remoteSiteId).getName(), dataId);

        ScmLock fileContentLock = ScmLockManager.getInstance().tryAcquiresLock(fileContentLockPath);
        if (fileContentLock == null) {
            logger.warn("try lock failed, skip this file:fileId={},version={}.{},dataId={}", fileId,
                    majorVersion, minorVersion, dataId);
            taskInfoContext.subTaskFinish(ScmDoFileRes.SKIP);
            return;
        }
        BSONObject file;
        try {
            ScmWorkspaceInfo ws = getWorkspaceInfo();
            file = ScmContentModule.getInstance().getMetaService().getFileInfo(ws.getMetaLocation(),
                    ws.getName(), fileId, majorVersion, minorVersion);
            if (file == null) {
                logger.warn("file not exist, skip this file:fileId={},version={}.{},dataId={}",
                        fileId, majorVersion, minorVersion, dataId);
                taskInfoContext.subTaskFinish(ScmDoFileRes.SKIP);
                return;
            }
            FileTransferInterrupter interrupter = new TaskTransfileInterrupter(parent);
            FileTransferDao fileTrans = new FileTransferDao(getWorkspaceInfo(), remoteSiteId,
                    interrupter, getTask().getDataCheckLevel());
            if (fileTrans.doTransfer(file)) {
                taskInfoContext.subTaskFinish(ScmDoFileRes.SUCCESS);
                return;
            }
            taskInfoContext.subTaskFinish(ScmDoFileRes.SKIP);
            return;
        }
        catch (ScmServerException e) {
            // skip exception
            if (e.getError() == ScmError.DATA_TYPE_ERROR || e.getError() == ScmError.FILE_NOT_FOUND
                    || e.getError() == ScmError.DATA_NOT_EXIST) {
                logger.warn("transfer file failed", e);
                taskInfoContext.subTaskFinish(ScmDoFileRes.SKIP);
                return;
            }
            // failed exception
            if (e.getError() == ScmError.DATA_UNAVAILABLE || e.getError() == ScmError.DATA_CORRUPTED
            // if data exist,it means main site data is exist but check
            // size failed
                    || e.getError() == ScmError.DATA_IS_IN_USE
                    || e.getError() == ScmError.DATA_EXIST) {
                logger.warn("transfer file failed", e);
                taskInfoContext.subTaskFinish(ScmDoFileRes.FAIL);
                return;
            }
            // abort exception
            throw e;
        }
        finally {
            fileContentLock.unlock();
        }
    }
}
