package com.sequoiacm.s3.scan;

import com.sequoiacm.metasource.MetaAccessor;
import com.sequoiacm.metasource.MetaCursor;
import com.sequoiacm.metasource.ScmMetasourceException;
import org.bson.BSONObject;

public interface S3ScanRecordCursorProvider {
    RecordWrapperCursor<? extends RecordWrapper> createRecordCursor(BSONObject matcher,
            BSONObject orderby) throws ScmMetasourceException;
}

