package com.sequoiacm.cloud.servicecenter.dao;

import com.sequoiacm.cloud.servicecenter.exception.ScmServiceCenterException;
import com.sequoiacm.cloud.servicecenter.exception.ScmMetasourceException;
import com.sequoiacm.cloud.servicecenter.model.ScmInstance;

import java.util.List;

public interface InstanceDao {
    void upsert(ScmInstance scmInstance) throws ScmServiceCenterException;

    List<ScmInstance> findAll() throws ScmServiceCenterException;

    void delete(String ipAddr, int port, String username, String userType)
            throws ScmServiceCenterException;

    void stopInstance(String ipAddr, int port) throws ScmServiceCenterException;
}
