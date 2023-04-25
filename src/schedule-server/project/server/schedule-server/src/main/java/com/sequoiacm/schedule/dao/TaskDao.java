package com.sequoiacm.schedule.dao;

import org.bson.BSONObject;

import com.sequoiacm.schedule.entity.ScmBSONObjectCursor;
import com.sequoiacm.schedule.entity.TaskEntity;

public interface TaskDao {
    public void insert(TaskEntity info) throws Exception;

    public ScmBSONObjectCursor query(BSONObject matcher) throws Exception;

    public TaskEntity queryOne(String taskId) throws Exception;

    public TaskEntity queryOne(BSONObject matcher, BSONObject orderBy) throws Exception;

    public void delete(String taskId) throws Exception;

    public void update(BSONObject matcher, BSONObject updator) throws Exception;
}
