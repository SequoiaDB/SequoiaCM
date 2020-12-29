package com.sequoiacm.fulltext.server.fileidx;

import org.bson.BasicBSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.sequoiacm.common.CommonDefine;
import com.sequoiacm.common.FieldName;
import com.sequoiacm.content.client.ContentserverClient;
import com.sequoiacm.content.client.ContentserverClientMgr;
import com.sequoiacm.content.client.ScmEleCursor;
import com.sequoiacm.content.client.model.ScmFileInfo;
import com.sequoiacm.exception.ScmServerException;
import com.sequoiacm.fulltext.server.es.EsClient;
import com.sequoiacm.fulltext.server.exception.FullTextException;
import com.sequoiacm.fulltext.server.sch.ScmFileUtil;
import com.sequoiacm.fulltext.server.site.ScmSiteInfoMgr;
import com.sequoiacm.infrastructure.fulltext.common.ScmFileFulltextExtData;
import com.sequoiacm.infrastructure.fulltext.core.ScmFileFulltextStatus;

class DropAndUpdateFileIdxDao extends FileIdxDao {
    private static final Logger logger = LoggerFactory.getLogger(DropAndUpdateFileIdxDao.class);
    private final ScmSiteInfoMgr siteInfoMgr;
    private final ContentserverClientMgr csClientMgr;
    private final EsClient esClient;
    private int fileCount = 1;

    DropAndUpdateFileIdxDao(String ws, String fileId, String esIndexlocation,
            ContentserverClientMgr csClientMgr, ScmSiteInfoMgr siteInfoMgr, EsClient esClient) {
        super(ws, fileId, esIndexlocation);
        this.csClientMgr = csClientMgr;
        this.esClient = esClient;
        this.siteInfoMgr = siteInfoMgr;
    }

    @Override
    public int processFileCount() {
        return fileCount;
    }

    public void process() throws FullTextException, ScmServerException {
        ContentserverClient csClient = csClientMgr.getClient(siteInfoMgr.getRootSiteName());
        ScmFileFulltextExtData newExtData = new ScmFileFulltextExtData(null,
                ScmFileFulltextStatus.NONE, null);

        ScmFileInfo latestVersionFile = csClient.getFileInfo(ws, fileId, -1, -1);
        if (latestVersionFile == null) {
            logger.debug("drop index for scm file, file not exist:ws={}, fileId={}", ws, fileId);
            fileCount = 0;
            return;
        }
        ScmFileFulltextExtData latestFileExtData = new ScmFileFulltextExtData(
                latestVersionFile.getExternalData());

        if (!ScmFileUtil.isFirstVersion(latestVersionFile)) {
            ScmEleCursor<ScmFileInfo> historyFileCursor = csClient.listFile(ws,
                    new BasicBSONObject(FieldName.FIELD_CLFILE_ID, fileId),
                    CommonDefine.Scope.SCOPE_HISTORY, null, 0, -1);
            try {
                while (historyFileCursor.hasNext()) {
                    ScmFileInfo historyFile = historyFileCursor.getNext();
                    ScmFileFulltextExtData historyExtData = new ScmFileFulltextExtData(
                            historyFile.getExternalData());
                    if (ScmFileUtil.compareFileVersion(historyFile, latestVersionFile)) {
                        // 历史文件的版本大于等于最新的文件版本, 不处理这些文件，这些文件创建的同时会自己发消息处理
                        continue;
                    }

                    fileCount++;

                    if (historyExtData.getIdxStatus() == ScmFileFulltextStatus.NONE) {
                        continue;
                    }

                    try {
                        boolean isUpdated = csClient.updateFileExternalData(ws, fileId,
                                historyFile.getMajorVersion(), historyFile.getMinorVersion(),
                                newExtData.toBson());
                        if (!isUpdated) {
                            logger.debug("file not found for drop index:ws={}, fileId={}", ws,
                                    fileId);
                            fileCount = 0;
                            return;
                        }
                    }
                    catch (Exception e) {
                        fileCount += ScmFileUtil.travelCursorSilenceForFileCount(historyFileCursor);
                        throw e;
                    }
                    unindexSilence(esIdxLocation, historyExtData.getIdxDocumentId(), historyFile);
                }
            }
            catch (Exception e) {
                try {
                    newExtData.setIdxStatus(ScmFileFulltextStatus.ERROR);
                    newExtData.setErrorMsg(e.getMessage());
                    csClient.updateFileExternalData(ws, fileId, latestVersionFile.getMajorVersion(),
                            latestVersionFile.getMinorVersion(), newExtData.toBson());
                    unindexSilence(esIdxLocation, latestFileExtData.getIdxDocumentId(),
                            latestVersionFile);
                }
                catch (Exception e1) {
                    logger.warn(
                            "failed to update latest file fulltext status to error:ws={}, fileId={}, version={}.{}",
                            ws, fileId, latestVersionFile.getMajorVersion(),
                            latestVersionFile.getMinorVersion(), e1);
                }
                throw e;
            }
            finally {
                historyFileCursor.close();
            }
        }
        csClient.updateFileExternalData(ws, fileId, latestVersionFile.getMajorVersion(),
                latestVersionFile.getMinorVersion(), newExtData.toBson());
        unindexSilence(esIdxLocation, latestFileExtData.getIdxDocumentId(), latestVersionFile);
        logger.debug("drop index for scm file:ws={}, fileId={}", ws, fileId);
    }

    private void unindexSilence(String indexLocation, String docId, ScmFileInfo file) {
        if (docId == null || docId.trim().length() == 0) {
            return;
        }
        try {
            esClient.deleteAsyncByDocId(indexLocation, docId);
        }
        catch (Exception e) {
            logger.warn(
                    "failed delete idx document for scm file:ws={}, fileId={}, version={}.{}, doumentId={}",
                    ws, fileId, file.getMajorVersion(), file.getMinorVersion(), docId, e);
        }
    }

}
