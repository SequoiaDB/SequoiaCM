package com.sequoiacm.om.omserver.service.impl;

import com.sequoiacm.om.omserver.core.ScmSiteChooser;
import com.sequoiacm.om.omserver.dao.ScmTaskDao;
import com.sequoiacm.om.omserver.exception.ScmInternalException;
import com.sequoiacm.om.omserver.exception.ScmOmServerException;
import com.sequoiacm.om.omserver.factory.ScmTaskDaoFactory;
import com.sequoiacm.om.omserver.service.ScmTaskService;
import com.sequoiacm.om.omserver.session.ScmOmSession;
import org.bson.BSONObject;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class ScmTaskServiceImpl implements ScmTaskService {
    @Autowired
    private ScmSiteChooser siteChooser;
    @Autowired
    private ScmTaskDaoFactory scmTaskDaoFactory;

    @Override
    public long getTaskCount(ScmOmSession session, BSONObject filter)
            throws ScmOmServerException, ScmInternalException {
        String preferSite = siteChooser.chooseFromAllSite();
        ScmTaskDao taskDao = scmTaskDaoFactory.createTaskDao(session);
        try {
            session.resetServiceEndpoint(preferSite);
            return taskDao.getTaskCount(filter);
        }
        catch (ScmInternalException e) {
            siteChooser.onException(e);
            throw e;
        }
    }

    @Override
    public void stopTask(ScmOmSession session, String taskId)
            throws ScmOmServerException, ScmInternalException {
        String rootSite = siteChooser.getRootSite();
        ScmTaskDao taskDao = scmTaskDaoFactory.createTaskDao(session);
        try {
            session.resetServiceEndpoint(rootSite);
            taskDao.stopTask(taskId);
        }
        catch (ScmInternalException e) {
            siteChooser.onException(e);
            throw e;
        }
    }

}
