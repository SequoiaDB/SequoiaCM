package com.sequoiacm.contentserver.job;

import org.bson.BSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.sequoiacm.common.CommonDefine;
import com.sequoiacm.common.FieldName;
import com.sequoiacm.contentserver.dao.FileTransferDao;
import com.sequoiacm.contentserver.dao.FileTransferInterrupter;
import com.sequoiacm.contentserver.exception.ScmServerException;
import com.sequoiacm.contentserver.lock.ScmLockManager;
import com.sequoiacm.contentserver.lock.ScmLockPath;
import com.sequoiacm.contentserver.lock.ScmLockPathFactory;
import com.sequoiacm.contentserver.model.ScmWorkspaceInfo;
import com.sequoiacm.contentserver.site.ScmContentServer;
import com.sequoiacm.exception.ScmError;
import com.sequoiacm.infrastructure.lock.ScmLock;

public class ScmTaskTransferFile extends ScmTaskFile {
    private static final Logger logger = LoggerFactory.getLogger(ScmTaskTransferFile.class);

    private FileTransferDao fileTrans;
    private int remoteSiteId;

    public ScmTaskTransferFile(ScmTaskManager mgr, BSONObject info) throws ScmServerException {
        super(mgr, info);

        ScmWorkspaceInfo ws = getWorkspaceInfo();
        FileTransferInterrupter interrupter = new TaskTransfileInterrupter(this);
        remoteSiteId = (int) info.get(FieldName.Task.FIELD_TARGET_SITE);
        fileTrans = new FileTransferDao(ws, remoteSiteId, interrupter);
    }

    @Override
    public int getTaskType() {
        return CommonDefine.TaskType.SCM_TASK_TRANSFER_FILE;
    }

    @Override
    public String getName() {
        return "SCM_TASK_TRANSFER_FILE";
    }

    @Override
    protected DoFileRes doFile(String fileId, int majorVersion, int minorVersion, String dataId)
            throws ScmServerException {
        ScmLockPath fileContentLockPath = ScmLockPathFactory.createFileContentLockPath(
                getWorkspaceInfo().getName(),
                ScmContentServer.getInstance().getSiteInfo(remoteSiteId).getName(), dataId);

        ScmLock fileContentLock = ScmLockManager.getInstance().tryAcquiresLock(
                fileContentLockPath);
        if (fileContentLock == null) {
            logger.warn("try lock failed, skip this file:fileId={},version={}.{},dataId={}",
                    fileId, majorVersion, minorVersion, dataId);
            return DoFileRes.SKIP;
        }

        BSONObject file = null;
        try {
            ScmWorkspaceInfo ws = getWorkspaceInfo();
            file = ScmContentServer
                    .getInstance()
                    .getMetaService()
                    .getFileInfo(ws.getMetaLocation(), ws.getName(), fileId, majorVersion, minorVersion);
            if (file == null) {
                logger.warn("file not exist, skip this file:fileId={},version={}.{},dataId={}", fileId,
                        majorVersion, minorVersion, dataId);
                fileContentLock.unlock();
                return DoFileRes.SKIP;
            }
        }
        catch(Exception e) {
            fileContentLock.unlock();
            throw e;
        }

        try {
            if (fileTrans.doTransfer(file)) {
                return DoFileRes.SUCCESS;
            }
            return DoFileRes.INTERRUPT;
        }
        catch (ScmServerException e) {
            // skip exception
            if (e.getError() == ScmError.DATA_TYPE_ERROR || e.getError() == ScmError.FILE_NOT_FOUND
                    || e.getError() == ScmError.DATA_NOT_EXIST) {
                logger.warn("transfer file failed", e);
                return DoFileRes.SKIP;
            }
            // failed exception
            if (e.getError() == ScmError.DATA_UNAVAILABLE
                    || e.getError() == ScmError.DATA_CORRUPTED
                    // if data exist,it means main site data is exist but check
                    // size failed
                    || e.getError() == ScmError.DATA_EXIST) {
                logger.warn("transfer file failed", e);
                return DoFileRes.FAIL;
            }
            // abort exception
            throw e;
        }
        finally {
            fileContentLock.unlock();
        }
    }
}
