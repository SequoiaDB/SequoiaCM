package com.sequoiacm.schedule.dao;

import com.sequoiacm.schedule.common.model.TransitionScheduleEntity;
import com.sequoiacm.schedule.entity.ScmBSONObjectCursor;
import org.bson.BSONObject;

public interface LifeCycleScheduleDao {
    ScmBSONObjectCursor query(BSONObject matcher) throws Exception;

    void update(BSONObject obj, Transaction t) throws Exception;

    void insert(BSONObject obj, Transaction t) throws Exception;

    TransitionScheduleEntity queryByName(String workspace, String transitionName) throws Exception;

    void delete(String id, Transaction t) throws Exception;

    void delete(BSONObject matcher, Transaction t) throws Exception;

    TransitionScheduleEntity queryOne(BSONObject matcher) throws Exception;

    ScmBSONObjectCursor query(BSONObject matcher, BSONObject orderBy) throws Exception;
}
