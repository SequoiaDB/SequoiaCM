package com.sequoiacm.fulltext.server.sch.createidx;

import java.io.InputStream;
import java.util.List;
import java.util.Random;

import org.bson.BasicBSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.sequoiacm.common.CommonDefine.Scope;
import com.sequoiacm.common.FieldName;
import com.sequoiacm.content.client.ContentserverClient;
import com.sequoiacm.content.client.ContentserverClientMgr;
import com.sequoiacm.content.client.ScmEleCursor;
import com.sequoiacm.content.client.model.ScmFileInfo;
import com.sequoiacm.exception.ScmError;
import com.sequoiacm.exception.ScmServerException;
import com.sequoiacm.fulltext.server.es.EsClient;
import com.sequoiacm.fulltext.server.es.EsDocument;
import com.sequoiacm.fulltext.server.exception.FullTextException;
import com.sequoiacm.fulltext.server.parser.TextualParser;
import com.sequoiacm.fulltext.server.parser.TextualParserMgr;
import com.sequoiacm.fulltext.server.sch.ScmFileUtil;
import com.sequoiacm.fulltext.server.site.ScmSiteInfo;
import com.sequoiacm.fulltext.server.site.ScmSiteInfoMgr;
import com.sequoiacm.infrastructure.common.IOUtils;
import com.sequoiacm.infrastructure.fulltext.common.ScmFileFulltextExtData;
import com.sequoiacm.infrastructure.fulltext.core.ScmFileFulltextStatus;

public class IdxCreateDao {
    private static final Logger logger = LoggerFactory.getLogger(IdxCreateDao.class);
    private ContentserverClientMgr csMgr;
    private TextualParserMgr textualParserMgr;
    private EsClient esClient;
    private ScmSiteInfoMgr siteInfoMgr;
    private String indexLocation;
    private String wsName;
    private Random random = new Random();

    private String fileId;
    private boolean syncIndex;
    private int fileCount = 1;
    public boolean reindex = false;

    private IdxCreateDao(EsClient esClient, ContentserverClientMgr csMgr,
            TextualParserMgr textualParserMgr, ScmSiteInfoMgr siteInfoMgr) {
        this.esClient = esClient;
        this.csMgr = csMgr;
        this.textualParserMgr = textualParserMgr;
        this.siteInfoMgr = siteInfoMgr;
    }

    private void updateFileExtDataSilence(ContentserverClient csClient, ScmFileInfo file,
            ScmFileFulltextExtData extData) {
        try {
            csClient.updateFileExternalData(wsName, file.getId(), file.getMajorVersion(),
                    file.getMinorVersion(), extData.toBson());
        }
        catch (Exception e) {
            logger.warn(
                    "failed update ext data for scm file:ws={}, fileId={}, version={}.{}, extData={}",
                    wsName, file.getId(), file.getMajorVersion(), file.getMinorVersion(), extData,
                    e);
        }
    }

    private void unindexSlience(ScmFileInfo file, String docId) {
        if (docId == null || docId.trim().length() == 0) {
            return;
        }
        try {
            esClient.deleteAsyncByDocId(indexLocation, docId);
        }
        catch (Exception e) {
            logger.warn(
                    "failed delete document for scm file:ws={}, fileId={}, version={}.{}, doumentId={}",
                    wsName, file.getId(), file.getMajorVersion(), file.getMinorVersion(), docId, e);
        }
    }

    public void createIdx() throws FullTextException, ScmServerException {
        ContentserverClient rootSiteCsClient = csMgr.getClient(siteInfoMgr.getRootSiteName());
        ScmFileInfo latestVersionFile = rootSiteCsClient.getFileInfo(wsName, fileId, -1, -1);
        if (latestVersionFile == null) {
            logger.debug("file not found for create index:ws={}, fileId={}", wsName, fileId);
            fileCount = 0;
            return;
        }
        if (!ScmFileUtil.isFirstVersion(latestVersionFile)) {
            ScmEleCursor<ScmFileInfo> historyFileCursor = rootSiteCsClient.listFile(wsName,
                    new BasicBSONObject(FieldName.FIELD_CLFILE_ID, fileId), Scope.SCOPE_HISTORY,
                    null, 0, -1);
            try {
                while (historyFileCursor.hasNext()) {
                    ScmFileInfo historyFile = historyFileCursor.getNext();

                    if (ScmFileUtil.compareFileVersion(historyFile, latestVersionFile)) {
                        // 历史文件的版本大于等于最新的文件版本, 不处理这些文件，这些文件创建的同时会自己发消息处理
                        continue;
                    }

                    fileCount++;

                    try {
                        boolean isCreateSuccess = createIdxForOneFile(rootSiteCsClient,
                                historyFile);
                        if (!isCreateSuccess) {
                            logger.debug("file not found for create index:ws={}, fileId={}", wsName,
                                    fileId);
                            fileCount = 0;
                            return;
                        }
                    }
                    catch (Exception e) {
                        fileCount += ScmFileUtil.travelCursorSilenceForFileCount(historyFileCursor);
                        throw e;
                    }
                }
            }
            catch (Exception e) {
                onException("failed to create index for the history version", rootSiteCsClient,
                        null, latestVersionFile);
                throw e;
            }
            finally {
                historyFileCursor.close();
            }
        }

        createIdxForOneFile(rootSiteCsClient, latestVersionFile);
        logger.debug("create index for file success:ws={}, fileId={}, latestVesion={}.{}", wsName,
                latestVersionFile.getId(), latestVersionFile.getMajorVersion(),
                latestVersionFile.getMinorVersion());
    }

