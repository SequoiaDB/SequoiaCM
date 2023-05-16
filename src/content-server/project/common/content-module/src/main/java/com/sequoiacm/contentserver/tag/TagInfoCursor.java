package com.sequoiacm.contentserver.tag;

import com.sequoiacm.common.module.TagInfo;
import com.sequoiacm.exception.ScmServerException;
import com.sequoiacm.metasource.MetaCursor;
import com.sequoiacm.metasource.ScmMetasourceException;
import org.bson.BSONObject;

public interface TagInfoCursor {

    TagInfo getNext() throws ScmServerException;

    boolean hasNext() throws ScmServerException;

    void close();
}

class TagInfoCursorMetaSourceImpl implements TagInfoCursor {

    private final MetaCursor cursor;

    public TagInfoCursorMetaSourceImpl(MetaCursor cursor) {
        this.cursor = cursor;
    }

    public TagInfo getNext() throws ScmServerException {
        try {
            BSONObject tagInfo = cursor.getNext();
            if (tagInfo == null) {
                return null;
            }
            return new TagInfo(tagInfo);
        }
        catch (ScmMetasourceException e) {
            throw new ScmServerException(e.getScmError(), "failed to get next tag", e);
        }
    }

    public boolean hasNext() throws ScmServerException {
        try {
            return cursor.hasNext();
        }
        catch (ScmMetasourceException e) {
            throw new ScmServerException(e.getScmError(), "failed to get next tag", e);
        }
    }

    public void close() {
        cursor.close();
    }
}
