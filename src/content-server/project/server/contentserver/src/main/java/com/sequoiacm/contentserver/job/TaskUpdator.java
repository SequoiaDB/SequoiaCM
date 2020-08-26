package com.sequoiacm.contentserver.job;

import com.sequoiacm.exception.ScmServerException;

public interface TaskUpdator {
    public String getTaskId();
    public void doUpdate() throws ScmServerException;
}
