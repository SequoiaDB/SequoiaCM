package com.sequoiacm.metasource.sequoiadb;

import org.bson.BSONObject;

import com.sequoiacm.common.FieldName;
import com.sequoiacm.metasource.MetaCursor;
import com.sequoiacm.metasource.ScmMetasourceException;

public class FileCursorWithNullSiteFilter implements MetaCursor {
    private MetaCursor fileRecCursor;

    public FileCursorWithNullSiteFilter(MetaCursor cursor) {
        this.fileRecCursor = cursor;
    }

    private BSONObject removeNullFromSiteList(BSONObject fileRecord) {
        SequoiadbHelper.removeNullElementFromList(fileRecord,
                FieldName.FIELD_CLFILE_FILE_SITE_LIST);
        return fileRecord;
    }

    @Override
    public boolean hasNext() throws ScmMetasourceException {
        return fileRecCursor.hasNext();
    }

    @Override
    public BSONObject getNext() throws ScmMetasourceException {
        BSONObject rec = fileRecCursor.getNext();
        if (rec == null) {
            return rec;
        }
        return removeNullFromSiteList(rec);
    }

    @Override
    public void close() {
        fileRecCursor.close();
    }

}