    // 创建成功返回 true， 文件不存在返回false，其它抛异常
    private boolean createIdxForOneFile(ContentserverClient rootSiteCsClient, ScmFileInfo file)
            throws FullTextException, ScmServerException {
        ScmFileFulltextExtData fileExtdata = new ScmFileFulltextExtData(file.getExternalData());
        if (fileExtdata.getIdxStatus() == ScmFileFulltextStatus.CREATED && !reindex) {
            return true;
        }

        String oldEsDocId = fileExtdata.getIdxDocumentId();

        String esDocId = null;
        ContentserverClient csClient = null;
        InputStream fileData = null;
        try {
            String site = readFromSite(file.getSites());
            if (site.equals(siteInfoMgr.getRootSiteName())) {
                csClient = rootSiteCsClient;
            }
            else {
                csClient = csMgr.getClient(site);
            }
            TextualParser parser = textualParserMgr.getParser(file.getMimeType());
            if (parser.fileSizeLimit() < file.getFileSize()) {
                throw new FullTextException(ScmError.OPERATION_UNSUPPORTED,
                        "can not parse file, file is too large:limit=" + parser.fileSizeLimit()
                                + ", fileSize=" + file.getFileSize());
            }

            fileData = csClient.download(wsName, file.getId(), file.getMajorVersion(),
                    file.getMinorVersion());
            String text = parser.parse(fileData);

            EsDocument document = new EsDocument();
            document.setContent(text);
            document.setFileId(file.getId());
            document.setFileVersion(file.getMajorVersion() + "." + file.getMinorVersion());
            esDocId = esClient.index(indexLocation, document, syncIndex);

            ScmFileFulltextExtData extData = new ScmFileFulltextExtData(esDocId,
                    ScmFileFulltextStatus.CREATED, null);

            boolean isUpdated = csClient.updateFileExternalData(wsName, fileId,
                    file.getMajorVersion(), file.getMinorVersion(), extData.toBson());
            if (!isUpdated) {
                logger.debug("file not found:ws={}, fileId={}, version={}.{}", wsName, fileId,
                        file.getMajorVersion(), file.getMinorVersion());
                unindexSlience(file, esDocId);
                return false;
            }
            logger.debug("create index for scm file:ws={}, fileId={}, version={}.{}", wsName,
                    file.getId(), file.getMajorVersion(), file.getMinorVersion());
            unindexSlience(file, oldEsDocId);
            return true;
        }
        catch (ScmServerException e) {
            if (e.getError() == ScmError.FILE_NOT_FOUND) {
                logger.debug("file not found:ws={}, fileId={}, version={}.{}", wsName, fileId,
                        file.getMajorVersion(), file.getMinorVersion());
                return false;
            }
            onException(e.getMessage(), rootSiteCsClient, esDocId, file);
            throw e;
        }
        catch (Exception e) {
            onException(e.getMessage(), rootSiteCsClient, esDocId, file);
            throw e;
        }
        finally {
            IOUtils.close(fileData);
        }
    }

    private void onException(String errorMsg, ContentserverClient csClient, String esDocId,
            ScmFileInfo file) {
        logger.warn("failed create index for scm file:ws={}, fileId={}, version={}.{}, causeby={}",
                wsName, file.getId(), file.getMajorVersion(), file.getMinorVersion(), errorMsg);
        unindexSlience(file, esDocId);
        if (csClient != null) {
            ScmFileFulltextExtData extData = new ScmFileFulltextExtData(null,
                    ScmFileFulltextStatus.ERROR, "index failed, cause by:" + errorMsg);
            updateFileExtDataSilence(csClient, file, extData);
        }
    }

    private String readFromSite(List<Integer> fileSites) throws FullTextException {
        ScmSiteInfo rootSite = siteInfoMgr.getRootSite();
        if (fileSites.contains(rootSite.getSiteId())) {
            return rootSite.getName();
        }
        return siteInfoMgr.getSiteNameById(fileSites.get(random.nextInt(fileSites.size())));
    }

    public static Builder newBuilder(EsClient esClient, ContentserverClientMgr csMgr,
            TextualParserMgr textualParserMgr, ScmSiteInfoMgr siteInfoMgr) {
        return new Builder(esClient, csMgr, textualParserMgr, siteInfoMgr);
    }

    public int fileCount() {
        return fileCount;
    }

    public String getFileId() {
        return fileId;
    }

    public String getWsName() {
        return wsName;
    }

    public static class Builder {
        private IdxCreateDao creator;

        private Builder(EsClient esClient, ContentserverClientMgr csMgr,
                TextualParserMgr textualParserMgr, ScmSiteInfoMgr siteInfoMgr) {
            creator = new IdxCreateDao(esClient, csMgr, textualParserMgr, siteInfoMgr);
        }

        public Builder file(String wsName, String fileId) {
            creator.wsName = wsName;
            creator.fileId = fileId;
            return this;
        }

        public Builder syncIndexInEs(boolean syncIndexInEs) {
            creator.syncIndex = syncIndexInEs;
            return this;
        }

        public Builder indexLocation(String indexLocation) {
            creator.indexLocation = indexLocation;
            return this;
        }

        public Builder reindex(boolean reindex) {
            creator.reindex = reindex;
            return this;
        }

        public IdxCreateDao get() {
            return creator;
        }
    }

}
