package com.sequoiacm.contentserver.job;

import java.util.List;
import java.util.Map;

import org.bson.BSONObject;
import org.bson.types.BasicBSONList;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.sequoiacm.common.CommonDefine;
import com.sequoiacm.common.CommonHelper;
import com.sequoiacm.common.FieldName;
import com.sequoiacm.common.ScmFileLocation;
import com.sequoiacm.contentserver.common.ScmSystemUtils;
import com.sequoiacm.contentserver.dao.FileCommonOperator;
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

public class ScmFileCleanSubTask extends ScmFileSubTask {

    private static final Logger logger = LoggerFactory.getLogger(ScmFileCleanSubTask.class);

    public ScmFileCleanSubTask(BSONObject fileInfo, ScmWorkspaceInfo wsInfo,
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

        int checkSiteId = 0;
        if (getTask().getTaskInfo().containsField(FieldName.Task.FIELD_TARGET_SITE)) {
            // 清理任务不是由生命周期数据流创建的，checkSiteId 值为 0，为 0 代表清理时不检查指定对端站点
            checkSiteId = (int) getTask().getTaskInfo().get(FieldName.Task.FIELD_TARGET_SITE);
        }

        BasicBSONList siteList = BsonUtils.getArrayChecked(fileInfo,
                FieldName.FIELD_CLFILE_FILE_SITE_LIST);
        List<Integer> fileDataSiteIdList = CommonHelper.getFileLocationIdList(siteList);
        if (!fileDataSiteIdList.contains(localSiteId)) {
            logger.warn(
                    "skip, file data not in local site: workspace={}, fileId={}, version={}.{}, fileDataSiteList={}",
                    getWorkspaceInfo().getName(), fileId, majorVersion, minorVersion,
                    fileDataSiteIdList);
            return new DoTaskRes(null, ScmDoFileRes.SKIP);
        }

        if (fileDataSiteIdList.size() < 2) {
            logger.warn(
                    "skip, file data only in local site: workspace={}, fileId={}, version={}.{}, fileDataSiteList={}",
                    getWorkspaceInfo().getName(), fileId, majorVersion, minorVersion,
                    fileDataSiteIdList);
            return new DoTaskRes(null, ScmDoFileRes.SKIP);
        }

        int otherSiteId;
        if (checkSiteId == 0) {
            int localSiteIdx = fileDataSiteIdList.indexOf(localSiteId);
            otherSiteId = localSiteIdx == fileDataSiteIdList.size() - 1 ? fileDataSiteIdList.get(0)
                    : fileDataSiteIdList.get(localSiteIdx + 1);
        }
        else {
            otherSiteId = checkSiteId;
        }

        ScmLock localFileContentLock = null;
        ScmLock otherSiteFileContentLock = null;
        try {
            localFileContentLock = tryLockFileContent(localSiteId, dataId);
            if (localFileContentLock == null) {
                logger.warn(
                        "try lock local data failed, skip this file: workspace={}, fileId={},version={}.{}, dataId={}",
                        getWorkspaceInfo().getName(), fileId, majorVersion, minorVersion, dataId);
                return new DoTaskRes(null, ScmDoFileRes.SKIP);
            }
            otherSiteFileContentLock = tryLockFileContent(otherSiteId, dataId);
            if (otherSiteFileContentLock == null) {
                logger.warn(
                        "try lock remote data failed, skip this file: workspace={}, fileId={},version={}.{},dataId={}",
                        getWorkspaceInfo().getName(), fileId, majorVersion, minorVersion, dataId);
                return new DoTaskRes(null, ScmDoFileRes.SKIP);
            }
            return cleanFile(fileId, majorVersion, minorVersion, otherSiteId);
        }
        catch (ScmServerException e) {
            // skip exception
            if (e.getError() == ScmError.DATA_TYPE_ERROR || e.getError() == ScmError.FILE_NOT_FOUND
                    || e.getError() == ScmError.DATA_NOT_EXIST) {
                logger.warn("skip, clean file failed: workspace={}, fileId={}, version={}.{}",
                        getWorkspaceInfo().getName(), fileId, majorVersion, minorVersion, e);
                return new DoTaskRes(null, ScmDoFileRes.SKIP);
            }

            // failed exception
            if (e.getError() == ScmError.DATA_UNAVAILABLE
                    || e.getError() == ScmError.DATA_CORRUPTED) {
                logger.warn("clean file failed: workspace={}, fileId={}, version={}.{}",
                        getWorkspaceInfo().getName(), fileId, majorVersion, minorVersion, e);
                return new DoTaskRes(null, ScmDoFileRes.FAIL);
            }

            // abort exception
            return new DoTaskRes(e, ScmDoFileRes.ABORT);
        }
        finally {
            unlock(otherSiteFileContentLock);
            unlock(localFileContentLock);
        }

    }

