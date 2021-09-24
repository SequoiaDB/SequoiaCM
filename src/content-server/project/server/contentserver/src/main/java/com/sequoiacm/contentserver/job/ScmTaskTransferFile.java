package com.sequoiacm.contentserver.job;

import com.sequoiacm.contentserver.exception.ScmSystemException;
import com.sequoiacm.contentserver.metasourcemgr.ScmMetaSourceHelper;
import org.bson.BSONObject;
import org.bson.BasicBSONObject;
import org.bson.types.BasicBSONList;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.sequoiacm.common.CommonDefine;
import com.sequoiacm.common.FieldName;
import com.sequoiacm.contentserver.dao.FileTransferDao;
import com.sequoiacm.contentserver.dao.FileTransferInterrupter;
import com.sequoiacm.exception.ScmServerException;
import com.sequoiacm.contentserver.lock.ScmLockManager;
import com.sequoiacm.contentserver.lock.ScmLockPath;
import com.sequoiacm.contentserver.lock.ScmLockPathFactory;
import com.sequoiacm.contentserver.model.ScmWorkspaceInfo;
import com.sequoiacm.contentserver.site.ScmContentServer;
import com.sequoiacm.exception.ScmError;
import com.sequoiacm.infrastructure.lock.ScmLock;

public class ScmTaskTransferFile extends ScmTaskFile {
    private static final Logger logger = LoggerFactory.getLogger(ScmTaskTransferFile.class);

    private final FileTransferDao fileTrans;

    public ScmTaskTransferFile(ScmTaskManager mgr, BSONObject info) throws ScmServerException {
        super(mgr, info);

        ScmWorkspaceInfo ws = getWorkspaceInfo();
        FileTransferInterrupter interrupter = new TaskTransfileInterrupter(this);
        int remoteSiteId = (int) info.get(FieldName.Task.FIELD_TARGET_SITE);
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
    protected DoFileRes doFile(BSONObject fileInfoNotInLock) throws ScmServerException {
        String fileId = (String) fileInfoNotInLock.get(FieldName.FIELD_CLFILE_ID);
        String dataId = (String) fileInfoNotInLock.get(FieldName.FIELD_CLFILE_FILE_DATA_ID);
        int majorVersion = (int) fileInfoNotInLock.get(FieldName.FIELD_CLFILE_MAJOR_VERSION);
        int minorVersion = (int) fileInfoNotInLock.get(FieldName.FIELD_CLFILE_MINOR_VERSION);

        int remoteSiteId = (int) taskInfo.get(FieldName.Task.FIELD_TARGET_SITE);
        ScmLockPath fileContentLockPath = ScmLockPathFactory.createFileContentLockPath(
                getWorkspaceInfo().getName(),
                ScmContentServer.getInstance().getSiteInfo(remoteSiteId).getName(), dataId);

        ScmLock fileContentLock = ScmLockManager.getInstance().tryAcquiresLock(fileContentLockPath);
        if (fileContentLock == null) {
            logger.warn("try lock failed, skip this file:fileId={},version={}.{},dataId={}", fileId,
                    majorVersion, minorVersion, dataId);
            return DoFileRes.SKIP;
        }

        BSONObject file;
        try {
            ScmWorkspaceInfo ws = getWorkspaceInfo();
            file = ScmContentServer.getInstance().getMetaService().getFileInfo(ws.getMetaLocation(),
                    ws.getName(), fileId, majorVersion, minorVersion);
            if (file == null) {
                logger.warn("file not exist, skip this file:fileId={},version={}.{},dataId={}",
                        fileId, majorVersion, minorVersion, dataId);
                fileContentLock.unlock();
                return DoFileRes.SKIP;
            }
        }
        catch (Exception e) {
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
            if (e.getError() == ScmError.DATA_UNAVAILABLE || e.getError() == ScmError.DATA_CORRUPTED
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

    @Override
    protected BSONObject buildActualMatcher() throws ScmServerException {
        int remoteSiteId = (int) taskInfo.get(FieldName.Task.FIELD_TARGET_SITE);
        try {
            BasicBSONList matcherList = new BasicBSONList();
            BSONObject taskMatcher = getTaskContent();
            BSONObject mySiteFileMatcher = ScmMetaSourceHelper
                    .dollarSiteInList(ScmContentServer.getInstance().getLocalSite());
            BSONObject targetSiteFileMatcher = ScmMetaSourceHelper
                    .dollarSiteNotInList(remoteSiteId);
            matcherList.add(taskMatcher);
            matcherList.add(mySiteFileMatcher);
            matcherList.add(targetSiteFileMatcher);

            BSONObject needProcessMatcher = new BasicBSONObject();
            needProcessMatcher.put(ScmMetaSourceHelper.SEQUOIADB_MATCHER_AND, matcherList);

            return needProcessMatcher;
        }
        catch (Exception e) {
            logger.error("build actual matcher failed", e);
            throw new ScmSystemException("build actual matcher failed", e);
        }
    }
}
