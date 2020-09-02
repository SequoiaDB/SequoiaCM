package com.sequoiacm.contentserver.job;

import java.util.ArrayList;
import java.util.List;

import org.bson.BSONObject;
import org.bson.types.BasicBSONList;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.sequoiacm.common.CommonDefine;
import com.sequoiacm.common.CommonHelper;
import com.sequoiacm.common.FieldName;
import com.sequoiacm.common.ScmFileLocation;
import com.sequoiacm.contentserver.dao.FileCommonOperator;
import com.sequoiacm.contentserver.datasourcemgr.ScmDataOpFactoryAssit;
import com.sequoiacm.exception.ScmServerException;
import com.sequoiacm.contentserver.lock.ScmLockManager;
import com.sequoiacm.contentserver.lock.ScmLockPath;
import com.sequoiacm.contentserver.lock.ScmLockPathFactory;
import com.sequoiacm.contentserver.model.ScmWorkspaceInfo;
import com.sequoiacm.contentserver.site.ScmContentServer;
import com.sequoiacm.contentserver.strategy.ScmStrategyMgr;
import com.sequoiacm.datasource.ScmDatasourceException;
import com.sequoiacm.datasource.dataoperation.ScmDataDeletor;
import com.sequoiacm.datasource.dataoperation.ScmDataInfo;
import com.sequoiacm.exception.ScmError;
import com.sequoiacm.infrastructure.lock.ScmLock;

public class ScmTaskCleanFile extends ScmTaskFile {
    private static final Logger logger = LoggerFactory.getLogger(ScmTaskCleanFile.class);

    public ScmTaskCleanFile(ScmTaskManager mgr, BSONObject info) throws ScmServerException {
        super(mgr, info);
    }

    private DoFileRes cleanFile(String fileId, int majorVersion, int minorVersion)
            throws ScmServerException {
        ScmWorkspaceInfo wsInfo = getWorkspaceInfo();
        BSONObject file = ScmContentServer
                .getInstance()
                .getMetaService()
                .getFileInfo(wsInfo.getMetaLocation(), wsInfo.getName(), fileId, majorVersion,
                        minorVersion);
        if (file == null) {
            logger.warn("skip, file is not exist:fileId={},version={}.{}", fileId, majorVersion,
                    minorVersion);
            return DoFileRes.SKIP;
        }
        long size = (long) file.get(FieldName.FIELD_CLFILE_FILE_SIZE);
        ScmDataInfo dataInfo = new ScmDataInfo(file);
        BasicBSONList sites = (BasicBSONList) file.get(FieldName.FIELD_CLFILE_FILE_SITE_LIST);
        List<ScmFileLocation> siteList = new ArrayList<>();
        CommonHelper.getFileLocationList(sites, siteList);
        List<Integer> siteIdList = CommonHelper.getFileLocationIdList(siteList);

        ScmWorkspaceInfo ws = getWorkspaceInfo();
        int localSite = ScmContentServer.getInstance().getLocalSite();
        List<Integer> verifySites = ScmStrategyMgr.getInstance().getVerifySites(ws, siteIdList,
                localSite);
        if (verifySites.size() == 0) {
            logger.warn("the file exists only at local site:workspace=" + ws.getName() + ",fileId="
                    + fileId);
            return DoFileRes.SKIP;
        }

        for (int site : verifySites) {
            if (FileCommonOperator.isRemoteDataExist(site, ws, dataInfo, size)) {
                // if occur exception here,just throw it,caller will process
                FileCommonOperator.deleteSiteFromFile(ws, fileId, majorVersion, minorVersion,
                        getLocalSiteId());
                try {
                    ScmDataDeletor deletor = ScmDataOpFactoryAssit.getFactory().createDeletor(
                            ScmContentServer.getInstance().getLocalSite(), ws.getName(),
                            ws.getDataLocation(), ScmContentServer.getInstance().getDataService(),
                            dataInfo);
                    deletor.delete();
                }
                catch (ScmDatasourceException e) {
                    throw new ScmServerException(e.getScmError(ScmError.DATA_DELETE_ERROR),
                            "Failed to delete file", e);
                }
                return DoFileRes.SUCCESS;
                // not need to close deletor
            }
            else {
                logger.warn("file data is not exist in the site:siteId=" + site + ",fileId="
                        + fileId + ",workspace=" + ws.getName());
            }
        }
        return DoFileRes.SKIP;
    }

    @Override
    protected DoFileRes doFile(String fileId, int majorVersion, int minorVersion, String dataId)
            throws ScmServerException {
        ScmLockPath fileContentLockPath = ScmLockPathFactory.createFileContentLockPath(
                getWorkspaceInfo().getName(), ScmContentServer.getInstance().getLocalSiteInfo()
                .getName(), dataId);
        ScmLock fileContentLock = ScmLockManager.getInstance().tryAcquiresLock(
                fileContentLockPath);
        if (fileContentLock == null) {
            logger.warn("try lock failed, skip this file:fileId={},version={}.{},dataId={}",
                    fileId, majorVersion, minorVersion, dataId);
            return DoFileRes.SKIP;
        }

        try {
            return cleanFile(fileId, majorVersion, minorVersion);
        }
        catch (ScmServerException e) {
            // skip exception
            if (e.getError() == ScmError.DATA_TYPE_ERROR || e.getError() == ScmError.FILE_NOT_FOUND
                    || e.getError() == ScmError.DATA_NOT_EXIST) {
                logger.warn("clean file failed", e);
                return DoFileRes.SKIP;
            }

            // failed exception
            if (e.getError() == ScmError.DATA_UNAVAILABLE
                    || e.getError() == ScmError.DATA_CORRUPTED) {
                logger.warn("clean file failed", e);
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
    public int getTaskType() {
        return CommonDefine.TaskType.SCM_TASK_CLEAN_FILE;
    }

    @Override
    public String getName() {
        return "SCM_TASK_CLEAN_FILE";
    }
}