    private DoTaskRes cleanFile(String fileId, int majorVersion, int minorVersion,
            int dataInOtherSiteId)
            throws ScmServerException {
        ScmWorkspaceInfo wsInfo = getWorkspaceInfo();
        BSONObject file = ScmContentModule.getInstance().getMetaService().getFileInfo(
                wsInfo.getMetaLocation(), wsInfo.getName(), fileId, majorVersion, minorVersion);
        if (file == null) {
            logger.warn("skip, file is not exist: workspace={}, fileId={},version={}.{}",
                    wsInfo.getName(), fileId, majorVersion, minorVersion);
            return new DoTaskRes(null, ScmDoFileRes.SKIP);
        }

        int localSiteId = ScmContentModule.getInstance().getLocalSite();
        long size = (long) file.get(FieldName.FIELD_CLFILE_FILE_SIZE);
        BasicBSONList sites = (BasicBSONList) file.get(FieldName.FIELD_CLFILE_FILE_SITE_LIST);
        List<Integer> fileDataSiteIdList = CommonHelper.getFileLocationIdList(sites);
        Map<Integer, ScmFileLocation> fileLocationMap = CommonHelper.getFileLocationList(sites);
        ScmFileLocation localFileLocation = fileLocationMap.get(localSiteId);
        ScmFileLocation otherFileLocation = fileLocationMap.get(dataInOtherSiteId);
        if (localFileLocation == null) {
            logger.warn(
                    "skip, file data is not in local site: workspace={}, fileId={}, version={}.{}, fileDataSiteList={}",
                    getWorkspaceInfo().getName(), fileId, majorVersion, minorVersion,
                    fileDataSiteIdList);
            return new DoTaskRes(null, ScmDoFileRes.SKIP);
        }

        if (otherFileLocation == null) {
            // 锁外检查，文件在本站点和一个远端站点（dataInOtherSiteId），锁住本站点和远端站点后，发现文件已不存在于远端站点，安全起见放弃本站点的文件清理
            logger.warn(
                    "skip, file data is not in locking remote site: workspace={}, fileId={}, version={}.{}, fileDataSiteList={}, lockingRemoteSite={}",
                    getWorkspaceInfo().getName(), fileId, majorVersion, minorVersion,
                    fileDataSiteIdList, dataInOtherSiteId);
            return new DoTaskRes(null, ScmDoFileRes.SKIP);
        }
        ScmWorkspaceInfo ws = getWorkspaceInfo();
        ScmDataInfo localDataInfo = ScmDataInfo.forOpenExistData(file,
                localFileLocation.getWsVersion(),
                localFileLocation.getTableName());
        ScmDataInfo remoteDataInfo = ScmDataInfo.forOpenExistData(file,
                otherFileLocation.getWsVersion(),
                otherFileLocation.getTableName());
        // 确认对端站点数据确实可用(规避极端情况下，数据损坏等问题)，再删除本地站点数据
        if (checkRemoteDataIsSame(fileId, dataInOtherSiteId, ws, localDataInfo, remoteDataInfo, size)) {
            FileCommonOperator.deleteSiteFromFile(ws, fileId, majorVersion, minorVersion,
                    ScmContentModule.getInstance().getLocalSite());
            try {
                ScmDataDeletor deleter = ScmDataOpFactoryAssit.getFactory().createDeletor(
                        ScmContentModule.getInstance().getLocalSite(), ws.getName(),
                        ws.getDataLocation(localDataInfo.getWsVersion()), ScmContentModule.getInstance().getDataService(),
                        localDataInfo);
                deleter.delete();
            }
            catch (ScmDatasourceException e) {
                ScmError scmError = e.getScmError(ScmError.DATA_DELETE_ERROR);
                if (scmError == ScmError.FILE_NOT_FOUND || scmError == ScmError.DATA_NOT_EXIST
                        || scmError == ScmError.DATA_IS_IN_USE
                        || scmError == ScmError.DATA_UNAVAILABLE) {
                    logger.warn("metasource updated successfully, but failed to delete data:fileId="
                            + fileId + ",workspace=" + ws.getName(), e);
                }
                else {
                    throw new ScmServerException(scmError, "Failed to delete file", e);
                }
            }
            return new DoTaskRes(null, ScmDoFileRes.SUCCESS);
        }
        logger.error("file data is not exist in the remote locking site:siteId=" + dataInOtherSiteId
                + ",fileId=" + fileId + ",workspace=" + ws.getName());
        return new DoTaskRes(null, ScmDoFileRes.FAIL);
    }

