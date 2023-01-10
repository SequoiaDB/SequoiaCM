package com.sequoiacm.schedule.dao;

import com.sequoiacm.schedule.common.model.LifeCycleConfigFullEntity;
import com.sequoiacm.schedule.entity.ScmBSONObjectCursor;
import org.bson.BSONObject;

// todo ctrl alt h 检查每一个接口是否需要在锁内
public interface LifeCycleConfigDao {
    ScmBSONObjectCursor query(BSONObject matcher) throws Exception;

    BSONObject queryOne() throws Exception;

    void insert(LifeCycleConfigFullEntity info) throws Exception;

    void update(BSONObject obj) throws Exception;

    void update(BSONObject matcher, BSONObject updator) throws Exception;

    void update(BSONObject obj, Transaction t) throws Exception;

    void delete() throws Exception;

    void delete(Transaction t) throws Exception;

    void insert(BSONObject obj, Transaction t) throws Exception;
}
