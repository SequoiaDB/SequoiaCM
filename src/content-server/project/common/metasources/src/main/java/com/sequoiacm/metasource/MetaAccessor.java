package com.sequoiacm.metasource;

import java.util.List;

import org.bson.BSONObject;

import com.sequoiacm.metasource.sequoiadb.SdbMetasourceException;

public interface MetaAccessor {
    public void insert(BSONObject insertor) throws ScmMetasourceException;

    public void insert(BSONObject insertor, int flag) throws ScmMetasourceException;

    public MetaCursor query(BSONObject matcher, BSONObject selector, BSONObject orderBy)
            throws ScmMetasourceException;

    public MetaCursor query(BSONObject matcher, BSONObject selector, BSONObject orderBy, long skip,
            long limit, int flag) throws ScmMetasourceException;

    public MetaCursor query(BSONObject matcher, BSONObject selector, BSONObject orderBy,
            BSONObject hint, long skip, long limit, int flag) throws ScmMetasourceException;

    public MetaCursor query(BSONObject matcher, BSONObject selector, BSONObject orderBy, long skip,
            long limit) throws ScmMetasourceException;

    public long count(BSONObject matcher) throws ScmMetasourceException;

    public double sum(BSONObject matcher, String field) throws ScmMetasourceException;

    public BSONObject queryOne(BSONObject matcher, BSONObject selector, BSONObject orderBy)
            throws ScmMetasourceException;

    public void delete(BSONObject deletor) throws ScmMetasourceException;

    public void delete(BSONObject deletor, BSONObject hint) throws ScmMetasourceException;

    public BSONObject queryAndDelete(BSONObject deletor) throws ScmMetasourceException;

    BSONObject queryAndUpdate(BSONObject matcher, BSONObject updator, BSONObject hint)
            throws ScmMetasourceException;

    void update(BSONObject matcher, BSONObject updator) throws ScmMetasourceException;

    void update(BSONObject matcher, BSONObject updator, BSONObject hint)
            throws ScmMetasourceException;

    // return an matching record (old|new), and update all matching records.
    BSONObject queryAndUpdate(BSONObject matcher, BSONObject updator, BSONObject hint,
            boolean returnNew) throws ScmMetasourceException;

    void ensureTable(List<String> indexFields, List<String> uniqueIndexField)
            throws ScmMetasourceException;

    void ensureTable(List<IndexDef> indexes) throws ScmMetasourceException;

    void upsert(BSONObject matcher, BSONObject updator) throws ScmMetasourceException;
    //
    // public long count(BSONObject matcher) throws ScmInnerException;
    //
    // public void update(BSONObject matcher, BSONObject updator) throws
    // ScmInnerException;
    // public boolean updateAndCheck(BSONObject matcher, BSONObject updator)
    // throws ScmInnerException;
}
