package com.sequoiacm.s3.scan;

import com.sequoiacm.metasource.MetaAccessor;
import com.sequoiacm.metasource.MetaCursor;
import com.sequoiacm.metasource.ScmMetasourceException;
import org.bson.BSONObject;

public interface S3ScanRecordCursorProvider {
    RecordWrapperCursor<? extends RecordWrapper> createRecordCursor(BSONObject matcher,
            BSONObject orderby) throws ScmMetasourceException;
}

class BasicS3ScanRecordCursorProvider implements S3ScanRecordCursorProvider {
    private final MetaAccessor tableAccessor;

    public BasicS3ScanRecordCursorProvider(MetaAccessor tableAccessor) {
        this.tableAccessor = tableAccessor;
    }

    @Override
    public RecordWrapperCursor<RecordWrapper> createRecordCursor(BSONObject matcher,
            BSONObject orderby) throws ScmMetasourceException {
        MetaCursor metaCursor = tableAccessor.query(matcher, null, orderby);
        return new RecordWrapperCursor<RecordWrapper>() {
            @Override
            public boolean hasNext() throws ScmMetasourceException {
                return metaCursor.hasNext();
            }

            @Override
            public RecordWrapper getNext() throws ScmMetasourceException {
                BSONObject rec = metaCursor.getNext();
                if (rec != null) {
                    return new RecordWrapper(rec);
                }
                return null;
            }

            @Override
            public void close() {
                metaCursor.close();
            }
        };
    }
}
