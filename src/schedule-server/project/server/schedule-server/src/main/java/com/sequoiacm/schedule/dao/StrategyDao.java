package com.sequoiacm.schedule.dao;

import org.bson.BSONObject;

import com.sequoiacm.schedule.entity.ScmBSONObjectCursor;

public interface StrategyDao {
    public ScmBSONObjectCursor query(BSONObject matcher) throws Exception;
}
