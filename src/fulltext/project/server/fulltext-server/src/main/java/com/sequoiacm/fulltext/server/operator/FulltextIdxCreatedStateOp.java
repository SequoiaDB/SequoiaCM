package com.sequoiacm.fulltext.server.operator;

import java.util.UUID;

import org.bson.BSONObject;
import org.bson.BasicBSONObject;
import org.bson.types.BasicBSONList;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import com.sequoiacm.common.CommonDefine;
import com.sequoiacm.common.FieldName;
import com.sequoiacm.content.client.ContentserverClient;
import com.sequoiacm.content.client.ContentserverClientMgr;
import com.sequoiacm.content.client.ScmEleCursor;
import com.sequoiacm.content.client.model.ScmFileInfo;
import com.sequoiacm.exception.ScmError;
import com.sequoiacm.exception.ScmServerException;
import com.sequoiacm.fulltext.server.WsFulltextExtDataModifier;
import com.sequoiacm.fulltext.server.exception.FullTextException;
import com.sequoiacm.fulltext.server.parser.TextualParserMgr;
import com.sequoiacm.fulltext.server.sch.FulltextIdxSchJobData;
import com.sequoiacm.fulltext.server.sch.FulltextIdxSchJobType;
import com.sequoiacm.fulltext.server.sch.createidx.IdxCreateDao;
import com.sequoiacm.fulltext.server.site.ScmSiteInfoMgr;
import com.sequoiacm.infrastructure.common.IOUtils;
import com.sequoiacm.infrastructure.fulltext.common.FulltextCommonDefine;
import com.sequoiacm.infrastructure.fulltext.common.ScmWorkspaceFulltextExtData;
import com.sequoiacm.infrastructure.fulltext.core.ScmFulltextMode;
import com.sequoiacm.infrastructure.fulltext.core.ScmFulltextStatus;
import com.sequoiacm.schedule.common.ScheduleDefine.ScopeType;

@Component
public class FulltextIdxCreatedStateOp extends FulltextIdxOperator {
    private static final Logger logger = LoggerFactory.getLogger(FulltextIdxCreatedStateOp.class);

    @Autowired
    private ScmSiteInfoMgr siteMgr;
    @Autowired
    private ContentserverClientMgr csMgr;
    @Autowired
    private TextualParserMgr textualParserMgr;

    @Override
    public void createIndex(ScmWorkspaceFulltextExtData currentWsFulltextExtData,
            BSONObject fileMatcher, ScmFulltextMode mode) throws FullTextException {
        throw new FullTextException(ScmError.FULL_TEXT_INDEX_ALREADY_CREATED,
                "index already created:ws=" + currentWsFulltextExtData.getWsName());
    }

    @Override
    public void dropIndex(ScmWorkspaceFulltextExtData currentWsFulltextExtData)
            throws FullTextException {
        String wsName = currentWsFulltextExtData.getWsName();
        String idxLocation = currentWsFulltextExtData.getIndexDataLocation();

        changeToDeletingAndCreateSch(wsName, idxLocation);
    }

    @Override
    public void updateIndex(ScmWorkspaceFulltextExtData currentWsFulltextExtData,
            BSONObject newFileMatcher, ScmFulltextMode newMode) throws FullTextException {
        if (newFileMatcher == null && newMode == null) {
            return;
        }

        if (newFileMatcher == null) {
            updateWsFulltextMode(currentWsFulltextExtData, newMode);
            return;
        }

        if (newFileMatcher.equals(currentWsFulltextExtData.getFileMatcher())) {
            updateWsFulltextMode(currentWsFulltextExtData, newMode);
            return;
        }
        updateAndBuildIndex(currentWsFulltextExtData, newFileMatcher, newMode);
    }

    protected void updateWsFulltextMode(ScmWorkspaceFulltextExtData currentWsFulltextExtData,
            ScmFulltextMode newMode) throws FullTextException {
        if (newMode == null) {
            return;
        }
        if (newMode.equals(currentWsFulltextExtData.getMode())) {
            return;
        }
        WsFulltextExtDataModifier modifier = new WsFulltextExtDataModifier(
                currentWsFulltextExtData.getWsName());
        modifier.setMode(newMode);
        confClient.updateWsExternalData(modifier);
    }

