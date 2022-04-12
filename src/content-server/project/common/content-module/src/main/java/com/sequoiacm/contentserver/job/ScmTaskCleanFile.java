package com.sequoiacm.contentserver.job;

import com.sequoiacm.common.CommonDefine;
import com.sequoiacm.common.CommonHelper;
import com.sequoiacm.common.FieldName;
import com.sequoiacm.contentserver.dao.FileCommonOperator;
import com.sequoiacm.contentserver.datasourcemgr.ScmDataOpFactoryAssit;
import com.sequoiacm.contentserver.exception.ScmSystemException;
import com.sequoiacm.contentserver.lock.ScmLockManager;
import com.sequoiacm.contentserver.lock.ScmLockPath;
import com.sequoiacm.contentserver.lock.ScmLockPathFactory;
import com.sequoiacm.contentserver.metasourcemgr.ScmMetaSourceHelper;
import com.sequoiacm.contentserver.model.ScmWorkspaceInfo;
import com.sequoiacm.contentserver.site.ScmContentModule;
import com.sequoiacm.datasource.ScmDatasourceException;
import com.sequoiacm.datasource.dataoperation.ScmDataDeletor;
import com.sequoiacm.datasource.dataoperation.ScmDataInfo;
import com.sequoiacm.exception.ScmError;
import com.sequoiacm.exception.ScmServerException;
import com.sequoiacm.infrastructure.common.BsonUtils;
import com.sequoiacm.infrastructure.lock.ScmLock;
import org.bson.BSONObject;
import org.bson.BasicBSONObject;
import org.bson.types.BasicBSONList;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;

public class ScmTaskCleanFile extends ScmTaskFile {
    private static final Logger logger = LoggerFactory.getLogger(ScmTaskCleanFile.class);

    public ScmTaskCleanFile(ScmTaskManager mgr, BSONObject info) throws ScmServerException {
        super(mgr, info);
    }

