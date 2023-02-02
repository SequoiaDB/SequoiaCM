package com.sequoiacm.om.omserver.service.impl;

import com.sequoiacm.client.core.*;
import com.sequoiacm.client.exception.ScmException;
import com.sequoiacm.infrastructrue.security.core.ScmRole;
import com.sequoiacm.om.omserver.core.ScmSiteChooser;
import com.sequoiacm.om.omserver.dao.ScmScheduleDao;
import com.sequoiacm.om.omserver.exception.ScmInternalException;
import com.sequoiacm.om.omserver.exception.ScmOmServerException;
import com.sequoiacm.om.omserver.factory.ScmScheduleDaoFactory;
import com.sequoiacm.om.omserver.factory.ScmUserDaoFactory;
import com.sequoiacm.om.omserver.module.OmScheduleBasicInfo;
import com.sequoiacm.om.omserver.module.OmScheduleInfo;
import com.sequoiacm.om.omserver.module.OmTaskBasicInfo;
import com.sequoiacm.om.omserver.module.OmUserInfo;
import com.sequoiacm.om.omserver.service.ScmScheduleService;
import com.sequoiacm.om.omserver.session.ScmOmSession;
import org.bson.BSONObject;
import org.bson.BasicBSONObject;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class ScmScheduleServiceImpl implements ScmScheduleService {
    @Autowired
    private ScmSiteChooser siteChooser;

    @Autowired
    private ScmScheduleDaoFactory scmScheduleDaoFactory;

    @Autowired
    private ScmUserDaoFactory scmUserDaoFactory;

    @Override
    public long getScheduleCount(ScmOmSession session, BSONObject condition)
            throws ScmInternalException {
        ScmScheduleDao scheduleDao = scmScheduleDaoFactory.createScheduleDao(session);
        condition = filterByUser(session, condition);
        return scheduleDao.getScheduleCount(condition);
    }

    @Override
    public List<OmScheduleBasicInfo> getScheduleList(ScmOmSession session, BSONObject condition,
            BSONObject orderBy, long skip, long limit) throws ScmInternalException {
        ScmScheduleDao scheduleDao = scmScheduleDaoFactory.createScheduleDao(session);
        condition = filterByUser(session, condition);
        return scheduleDao.getScheduleList(condition, orderBy, skip, limit);
    }

    @Override
    public void createSchedule(ScmOmSession session, OmScheduleInfo omScheduleInfo)
            throws ScmOmServerException, ScmInternalException {
        ScmScheduleDao scheduleDao = scmScheduleDaoFactory.createScheduleDao(session);
        scheduleDao.createSchedule(omScheduleInfo);
    }

    @Override
    public OmScheduleInfo getScheduleDetail(ScmOmSession session, String scheduleId)
            throws ScmInternalException {
        ScmScheduleDao scheduleDao = scmScheduleDaoFactory.createScheduleDao(session);
        return scheduleDao.getScheduleDetail(scheduleId);
    }

    @Override
    public void deleteSchedule(ScmOmSession session, String scheduleId)
            throws ScmOmServerException, ScmInternalException {
        ScmScheduleDao scheduleDao = scmScheduleDaoFactory.createScheduleDao(session);
        scheduleDao.deleteSchedule(scheduleId);
    }

    @Override
    public void updateSchedule(ScmOmSession session, OmScheduleInfo omScheduleInfo)
            throws ScmOmServerException, ScmInternalException {
        ScmScheduleDao scheduleDao = scmScheduleDaoFactory.createScheduleDao(session);
        scheduleDao.updateSchedule(omScheduleInfo);
    }

    @Override
    public List<OmTaskBasicInfo> getScheduleTasks(ScmOmSession session, String scheduleId,
            BSONObject filter, BSONObject orderBy, long skip, long limit)
            throws ScmOmServerException, ScmInternalException {
        String preferSite = siteChooser.getRootSite();
        ScmScheduleDao scheduleDao = scmScheduleDaoFactory.createScheduleDao(session);
        try {
            session.resetServiceEndpoint(preferSite);
            return scheduleDao.getTasks(scheduleId, filter, orderBy, skip, limit);
        }
        catch (ScmInternalException e) {
            siteChooser.onException(e);
            throw e;
        }
    }

    private BSONObject filterByUser(ScmOmSession session, BSONObject condition)
            throws ScmInternalException {
        String username = session.getUser();
        OmUserInfo user = scmUserDaoFactory.createUserDao(session).getUser(username);
        if (user.hasRole(ScmRole.AUTH_ADMIN_ROLE_NAME)) {
            return condition;
        }
        BSONObject filterByCreateUser = new BasicBSONObject(ScmAttributeName.Schedule.CREATE_USER,
                username);
        BSONObject realCondition = null;
        try {
            realCondition = ScmQueryBuilder.start().and(filterByCreateUser).and(condition).get();
        }
        catch (ScmException e) {
            throw new ScmInternalException(e.getError(), e.getMessage(), e);
        }
        return realCondition;
    }
}
