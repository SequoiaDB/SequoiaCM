package com.sequoiacm.schedule.dao;

import org.bson.BSONObject;

import com.sequoiacm.schedule.entity.ScheduleFullEntity;
import com.sequoiacm.schedule.entity.ScmBSONObjectCursor;

public interface ScheduleDao {
    public void insert(ScheduleFullEntity info) throws Exception;

    public ScmBSONObjectCursor query(BSONObject matcher) throws Exception;

    public ScheduleFullEntity queryOne(String scheduleId) throws Exception;

    public void delete(String scheduleId) throws Exception;

    // TODO: 注意嵌套结构不能直接使用{$set:{a:{b:1}}}, 或者限定要全字段update,
    // 或者需要更新嵌套结构时必须带嵌套结构中的所有值
    public void update(String scheduleId, BSONObject newValue) throws Exception;

    public void delete(BSONObject matcher) throws Exception;
}
