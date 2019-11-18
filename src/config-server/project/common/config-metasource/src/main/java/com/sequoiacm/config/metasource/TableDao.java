package com.sequoiacm.config.metasource;

import org.bson.BSONObject;

import com.sequoiacm.config.metasource.exception.MetasourceException;

public interface TableDao {
    public void delete(BSONObject matcher) throws MetasourceException;

    // delete and return old
    public BSONObject deleteAndCheck(BSONObject matcher) throws MetasourceException;

    public void insert(BSONObject record) throws MetasourceException;

    public void update(BSONObject matcher, BSONObject updator) throws MetasourceException;

    // update and return new
    public BSONObject updateAndCheck(BSONObject matcher, BSONObject updator)
            throws MetasourceException;

    public MetaCursor query(BSONObject matcher, BSONObject seletor, BSONObject orderBy)
            throws MetasourceException;

    public BSONObject queryOne(BSONObject matcher, BSONObject seletor, BSONObject orderBy)
            throws MetasourceException;

    public long count(BSONObject matcher) throws MetasourceException;

    public int generateId() throws MetasourceException;

}
