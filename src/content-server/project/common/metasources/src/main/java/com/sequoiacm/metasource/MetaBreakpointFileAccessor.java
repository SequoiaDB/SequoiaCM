package com.sequoiacm.metasource;

import org.bson.BSONObject;

public interface MetaBreakpointFileAccessor extends MetaAccessor {
    void update(BSONObject matcher, BSONObject updater) throws ScmMetasourceException;
    void delete(String fileName) throws ScmMetasourceException;
}
