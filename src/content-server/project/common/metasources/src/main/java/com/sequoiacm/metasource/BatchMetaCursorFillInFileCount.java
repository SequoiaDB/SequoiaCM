package com.sequoiacm.metasource;

import org.bson.BSONObject;
import org.bson.types.BasicBSONList;

import com.sequoiacm.common.CommonDefine;
import com.sequoiacm.common.FieldName;

public class BatchMetaCursorFillInFileCount implements MetaCursor {

    private MetaCursor innerCursor;

    public BatchMetaCursorFillInFileCount(MetaCursor batchRecordCursor) {
        this.innerCursor = batchRecordCursor;
    }

    @Override
    public boolean hasNext() throws ScmMetasourceException {
        return innerCursor.hasNext();
    }

    @Override
    public BSONObject getNext() throws ScmMetasourceException {
        BSONObject batchRecord = innerCursor.getNext();
        if (batchRecord == null) {
            return null;
        }

        BasicBSONList files = (BasicBSONList) batchRecord.get(FieldName.Batch.FIELD_FILES);
        if (files == null) {
            batchRecord.put(CommonDefine.RestArg.BATCH_FILES_COUNT, 0);
        }
        else {
            batchRecord.put(CommonDefine.RestArg.BATCH_FILES_COUNT, files.size());
        }
        return batchRecord;
    }

    @Override
    public void close() {
        innerCursor.close();
    }

}
