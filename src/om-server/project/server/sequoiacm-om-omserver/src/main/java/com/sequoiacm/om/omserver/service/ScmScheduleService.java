package com.sequoiacm.om.omserver.service;

import com.sequoiacm.om.omserver.exception.ScmInternalException;
import com.sequoiacm.om.omserver.exception.ScmOmServerException;
import com.sequoiacm.om.omserver.module.OmScheduleBasicInfo;
import com.sequoiacm.om.omserver.module.OmScheduleInfo;
import com.sequoiacm.om.omserver.module.OmTaskBasicInfo;
import com.sequoiacm.om.omserver.session.ScmOmSession;
import org.bson.BSONObject;

import java.util.List;

public interface ScmScheduleService {
    public long getScheduleCount(ScmOmSession session, BSONObject condition)
            throws ScmInternalException, ScmOmServerException;

    public List<OmScheduleBasicInfo> getScheduleList(ScmOmSession session, BSONObject condition,
            BSONObject orderBy, long skip, long limit)
            throws ScmInternalException, ScmOmServerException;

    public void createSchedule(ScmOmSession session, OmScheduleInfo omScheduleInfo)
            throws ScmOmServerException, ScmInternalException;

    public OmScheduleInfo getScheduleDetail(ScmOmSession session, String scheduleId)
            throws ScmOmServerException, ScmInternalException;

    public void deleteSchedule(ScmOmSession session, String scheduleId)
            throws ScmOmServerException, ScmInternalException;

    public void updateSchedule(ScmOmSession session, OmScheduleInfo omScheduleInfo)
            throws ScmOmServerException, ScmInternalException;

    public List<OmTaskBasicInfo> getScheduleTasks(ScmOmSession session, String scheduleId,
            BSONObject filter, BSONObject orderBy, long skip, long limit)
            throws ScmOmServerException, ScmInternalException;
}
