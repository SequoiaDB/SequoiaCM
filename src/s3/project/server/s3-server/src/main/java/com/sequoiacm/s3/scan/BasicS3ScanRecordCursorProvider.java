package com.sequoiacm.s3.scan;

import com.sequoiacm.metasource.MetaAccessor;
import com.sequoiacm.metasource.MetaCursor;
import com.sequoiacm.metasource.ScmMetasourceException;
import org.bson.BSONObject;

public class BasicS3ScanRecordCursorProvider implements S3ScanRecordCursorProvider {
    private final MetaAccessor tableAccessor;
    private BSONObject hint;

    public BasicS3ScanRecordCursorProvider(MetaAccessor tableAccessor) {
        this.tableAccessor = tableAccessor;
    }

    public BasicS3ScanRecordCursorProvider(MetaAccessor tableAccessor, BSONObject hint) {
        this.tableAccessor = tableAccessor;
        this.hint = hint;
    }

    @Override
    public RecordWrapperCursor<RecordWrapper> createRecordCursor(BSONObject matcher,
            BSONObject orderby) throws ScmMetasourceException {
        MetaCursor metaCursor = tableAccessor.query(matcher, null, orderby, hint, 0, -1, 0);
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
