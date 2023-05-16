package com.sequoiacm.contentserver.tag;

import com.sequoiacm.common.module.TagInfo;
import com.sequoiacm.exception.ScmServerException;

import java.util.Iterator;
import java.util.List;

public class TagInfoCursorArrayImpl implements TagInfoCursor {

    private final List<TagInfo> list;
    private final Iterator<TagInfo> it;

    public TagInfoCursorArrayImpl(List<TagInfo> list) {
        this.list = list;
        it = list.iterator();
    }

    @Override
    public TagInfo getNext() throws ScmServerException {
        return it.next();
    }

    @Override
    public boolean hasNext() throws ScmServerException {
        return it.hasNext();
    }

    @Override
    public void close() {
    }
}
