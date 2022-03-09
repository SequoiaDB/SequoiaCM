package com.sequoiacm.cloud.servicecenter.service;

import com.sequoiacm.cloud.servicecenter.exception.ScmServiceCenterException;
import com.sequoiacm.cloud.servicecenter.model.ScmInstance;

import java.util.List;

public interface InstanceService {
    void save(ScmInstance scmInstance) throws ScmServiceCenterException;

    List<ScmInstance> listInstances() throws ScmServiceCenterException;

    void deleteInstance(String ipAddr, int port, String username, String userType)
            throws ScmServiceCenterException;

    void stopInstance(String ipAddr, int port) throws ScmServiceCenterException;
}