    private boolean checkRemoteDataIsSame(String fileId, int dataInOtherSiteId, ScmWorkspaceInfo ws,
            ScmDataInfo localDataInfo, ScmDataInfo remoteDataInfo, long size) {
        try {
            String dataCheckLevel = getTask().getDataCheckLevel();
            if (dataCheckLevel.equals(CommonDefine.DataCheckLevel.WEEK)) {
                return FileCommonOperator.isRemoteDataExist(dataInOtherSiteId, ws, remoteDataInfo, size);
            }
            else {
                String localDataMd5 = (String) fileInfo.get(FieldName.FIELD_CLFILE_FILE_MD5);
                if (localDataMd5 == null) {
                    localDataMd5 = ScmSystemUtils.calcMd5(getWorkspaceInfo(), localDataInfo);
                }
                if (taskInfoContext.isSupportRemoteCalcMd5()) {
                    try {
                        return FileCommonOperator.isRemoteDataExistV2(dataInOtherSiteId, ws,
                                remoteDataInfo, localDataMd5);
                    }
                    catch (ScmServerException e) {
                        if (e.getError() == ScmError.OPERATION_UNSUPPORTED) {
                            taskInfoContext.setSupportRemoteCalcMd5(false);
                            return FileCommonOperator.isRemoteDataExistV1(dataInOtherSiteId, ws,
                                    remoteDataInfo, localDataMd5);
                        }
                        throw e;
                    }
                }
                else {
                    return FileCommonOperator.isRemoteDataExistV1(dataInOtherSiteId, ws,
                            remoteDataInfo, localDataMd5);
                }
            }
        }
        catch (Exception e) {
            logger.warn("failed to check data, localSiteId={},remoteSiteId={},fileId={}",
                    ScmContentModule.getInstance().getLocalSite(), dataInOtherSiteId, fileId);
            return false;
        }
    }

    private ScmLock tryLockFileContent(int siteId, String dataId) throws ScmServerException {
        ScmLockPath localFileContentLockPath = ScmLockPathFactory.createFileContentLockPath(
                getWorkspaceInfo().getName(),
                ScmContentModule.getInstance().getSiteInfo(siteId).getName(), dataId);
        return ScmLockManager.getInstance().tryAcquiresLock(localFileContentLockPath);
    }

    private void unlock(ScmLock lock) {
        if (lock != null) {
            lock.unlock();
        }
    }

}
