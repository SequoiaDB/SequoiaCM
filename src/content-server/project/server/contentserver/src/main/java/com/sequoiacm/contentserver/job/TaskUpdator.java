package com.sequoiacm.contentserver.job;

import com.sequoiacm.contentserver.exception.ScmServerException;

public interface TaskUpdator {
    public String getTaskId();
    public void doUpdate() throws ScmServerException;
}