    protected void updateAndBuildIndex(ScmWorkspaceFulltextExtData currentWsFulltextExtData,
            BSONObject newFileMatcher, ScmFulltextMode newMode) throws FullTextException {
        String schName = FulltextCommonDefine.FULLTEXT_SCHEDULE_PREFIX
                + currentWsFulltextExtData.getWsName() + "-"
                + FulltextIdxSchJobType.FULLTEXT_INDEX_UPDATE + "-" + UUID.randomUUID().toString();

        WsFulltextExtDataModifier modifier = new WsFulltextExtDataModifier(
                currentWsFulltextExtData.getWsName());
        modifier.setEnabled(true);
        modifier.setIndexStatus(ScmFulltextStatus.CREATING);
        modifier.setFileMatcher(newFileMatcher);
        if (newMode != null) {
            modifier.setMode(newMode);
        }
        modifier.setFulltextJobName(schName);
        confClient.updateWsExternalData(modifier);

        long latestMsgId = getLatestMsgId(currentWsFulltextExtData.getWsName());

        FulltextIdxSchJobData fulltextSch = new FulltextIdxSchJobData();
        fulltextSch.setFileMatcher(newFileMatcher);
        fulltextSch.setIndexDataLocation(currentWsFulltextExtData.getIndexDataLocation());
        fulltextSch.setWs(currentWsFulltextExtData.getWsName());
        fulltextSch.setLatestMsgId(latestMsgId);

        schClient.createFulltextSch(schName, FulltextIdxSchJobType.FULLTEXT_INDEX_UPDATE,
                fulltextSch);
    }

    @Override
    public ScmFulltextStatus operatorForStatus() {
        return ScmFulltextStatus.CREATED;
    }

    @Override
    public void inspectIndex(ScmWorkspaceFulltextExtData fulltextData) throws FullTextException {
        updateAndBuildIndex(fulltextData, fulltextData.getFileMatcher(), fulltextData.getMode());
    }

    @Override
    public void rebuildIndex(ScmWorkspaceFulltextExtData currentWsFulltextExtData, String fileId)
            throws FullTextException {
        tryGetFileWithFulltextMatcher(currentWsFulltextExtData, fileId);
        IdxCreateDao creator = IdxCreateDao.newBuilder(esClient, csMgr, textualParserMgr, siteMgr)
                .file(currentWsFulltextExtData.getWsName(), fileId).reindex(true)
                .indexLocation(currentWsFulltextExtData.getIndexDataLocation()).syncIndexInEs(true)
                .get();
        try {
            creator.createIdx();
        }
        catch (FullTextException e) {
            throw e;
        }
        catch (Exception e) {
            throw new FullTextException(ScmError.SYSTEM_ERROR,
                    "failed to create fulltext index for file:ws="
                            + currentWsFulltextExtData.getWsName() + ", fileId=" + fileId
                            + ", cause by:" + e.getMessage(),
                    e);
        }
    }

    private ScmFileInfo tryGetFileWithFulltextMatcher(
            ScmWorkspaceFulltextExtData currentWsFulltextExtData, String fileId)
            throws FullTextException {
        BasicBSONList andArr = new BasicBSONList();
        andArr.add(currentWsFulltextExtData.getFileMatcher());
        andArr.add(new BasicBSONObject(FieldName.FIELD_CLFILE_ID, fileId));
        BasicBSONObject matcher = new BasicBSONObject("$and", andArr);
        ContentserverClient csClient = csMgr.getClient(siteMgr.getRootSiteName());

        ScmEleCursor<ScmFileInfo> cursor = null;
        try {
            cursor = csClient.listFile(currentWsFulltextExtData.getWsName(), matcher,
                    CommonDefine.Scope.SCOPE_CURRENT, null, 0, 1);
            if (!cursor.hasNext()) {
                ScmFileInfo fileInfo = csClient.getFileInfo(currentWsFulltextExtData.getWsName(),
                        fileId, -1, -1);
                if (fileInfo == null) {
                    throw new FullTextException(ScmError.FILE_NOT_FOUND, "file not found:ws="
                            + currentWsFulltextExtData.getWsName() + ", fileId=" + fileId);
                }
                throw new FullTextException(ScmError.FILE_NOT_MEET_WORKSPACE_INDEX_MATCHER,
                        "file not meet workspace index matcher:ws="
                                + currentWsFulltextExtData.getWsName() + ", wsFulltextIdxMatcher="
                                + currentWsFulltextExtData.getFileMatcher() + ", fileId=" + fileId);
            }
            ScmFileInfo file = cursor.getNext();
            return file;
        }
        catch (ScmServerException e) {
            throw new FullTextException(e.getError(),
                    "failed to get file info from content-server:ws="
                            + currentWsFulltextExtData.getWsName() + ", fileId=" + fileId,
                    e);
        }
        finally {
            IOUtils.close(cursor);
        }
    }
}
