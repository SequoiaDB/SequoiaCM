package com.sequoiacm.contentserver.dao;

import java.util.ArrayList;
import java.util.List;

import org.bson.BSONObject;
import org.bson.BasicBSONObject;
import org.bson.types.BasicBSONList;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.util.Assert;
import org.springframework.util.StringUtils;

import com.sequoiacm.common.CommonHelper;
import com.sequoiacm.common.FieldName;
import com.sequoiacm.common.ScmFileLocation;
import com.sequoiacm.contentserver.common.ScmSystemUtils;
import com.sequoiacm.contentserver.datasourcemgr.ScmDataOpFactoryAssit;
import com.sequoiacm.contentserver.exception.ScmFileNotFoundException;
import com.sequoiacm.contentserver.exception.ScmSystemException;
import com.sequoiacm.contentserver.listener.FileOperationListenerMgr;
import com.sequoiacm.contentserver.lock.ScmLockManager;
import com.sequoiacm.contentserver.lock.ScmLockPath;
import com.sequoiacm.contentserver.lock.ScmLockPathFactory;
import com.sequoiacm.contentserver.model.ScmWorkspaceInfo;
import com.sequoiacm.contentserver.remote.ContentServerClient;
import com.sequoiacm.contentserver.remote.ContentServerClientFactory;
import com.sequoiacm.contentserver.remote.ScmInnerRemoteDataDeletor;
import com.sequoiacm.contentserver.site.ScmContentServer;
import com.sequoiacm.datasource.dataoperation.ScmDataDeletor;
import com.sequoiacm.datasource.dataoperation.ScmDataInfo;
import com.sequoiacm.exception.ScmError;
import com.sequoiacm.exception.ScmServerException;
import com.sequoiacm.infrastructure.lock.ScmLock;
import com.sequoiacm.metasource.MetaCursor;
import com.sequoiacm.metasource.MetaFileHistoryAccessor;
import com.sequoiacm.metasource.ScmMetasourceException;

public class ScmFileDeletorPysical implements ScmFileDeletor {
    private static final Logger logger = LoggerFactory.getLogger(ScmFileDeletorPysical.class);
    ScmContentServer contentServer = ScmContentServer.getInstance();
    private ScmWorkspaceInfo wsInfo;
    private String fileId;
    private int majorVersion;
    private int minorVersion;

    private String sessionId;
    private String userDetail;
    private FileOperationListenerMgr listenerMgr;

    public ScmFileDeletorPysical(String sessionId, String userDetail, ScmWorkspaceInfo wsInfo,
            String fileId, int majorVersion, int minorVersion,
            FileOperationListenerMgr listenerMgr) {
        this.wsInfo = wsInfo;
        this.fileId = fileId;
        this.majorVersion = majorVersion;
        this.minorVersion = minorVersion;
        this.sessionId = sessionId;
        this.userDetail = userDetail;
        this.listenerMgr = listenerMgr;
    }

    @Override
    public void delete() throws ScmServerException {
        if (contentServer.getMainSite() != contentServer.getLocalSite()) {
            Assert.notNull(sessionId, "sessionIdis null, forward mainSite failed");
            Assert.notNull(userDetail, "userDetail is null, forward mainSite failed");
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
    }

    private void forwardToMainSite() throws ScmServerException {
        ScmContentServer contentServer = ScmContentServer.getInstance();
        String remoteSiteName = contentServer.getMainSiteName();
        try {
            ContentServerClient client = ContentServerClientFactory
                    .getFeignClientByServiceName(remoteSiteName);
            client.deleteFile(sessionId, userDetail, wsInfo.getName(), fileId, majorVersion,
                    minorVersion, true);
        }
        catch (ScmServerException e) {
            throw e;
        }
        catch (Exception e) {
            throw new ScmSystemException("forwardToMainSite failed:,remote=" + remoteSiteName, e);
        }
    }

    private void deleteInMainSite() throws ScmServerException {
        BSONObject currentFile = contentServer.getCurrentFileInfo(wsInfo, fileId);
        if (null == currentFile) {
            throw new ScmFileNotFoundException("file is unexist:workspace=" + wsInfo.getName()
                    + ",file=" + fileId + ",version="
                    + ScmSystemUtils.getVersionStr(majorVersion, minorVersion));
        }

        // if the file belongs to a batch, the relationship needs to be detached
        // before deleting.
        String batchId = (String) currentFile.get(FieldName.FIELD_CLFILE_BATCH_ID);
        if (!StringUtils.isEmpty(batchId)) {
            throw new ScmServerException(ScmError.FILE_IN_ANOTHER_BATCH,
                    "file belongs to a batch, detach the relationship before deleting it:"
                            + "workspace=" + wsInfo.getName() + ",file=" + fileId + ",version="
                            + ScmSystemUtils.getVersionStr(majorVersion, minorVersion) + ",batch="
                            + batchId);
        }

        List<BSONObject> allVersionFile = new ArrayList<>();
        allVersionFile.add(currentFile);
        if (!isFirstVersion(currentFile)) {
            addHistoryVersionRecord(allVersionFile, currentFile);
        }

        // delete file meta
        contentServer.getMetaService().deleteFile(wsInfo, fileId);

        // delete file data
        for (BSONObject fileRecord : allVersionFile) {
            deleteData(fileRecord);
        }
        listenerMgr.postDelete(wsInfo, allVersionFile);
    }

    private void deleteData(BSONObject file) {
        ScmDataInfo dataInfo = new ScmDataInfo(file);
        BasicBSONList sites = (BasicBSONList) file.get(FieldName.FIELD_CLFILE_FILE_SITE_LIST);
        List<ScmFileLocation> siteList = new ArrayList<>();
        CommonHelper.getFileLocationList(sites, siteList);
        for (ScmFileLocation info : siteList) {
            try {
                if (info.getSiteId() == contentServer.getLocalSite()) {
                    // local
                    ScmDataDeletor deletor = ScmDataOpFactoryAssit.getFactory().createDeletor(
                            contentServer.getLocalSite(), wsInfo.getName(),
                            wsInfo.getDataLocation(), contentServer.getDataService(), dataInfo);
                    deletor.delete();
                }
                else {
                    // remote
                    ScmInnerRemoteDataDeletor rDeletor = new ScmInnerRemoteDataDeletor(
                            info.getSiteId(), wsInfo, dataInfo);
                    rDeletor.delete();
                }
            }
            catch (Exception e) {
                logger.warn("remove file data failed:siteId={},fileId={},version={}.{},dataInfo={}",
                        info.getSiteId(), fileId, file.get(FieldName.FIELD_CLFILE_MAJOR_VERSION),
                        file.get(FieldName.FIELD_CLFILE_MINOR_VERSION), dataInfo, e);
            }
        }
    }

    private void addHistoryVersionRecord(List<BSONObject> allVersionFile, BSONObject currentFile)
            throws ScmServerException {
        BSONObject matcher = new BasicBSONObject();
        matcher.put(FieldName.FIELD_CLFILE_ID, fileId);
        matcher.put(FieldName.FIELD_CLFILE_INNER_CREATE_MONTH,
                currentFile.get(FieldName.FIELD_CLFILE_INNER_CREATE_MONTH));

        MetaFileHistoryAccessor historyAccessor = contentServer.getMetaService().getMetaSource()
                .getFileHistoryAccessor(wsInfo.getMetaLocation(), wsInfo.getName(), null);
        MetaCursor cursor = null;
        try {
            cursor = historyAccessor.query(matcher, null, null);
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