    private DoFileRes cleanFile(String fileId, int majorVersion, int minorVersion,
            int dataInOtherSiteId) throws ScmServerException {
        ScmWorkspaceInfo wsInfo = getWorkspaceInfo();
        BSONObject file = ScmContentModule.getInstance().getMetaService().getFileInfo(
                wsInfo.getMetaLocation(), wsInfo.getName(), fileId, majorVersion, minorVersion);
        if (file == null) {
            logger.warn("skip, file is not exist: workspace={}, fileId={},version={}.{}",
                    wsInfo.getName(), fileId, majorVersion, minorVersion);
            return DoFileRes.SKIP;
        }

        ScmDataInfo dataInfo = new ScmDataInfo(file);
        long size = (long) file.get(FieldName.FIELD_CLFILE_FILE_SIZE);
        BasicBSONList sites = (BasicBSONList) file.get(FieldName.FIELD_CLFILE_FILE_SITE_LIST);
        List<Integer> fileDataSiteIdList = CommonHelper.getFileLocationIdList(sites);

        if (!fileDataSiteIdList.contains(ScmContentModule.getInstance().getLocalSite())) {
            logger.warn(
                    "skip, file data is not in local site: workspace={}, fileId={}, version={}.{}, fileDataSiteList={}",
                    getWorkspaceInfo().getName(), fileId, majorVersion, minorVersion,
                    fileDataSiteIdList);
            return DoFileRes.SKIP;
        }

        if (!fileDataSiteIdList.contains(dataInOtherSiteId)) {
            // 锁外检查，文件在本站点和一个远端站点（dataInOtherSiteId），锁住本站点和远端站点后，发现文件已不存在于远端站点，安全起见放弃本站点的文件清理
            logger.warn(
                    "skip, file data is not in locking remote site: workspace={}, fileId={}, version={}.{}, fileDataSiteList={}, lockingRemoteSite={}",
                    getWorkspaceInfo().getName(), fileId, majorVersion, minorVersion,
                    fileDataSiteIdList, dataInOtherSiteId);
            return DoFileRes.SKIP;
        }
        ScmWorkspaceInfo ws = getWorkspaceInfo();
        // 确认对端站点数据确实可用(规避极端情况下，数据损坏等问题)，再删除本地站点数据
        if (FileCommonOperator.isRemoteDataExist(dataInOtherSiteId, ws, dataInfo, size)) {
            FileCommonOperator.deleteSiteFromFile(ws, fileId, majorVersion, minorVersion,
                    getLocalSiteId());
            try {
                ScmDataDeletor deleter = ScmDataOpFactoryAssit.getFactory().createDeletor(
                        ScmContentModule.getInstance().getLocalSite(), ws.getName(),
                        ws.getDataLocation(), ScmContentModule.getInstance().getDataService(),
                        dataInfo);
                deleter.delete();
            }
            catch (ScmDatasourceException e) {
                ScmError scmError = e.getScmError(ScmError.DATA_DELETE_ERROR);
                if (scmError == ScmError.FILE_NOT_FOUND || scmError == ScmError.DATA_NOT_EXIST
                        || scmError == ScmError.DATA_IS_IN_USE
                        || scmError == ScmError.DATA_UNAVAILABLE) {
                    logger.warn("metasource updated successfully, but failed to delete data:fileId="
                            + fileId + ",workspace=" + ws.getName(), e);
                    return DoFileRes.SUCCESS;
                }
                else {
                    throw new ScmServerException(scmError, "Failed to delete file", e);
                }
            }
            return DoFileRes.SUCCESS;
        }
        logger.warn("file data is not exist in the remote locking site:siteId=" + dataInOtherSiteId
                + ",fileId=" + fileId + ",workspace=" + ws.getName());
        return DoFileRes.SKIP;
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

    @Override
    protected DoFileRes doFile(BSONObject fileInfoNotInLock) throws ScmServerException {
        String fileId = (String) fileInfoNotInLock.get(FieldName.FIELD_CLFILE_ID);
        String dataId = (String) fileInfoNotInLock.get(FieldName.FIELD_CLFILE_FILE_DATA_ID);
        int majorVersion = (int) fileInfoNotInLock.get(FieldName.FIELD_CLFILE_MAJOR_VERSION);
        int minorVersion = (int) fileInfoNotInLock.get(FieldName.FIELD_CLFILE_MINOR_VERSION);

        int localSiteId = ScmContentModule.getInstance().getLocalSite();

        BasicBSONList siteList = BsonUtils.getArrayChecked(fileInfoNotInLock,
                FieldName.FIELD_CLFILE_FILE_SITE_LIST);
        List<Integer> fileDataSiteIdList = CommonHelper.getFileLocationIdList(siteList);
        if (!fileDataSiteIdList.contains(localSiteId)) {
            logger.warn(
                    "skip, file data not in local site: workspace={}, fileId={}, version={}.{}, fileDataSiteList={}",
                    getWorkspaceInfo().getName(), fileId, majorVersion, minorVersion,
                    fileDataSiteIdList);
            return DoFileRes.SKIP;
        }

        if (fileDataSiteIdList.size() < 2) {
            logger.warn(
                    "skip, file data only in local site: workspace={}, fileId={}, version={}.{}, fileDataSiteList={}",
                    getWorkspaceInfo().getName(), fileId, majorVersion, minorVersion,
                    fileDataSiteIdList);
            return DoFileRes.SKIP;
        }

        int localSiteIdx = fileDataSiteIdList.indexOf(localSiteId);
        int otherSiteId = localSiteIdx == fileDataSiteIdList.size() - 1 ? fileDataSiteIdList.get(0)
                : fileDataSiteIdList.get(localSiteIdx + 1);

        ScmLock localFileContentLock = null;
        ScmLock otherSiteFileContentLock = null;
        try {
            localFileContentLock = tryLockFileContent(localSiteId, dataId);
            if (localFileContentLock == null) {
                logger.warn(
                        "try lock local data failed, skip this file: workspace={}, fileId={},version={}.{}, dataId={}",
                        getWorkspaceInfo().getName(), fileId, majorVersion, minorVersion, dataId);
                return DoFileRes.SKIP;
            }
            otherSiteFileContentLock = tryLockFileContent(otherSiteId, dataId);
            if (otherSiteFileContentLock == null) {
                logger.warn(
                        "try lock remote data failed, skip this file: workspace={}, fileId={},version={}.{},dataId={}",
                        getWorkspaceInfo().getName(), fileId, majorVersion, minorVersion, dataId);
                return DoFileRes.SKIP;
            }
            return cleanFile(fileId, majorVersion, minorVersion, otherSiteId);
        }
        catch (ScmServerException e) {
            // skip exception
            if (e.getError() == ScmError.DATA_TYPE_ERROR || e.getError() == ScmError.FILE_NOT_FOUND
                    || e.getError() == ScmError.DATA_NOT_EXIST) {
                logger.warn("skip, clean file failed: workspace={}, fileId={}, version={}.{}",
                        getWorkspaceInfo().getName(), fileId, majorVersion, minorVersion, e);
                return DoFileRes.SKIP;
            }

            // failed exception
            if (e.getError() == ScmError.DATA_UNAVAILABLE
                    || e.getError() == ScmError.DATA_CORRUPTED) {
                logger.warn("clean file failed: workspace={}, fileId={}, version={}.{}",
                        getWorkspaceInfo().getName(), fileId, majorVersion, minorVersion, e);
                return DoFileRes.FAIL;
            }

            // abort exception
            throw e;
        }
        finally {
            unlock(otherSiteFileContentLock);
            unlock(localFileContentLock);
        }
    }

    @Override
    protected BSONObject buildActualMatcher() throws ScmServerException {
        try {
            BasicBSONList matcherList = new BasicBSONList();
            BSONObject taskMatcher = getTaskContent();
            BSONObject mySiteFileMatcher = ScmMetaSourceHelper
                    .dollarSiteInList(ScmContentModule.getInstance().getLocalSite());
            matcherList.add(taskMatcher);
            matcherList.add(mySiteFileMatcher);

            BSONObject needProcessMatcher = new BasicBSONObject();
            needProcessMatcher.put(ScmMetaSourceHelper.SEQUOIADB_MATCHER_AND, matcherList);

            return needProcessMatcher;
        }
        catch (Exception e) {
            logger.error("build actual matcher failed", e);
            throw new ScmSystemException("build actual matcher failed", e);
        }
    }

    @Override
    public int getTaskType() {
        return CommonDefine.TaskType.SCM_TASK_CLEAN_FILE;
    }

    @Override
    public String getName() {
        return "SCM_TASK_CLEAN_FILE";
    }
}
