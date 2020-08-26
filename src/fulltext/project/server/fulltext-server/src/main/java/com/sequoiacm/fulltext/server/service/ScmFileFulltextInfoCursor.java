package com.sequoiacm.fulltext.server.service;

import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.List;

import org.bson.BSONObject;
import org.bson.BasicBSONObject;
import org.bson.types.BasicBSONList;

import com.sequoiacm.common.CommonDefine;
import com.sequoiacm.common.FieldName;
import com.sequoiacm.content.client.ContentserverClient;
import com.sequoiacm.content.client.ScmEleCursor;
import com.sequoiacm.content.client.model.ScmFileInfo;
import com.sequoiacm.exception.ScmServerException;
import com.sequoiacm.fulltext.server.exception.FullTextException;
import com.sequoiacm.infrastructure.fulltext.common.ScmFileFulltextExtData;
import com.sequoiacm.infrastructure.fulltext.common.ScmWorkspaceFulltextExtData;
import com.sequoiacm.infrastructure.fulltext.core.ScmFileFulltextStatus;

public class ScmFileFulltextInfoCursor implements FulltextCursor {
    private static final int QUERY_HISTORY_THRESHOLD = 1000;
    private ScmFileFulltextStatus expectedStatus;
    private List<String> fileIdForQueryHistory;
    private ScmWorkspaceFulltextExtData ws;
    private ScmEleCursor<ScmFileInfo> historyFileCursor;
    private ScmFileInfo cacheByHasNext;
    private ScmEleCursor<ScmFileInfo> currentFileCursor;
    private ContentserverClient client;

    // fulltext-service
    public ScmFileFulltextInfoCursor(ContentserverClient client, ScmWorkspaceFulltextExtData ws,
            ScmFileFulltextStatus expectedStatus) throws FullTextException {
        this.ws = ws;
        this.expectedStatus = expectedStatus;
        this.fileIdForQueryHistory = new ArrayList<String>();
        this.client = client;
        BSONObject condition = conditionForQueryCurrentFile(ws, expectedStatus);
        try {
            currentFileCursor = client.listFile(ws.getWsName(), condition,
                    CommonDefine.Scope.SCOPE_CURRENT, null, 0, -1);
        }
        catch (ScmServerException e) {
            throw new FullTextException(e.getError(), "failed to query file in contentserver:ws="
                    + ws.getWsName() + ", condition=" + condition, e);
        }
    }

    static BSONObject conditionForQueryCurrentFile(ScmWorkspaceFulltextExtData ws,
            ScmFileFulltextStatus status) throws FullTextException {
        BasicBSONList andArr = new BasicBSONList();
        if (ws.getFileMatcher() != null) {
            andArr.add(ws.getFileMatcher());
        }

        BasicBSONList orArr = new BasicBSONList();
        BSONObject conditionStatus = new BasicBSONObject();
        conditionStatus.put(FieldName.FIELD_CLFILE_FILE_EXTERNAL_DATA + "."
                + ScmFileFulltextExtData.FIELD_IDX_STATUS, status.name());
        orArr.add(conditionStatus);

        BasicBSONObject conditionVersion = new BasicBSONObject(FieldName.FIELD_CLFILE_MAJOR_VERSION,
                new BasicBSONObject("$gt", 1));
        orArr.add(conditionVersion);
        BasicBSONObject orCondition = new BasicBSONObject("$or", orArr);

        andArr.add(orCondition);

        BasicBSONObject condition = new BasicBSONObject("$and", andArr);
        return condition;
    }

    public ScmFileInfo getNext() throws ScmServerException {
        if (cacheByHasNext != null) {
            ScmFileInfo tmp = cacheByHasNext;
            cacheByHasNext = null;
            return tmp;
        }
        while (true) {
            if (historyFileCursor != null && historyFileCursor.hasNext()) {
                return historyFileCursor.getNext();
            }
            if (!currentFileCursor.hasNext()) {
                if (fileIdForQueryHistory.size() > 0) {
                    initHistoryCursor();
                    fileIdForQueryHistory.clear();
                    continue;
                }
                return null;
            }
            ScmFileInfo e = currentFileCursor.getNext();
            if (e.getMajorVersion() > 1) {
                fileIdForQueryHistory.add(e.getId());
                if (fileIdForQueryHistory.size() > QUERY_HISTORY_THRESHOLD) {
                    initHistoryCursor();
                    fileIdForQueryHistory.clear();
                }
            }
            ScmFileFulltextExtData extData = new ScmFileFulltextExtData(e.getExternalData());
            if (extData.getIdxStatus() == expectedStatus) {
                return e;
            }
        }
    }

    static BSONObject conditionForQueryHisoty(List<String> fileIds, ScmFileFulltextStatus status) {
        BasicBSONList andArr = new BasicBSONList();
        BSONObject inFileId = new BasicBSONObject("$in", fileIds);
        andArr.add(new BasicBSONObject(FieldName.FIELD_CLFILE_ID, inFileId));
        andArr.add(new BasicBSONObject(FieldName.FIELD_CLFILE_FILE_EXTERNAL_DATA + "."
                + ScmFileFulltextExtData.FIELD_IDX_STATUS, status.name()));
        return new BasicBSONObject("$and", andArr);
    }

    private void initHistoryCursor() throws ScmServerException {
        historyFileCursor = client.listFile(ws.getWsName(),
                conditionForQueryHisoty(fileIdForQueryHistory, expectedStatus),
                CommonDefine.Scope.SCOPE_HISTORY, null, 0, -1);
    }

    @Override
    public boolean hasNext() throws ScmServerException {
        if (cacheByHasNext != null) {
            return true;
        }
        cacheByHasNext = getNext();
        return cacheByHasNext != null;

    }

    @Override
    public void close() {
        currentFileCursor.close();
        if (historyFileCursor != null) {
            historyFileCursor.close();
        }
    }

    @Override
    public void writeNextToWriter(PrintWriter writer) throws Exception {
        ScmFileInfo next = getNext();
        if (next == null) {
            return;
        }

        writer.write(next.getBson().toString());
    }

}
