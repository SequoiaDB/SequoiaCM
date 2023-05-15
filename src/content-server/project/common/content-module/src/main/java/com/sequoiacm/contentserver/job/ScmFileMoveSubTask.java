package com.sequoiacm.contentserver.job;

import java.util.Map;

import org.bson.BSONObject;
import org.bson.types.BasicBSONList;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.sequoiacm.common.CommonHelper;
import com.sequoiacm.common.FieldName;
import com.sequoiacm.common.ScmFileLocation;
import com.sequoiacm.contentserver.common.ScmSystemUtils;
import com.sequoiacm.contentserver.dao.FileCommonOperator;
import com.sequoiacm.contentserver.dao.FileTransferDao;
import com.sequoiacm.contentserver.datasourcemgr.ScmDataOpFactoryAssit;
import com.sequoiacm.contentserver.lock.ScmLockManager;
import com.sequoiacm.contentserver.lock.ScmLockPath;
import com.sequoiacm.contentserver.lock.ScmLockPathFactory;
import com.sequoiacm.contentserver.model.ScmWorkspaceInfo;
import com.sequoiacm.contentserver.site.ScmContentModule;
import com.sequoiacm.datasource.ScmDatasourceException;
import com.sequoiacm.datasource.dataoperation.ScmDataDeletor;
import com.sequoiacm.datasource.dataoperation.ScmDataInfo;
import com.sequoiacm.exception.ScmError;
import com.sequoiacm.exception.ScmServerException;
import com.sequoiacm.infrastructure.common.BsonUtils;
import com.sequoiacm.infrastructure.lock.ScmLock;

public class ScmFileMoveSubTask extends ScmFileSubTask {

    private static final Logger logger = LoggerFactory.getLogger(ScmFileMoveSubTask.class);

    public ScmFileMoveSubTask(BSONObject fileInfo, ScmWorkspaceInfo wsInfo,
            ScmTaskInfoContext taskInfoContext) {
        super(fileInfo, wsInfo, taskInfoContext);
    }

