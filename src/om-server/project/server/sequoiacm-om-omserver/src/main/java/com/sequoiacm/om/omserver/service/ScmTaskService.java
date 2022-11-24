package com.sequoiacm.om.omserver.service;

import com.sequoiacm.om.omserver.exception.ScmInternalException;
import com.sequoiacm.om.omserver.exception.ScmOmServerException;
import com.sequoiacm.om.omserver.session.ScmOmSession;
import org.bson.BSONObject;


public interface ScmTaskService {
    public long getTaskCount(ScmOmSession session, BSONObject condition)
            throws ScmOmServerException, ScmInternalException;

    public void stopTask(ScmOmSession session, String taskId)
            throws ScmOmServerException, ScmInternalException;
}
