package com.sequoiacm.metasource;

import com.sequoiacm.metasource.sequoiadb.module.ScmRecyclingLog;
import org.bson.BSONObject;

public interface MetaSpaceRecyclingLogAccessor extends MetaAccessor {

    void delete(BSONObject matcher) throws ScmMetasourceException;

    ScmRecyclingLog queryOneRecyclingLog(BSONObject matcher) throws ScmMetasourceException;

    void insertRecyclingLog(ScmRecyclingLog scmRecyclingLog) throws ScmMetasourceException;
}
