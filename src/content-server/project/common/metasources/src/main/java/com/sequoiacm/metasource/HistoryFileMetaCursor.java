package com.sequoiacm.metasource;

import org.bson.BSONObject;
import org.bson.BasicBSONObject;

import com.sequoiacm.common.FieldName;
import com.sequoiacm.infrastructure.common.ScmIdParser;

public class HistoryFileMetaCursor implements MetaCursor {
    private MetaCursor historyFileCursor;
    private MetaFileAccessor currentFileAccessor;
    private BSONObject selector;
    private BSONObject currentFileRec;
    private BSONObject historyFileRec;

    //TODO:selector do not have nested fields, like {'a.b':1}!
    public HistoryFileMetaCursor(MetaFileAccessor currentFileAccessor,
            MetaFileHistoryAccessor historyFileAccessor, BSONObject matcher, BSONObject selector, BSONObject orderby, long skip, long limit)
            throws ScmMetasourceException {
        this.selector = selector;
        this.currentFileAccessor = currentFileAccessor;
        if(orderby == null) {
            orderby = new BasicBSONObject();
            // orderby id, reduce query current cl.
            orderby.put(FieldName.FIELD_CLFILE_ID, 1);
        }

        // do not select in historyCL
        this.historyFileCursor = historyFileAccessor.query(matcher, null, orderby, skip, limit);
    }

    @Override
    public boolean hasNext() throws ScmMetasourceException {
        if (historyFileRec != null) {
            // currentFileRec is not null in here!
            // currentFileRec match historyFileRec in here!
            return true;
        }

        while (historyFileCursor.hasNext()) {
            // get a history record
            historyFileRec = historyFileCursor.getNext();
            // already have a matching currentRec, return true
            if (currentFileRec != null
                    && currentFileRec.get(FieldName.FIELD_CLFILE_ID).equals(
                            historyFileRec.get(FieldName.FIELD_CLFILE_ID))) {
                return true;
            }
            else {
                // get a matching currentRec
                BSONObject currentFileMatcher = createCurrentFileMatcher(historyFileRec);
                currentFileRec = queryOne(currentFileAccessor, currentFileMatcher);
                if (currentFileRec == null) {
                    // no matching currentFileRec, the file is deleting,
                    // get next history record
                    continue;
                }
                // have a matching currentFileRec, return true
                return true;
            }
        }
        // no more history record, return false, HistoryFileMetaCursor is end
        historyFileRec = null;
        currentFileRec = null;
        return false;
    }

    @Override
    public BSONObject getNext() throws ScmMetasourceException {
        if (hasNext()) {
            BSONObject completeHistoryRec = new BasicBSONObject();
            completeHistoryRec.putAll(currentFileRec);
            BSONObject selectedHistoryRec = filterHistoryRecBySeletor(historyFileRec);
            completeHistoryRec.putAll(selectedHistoryRec);
            historyFileRec = null;
            return completeHistoryRec;
        }
        else {
            return null;
        }
    }

    private BSONObject createCurrentFileMatcher(BSONObject historyRec)
            throws ScmMetasourceException {
        // {id:fileId,create_month:"createMonth"}
        BasicBSONObject currentFileMatcher = new BasicBSONObject();
        String id = (String) historyRec.get(FieldName.FIELD_CLFILE_ID);
        ScmIdParser idParser;
        try {
            idParser = new ScmIdParser(id);
        }
        catch (Exception e) {
            throw new ScmMetasourceException(e.getMessage(), e);
        }
        currentFileMatcher.put(FieldName.FIELD_CLFILE_INNER_CREATE_MONTH, idParser.getMonth());
        currentFileMatcher.put(FieldName.FIELD_CLFILE_ID, id);
        return currentFileMatcher;
    }

    @Override
    public void close() {
        historyFileRec = null;
        currentFileRec = null;
        if (historyFileCursor != null) {
            historyFileCursor.close();
        }
    }

    private BSONObject queryOne(MetaAccessor accessor, BSONObject matcher)
            throws ScmMetasourceException {
        MetaCursor cursor = null;
        try {
            cursor = accessor.query(matcher, selector, null);
            if (cursor.hasNext()) {
                return cursor.getNext();
            }
            else {
                return null;
            }
        }
        finally {
            if (cursor != null) {
                cursor.close();
            }
        }
    }

    private BSONObject filterHistoryRecBySeletor(BSONObject historyRec) {
        if (selector == null) {
            return historyRec;
        }

        BSONObject selectedRec = new BasicBSONObject();
        for (String selectorKey : selector.keySet()) {
            if (historyRec.containsField(selectorKey)) {
                selectedRec.put(selectorKey, historyRec.get(selectorKey));
            }
        }

        return selectedRec;
    }
}
