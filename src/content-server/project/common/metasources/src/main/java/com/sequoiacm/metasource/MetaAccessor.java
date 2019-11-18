package com.sequoiacm.metasource;

import org.bson.BSONObject;

public interface MetaAccessor {
    public void insert(BSONObject insertor) throws ScmMetasourceException;
    public MetaCursor query(BSONObject matcher, BSONObject selector, BSONObject orderBy)
            throws ScmMetasourceException;
    
    public MetaCursor query(BSONObject matcher, BSONObject selector, BSONObject orderBy, long skip, long limit)
            throws ScmMetasourceException;
    
    public long count(BSONObject matcher) throws ScmMetasourceException;
    
    public double sum(BSONObject matcher, String field) throws ScmMetasourceException;

    public BSONObject queryOne(BSONObject matcher, BSONObject selector, BSONObject orderBy)
            throws ScmMetasourceException;

    //    public void delete(BSONObject deletor) throws ScmInnerException;
    //
    //    public long count(BSONObject matcher) throws ScmInnerException;
    //
    //    public void update(BSONObject matcher, BSONObject updator) throws ScmInnerException;
    //    public boolean updateAndCheck(BSONObject matcher, BSONObject updator) throws ScmInnerException;
}
