package com.sequoiacm.om.omserver.dao.impl;

import com.sequoiacm.client.core.*;
import com.sequoiacm.client.element.ScmId;
import com.sequoiacm.client.exception.ScmException;
import com.sequoiacm.om.omserver.dao.ScmTaskDao;
import com.sequoiacm.om.omserver.exception.ScmInternalException;
import com.sequoiacm.om.omserver.session.ScmOmSession;
import org.bson.BSONObject;


public class ScmTaskDaoImpl implements ScmTaskDao {

    private ScmOmSession session;

    public ScmTaskDaoImpl(ScmOmSession session) {
        this.session = session;
    }

    @Override
    public long getTaskCount(BSONObject filter) throws ScmInternalException {
        ScmSession conn = session.getConnection();
        try {
            return ScmSystem.Task.count(conn, filter);
        }
        catch (ScmException e) {
            throw new ScmInternalException(e.getError(),
                    "failed to get task count, " + e.getMessage(), e);
        }
    }


    @Override
    public void stopTask(String taskId) throws ScmInternalException {
        ScmSession conn = session.getConnection();
        try {
            ScmSystem.Task.stopTask(conn, new ScmId(taskId));
        }
        catch (ScmException e) {
            throw new ScmInternalException(e.getError(), "failed to stop task, " + e.getMessage(),
                    e);
        }
    }

}
