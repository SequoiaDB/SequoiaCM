package com.sequoiacm.om.omserver.dao;

import java.util.List;

import com.sequoiacm.client.exception.ScmException;
import com.sequoiacm.om.omserver.exception.ScmInternalException;
import com.sequoiacm.om.omserver.exception.ScmOmServerException;
import com.sequoiacm.om.omserver.module.*;
import org.bson.BSONObject;

public interface ScmScheduleDao {

    public long getScheduleCount(BSONObject condition) throws ScmInternalException;

    public List<OmScheduleBasicInfo> getScheduleList(BSONObject condition, BSONObject orderby,
            long skip, long limit) throws ScmInternalException;

    public void createSchedule(OmScheduleInfo omScheduleInfo)
            throws ScmInternalException, ScmOmServerException;

    public OmScheduleInfo getScheduleDetail(String scheduleId) throws ScmInternalException;

    public void deleteSchedule(String scheduleId) throws ScmInternalException;

    public void updateSchedule(OmScheduleInfo omScheduleInfo) throws ScmInternalException, ScmOmServerException;

    public List<OmTaskBasicInfo> getTasks(String scheduleId, BSONObject filter, BSONObject orderBy, long skip, long limit) throws ScmInternalException;
}