    @Override
    protected DoTaskRes doTask() {
        String fileId = (String) fileInfo.get(FieldName.FIELD_CLFILE_ID);
        String dataId = (String) fileInfo.get(FieldName.FIELD_CLFILE_FILE_DATA_ID);
        int majorVersion = (int) fileInfo.get(FieldName.FIELD_CLFILE_MAJOR_VERSION);
        int minorVersion = (int) fileInfo.get(FieldName.FIELD_CLFILE_MINOR_VERSION);
        int localSiteId = ScmContentModule.getInstance().getLocalSite();
        int targetSiteId = (int) getTask().getTaskInfo().get(FieldName.Task.FIELD_TARGET_SITE);

        BasicBSONList siteList = BsonUtils.getArrayChecked(fileInfo,
                FieldName.FIELD_CLFILE_FILE_SITE_LIST);
        Map<Integer, ScmFileLocation> fileDataSiteList = CommonHelper.getFileLocationList(siteList);
        ScmFileLocation localFileLocation = fileDataSiteList.get(localSiteId);
        if (localFileLocation == null) {
            logger.warn(
                    "skip, file data not in local site: workspace={}, fileId={}, version={}.{}, fileDataSiteList={}",
                    getWorkspaceInfo().getName(), fileId, majorVersion, minorVersion,
                    fileDataSiteList);
            return new DoTaskRes(null, ScmDoFileRes.SKIP);
        }

        ScmLock localFileContentLock = null;
        ScmLock targetSiteFileContentLock = null;
        try {
            localFileContentLock = tryLockFileContent(localSiteId, dataId);
            if (localFileContentLock == null) {
                logger.warn(
                        "try lock local data failed, skip this file: workspace={}, fileId={},version={}.{}, dataId={}",
                        getWorkspaceInfo().getName(), fileId, majorVersion, minorVersion, dataId);
                return new DoTaskRes(null, ScmDoFileRes.SKIP);
            }
            targetSiteFileContentLock = tryLockFileContent(targetSiteId, dataId);
            if (targetSiteFileContentLock == null) {
                logger.warn(
                        "try lock remote data failed, skip this file: workspace={}, fileId={},version={}.{},dataId={}",
                        getWorkspaceInfo().getName(), fileId, majorVersion, minorVersion, dataId);
                return new DoTaskRes(null, ScmDoFileRes.SKIP);
            }
            BSONObject file = ScmContentModule.getInstance().getMetaService().getFileInfo(
                    getWorkspaceInfo().getMetaLocation(), getWorkspaceInfo().getName(), fileId,
                    majorVersion, minorVersion);
            if (file == null) {
                logger.warn("skip, file is not exist: workspace={}, fileId={},version={}.{}",
                        getWorkspaceInfo().getName(), fileId, majorVersion, minorVersion);
                return new DoTaskRes(null, ScmDoFileRes.SKIP);
            }

            ScmWorkspaceInfo ws = getWorkspaceInfo();
            FileTransferDao fileTransferDao = new FileTransferDao(ws, targetSiteId,
                    new TaskTransfileInterrupter(getTask()), getTask().getDataCheckLevel(),
                    taskInfoContext);
            FileTransferDao.FileTransferResult transferResult = fileTransferDao.doTransfer(file);
            if (transferResult == FileTransferDao.FileTransferResult.SUCCESS) {
                ScmDataInfo dataInfo = ScmDataInfo.forOpenExistData(fileInfo,
                        localFileLocation.getWsVersion(),
                        localFileLocation.getTableName());
                cleanFile(ws, fileId, majorVersion, minorVersion, dataInfo);
                return new DoTaskRes(null, ScmDoFileRes.SUCCESS);
            }
            else if (transferResult == FileTransferDao.FileTransferResult.DATA_INCORRECT) {
                return new DoTaskRes(null, ScmDoFileRes.FAIL);
            }
            else {
                return new DoTaskRes(null, ScmDoFileRes.SKIP);
            }

        }
        catch (ScmServerException e) {
            // skip exception
            if (e.getError() == ScmError.DATA_TYPE_ERROR || e.getError() == ScmError.FILE_NOT_FOUND
                    || e.getError() == ScmError.DATA_NOT_EXIST) {
                logger.warn("skip, move file failed: workspace={}, fileId={}, version={}.{}",
                        getWorkspaceInfo().getName(), fileId, majorVersion, minorVersion, e);
                return new DoTaskRes(null, ScmDoFileRes.SKIP);
            }

            // failed exception
            if (e.getError() == ScmError.DATA_UNAVAILABLE || e.getError() == ScmError.DATA_CORRUPTED
            // if data exist,it means main site data is exist but check
            // size failed
                    || e.getError() == ScmError.DATA_EXIST
                    || e.getError() == ScmError.DATA_IS_IN_USE) {
                logger.warn("move file failed: workspace={}, fileId={}, version={}.{}",
                        getWorkspaceInfo().getName(), fileId, majorVersion, minorVersion, e);
                return new DoTaskRes(null, ScmDoFileRes.FAIL);
            }

            // abort exception
            return new DoTaskRes(e, ScmDoFileRes.ABORT);
        }
        finally {
            if (localFileContentLock != null) {
                localFileContentLock.unlock();
            }
            if (targetSiteFileContentLock != null) {
                targetSiteFileContentLock.unlock();
            }
        }

    }

    private void cleanFile(ScmWorkspaceInfo ws, String fileId, int majorVersion, int minorVersion,
            ScmDataInfo dataInfo) throws ScmServerException {
        FileCommonOperator.deleteSiteFromFile(ws, fileId, majorVersion, minorVersion,
                ScmContentModule.getInstance().getLocalSite());
        try {
            ScmDataDeletor deleter = ScmDataOpFactoryAssit.getFactory().createDeletor(
                    ScmContentModule.getInstance().getLocalSite(), ws.getName(),
                    ws.getDataLocation(dataInfo.getWsVersion()),
                    ScmContentModule.getInstance().getDataService(),
                    dataInfo);
            deleter.delete();
        }
        catch (ScmDatasourceException e) {
            throw new ScmServerException(e.getScmError(ScmError.DATA_DELETE_ERROR),
                    "Failed to delete file:fileId=" + fileId + ",version="
                            + ScmSystemUtils.getVersionStr(majorVersion, minorVersion),
                    e);
        }
    }

    private ScmLock tryLockFileContent(int siteId, String dataId) throws ScmServerException {
        ScmLockPath localFileContentLockPath = ScmLockPathFactory.createFileContentLockPath(
                getWorkspaceInfo().getName(),
                ScmContentModule.getInstance().getSiteInfo(siteId).getName(), dataId);
        return ScmLockManager.getInstance().tryAcquiresLock(localFileContentLockPath);
    }
}
