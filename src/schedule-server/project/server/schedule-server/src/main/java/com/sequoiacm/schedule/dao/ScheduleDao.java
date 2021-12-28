package com.sequoiacm.schedule.dao;

import org.bson.BSONObject;

import com.sequoiacm.schedule.common.model.ScheduleFullEntity;
import com.sequoiacm.schedule.entity.ScmBSONObjectCursor;

public interface ScheduleDao {
    public void insert(ScheduleFullEntity info) throws Exception;

    public ScmBSONObjectCursor query(BSONObject matcher) throws Exception;
    
    public ScmBSONObjectCursor query(BSONObject matcher, BSONObject orderBy, long skip, long limit)
            throws Exception;

    public ScheduleFullEntity queryOne(String scheduleId) throws Exception;
    
    public ScheduleFullEntity queryOneByName(String name) throws Exception;

    public void delete(String scheduleId) throws Exception;

    // TODO: 注意嵌套结构不能直接使用{$set:{a:{b:1}}}, 或者限定要全字段update,
    // 或者需要更新嵌套结构时必须带嵌套结构中的所有值
    public void updateByScheduleId(String scheduleId, BSONObject newValue) throws Exception;

    public void update(BSONObject matcher, BSONObject updator) throws Exception;
    
    public void delete(BSONObject matcher) throws Exception;

    long countSchedule(BSONObject condition) throws Exception;
}
