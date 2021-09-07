package com.sequoiacm.om.omserver.dao;

import com.sequoiacm.om.omserver.exception.ScmInternalException;
import org.bson.BSONObject;


public interface ScmTaskDao {
    public long getTaskCount(BSONObject filter) throws ScmInternalException;

    public void stopTask(String taskId) throws ScmInternalException;
}
