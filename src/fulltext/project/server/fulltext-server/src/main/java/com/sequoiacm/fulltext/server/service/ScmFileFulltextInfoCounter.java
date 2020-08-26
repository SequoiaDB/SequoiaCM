package com.sequoiacm.fulltext.server.service;

import java.util.ArrayList;
import java.util.List;

import org.bson.BSONObject;

import com.sequoiacm.common.CommonDefine;
import com.sequoiacm.content.client.ContentserverClient;
import com.sequoiacm.content.client.ScmEleCursor;
import com.sequoiacm.content.client.model.ScmFileInfo;
import com.sequoiacm.exception.ScmServerException;
import com.sequoiacm.fulltext.server.exception.FullTextException;
import com.sequoiacm.infrastructure.fulltext.common.ScmFileFulltextExtData;
import com.sequoiacm.infrastructure.fulltext.common.ScmWorkspaceFulltextExtData;
import com.sequoiacm.infrastructure.fulltext.core.ScmFileFulltextStatus;

public class ScmFileFulltextInfoCounter {
    private static final int QUERY_HISTORY_THRESHOLD = 100;
    private ScmFileFulltextStatus expectedStatus;
    private List<String> fileIdForQueryHitory;
    private ScmWorkspaceFulltextExtData ws;
    private ScmEleCursor<ScmFileInfo> currentFileCursor;
    private ContentserverClient client;

    ScmFileFulltextInfoCounter(ContentserverClient client, ScmWorkspaceFulltextExtData ws,
            ScmFileFulltextStatus expectedStatus) throws FullTextException {
        this.ws = ws;
        this.client = client;
        this.expectedStatus = expectedStatus;
        this.fileIdForQueryHitory = new ArrayList<String>();
        BSONObject condition = ScmFileFulltextInfoCursor.conditionForQueryCurrentFile(ws,
                expectedStatus);
        try {
            currentFileCursor = client.listFile(ws.getWsName(), condition,
                    CommonDefine.Scope.SCOPE_CURRENT, null, 0, -1);
        }
        catch (ScmServerException e) {
            throw new FullTextException(e.getError(), "failed to query file in contentserver:ws="
                    + ws.getWsName() + ", condition=" + condition, e);
        }
    }

    public long count() throws ScmServerException {
        long count = 0;
        while (true) {
            if (!currentFileCursor.hasNext()) {
                if (fileIdForQueryHitory.size() > 0) {
                    count += countInHistory();
                }
                return count;
            }
            ScmFileInfo e = currentFileCursor.getNext();
            if (e.getMajorVersion() > 1) {
                fileIdForQueryHitory.add(e.getId());
                if (fileIdForQueryHitory.size() > QUERY_HISTORY_THRESHOLD) {
                    count += countInHistory();
                    fileIdForQueryHitory.clear();
                }
            }
            ScmFileFulltextExtData fileExtData = new ScmFileFulltextExtData(e.getExternalData());
            if (fileExtData.getIdxStatus() == expectedStatus) {
                count++;
            }
        }
    }

    private long countInHistory() throws ScmServerException {
        BSONObject condition = ScmFileFulltextInfoCursor
                .conditionForQueryHisoty(fileIdForQueryHitory, expectedStatus);
        return client.countFile(ws.getWsName(), CommonDefine.Scope.SCOPE_HISTORY, condition);
    }

    public void close() {
        currentFileCursor.close();
    }
}