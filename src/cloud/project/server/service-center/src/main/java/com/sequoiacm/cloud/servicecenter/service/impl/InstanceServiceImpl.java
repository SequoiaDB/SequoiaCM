package com.sequoiacm.cloud.servicecenter.service.impl;

import com.sequoiacm.cloud.servicecenter.EurekaStateListener;
import com.sequoiacm.cloud.servicecenter.dao.InstanceDao;
import com.sequoiacm.cloud.servicecenter.exception.ScmServiceCenterException;
import com.sequoiacm.cloud.servicecenter.model.ScmInstance;
import com.sequoiacm.cloud.servicecenter.service.InstanceService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class InstanceServiceImpl implements InstanceService {

    @Autowired
    private InstanceDao instanceDao;

    @Autowired
    private EurekaStateListener eurekaStateListener;

    @Override
    public void save(ScmInstance scmInstance) throws ScmServiceCenterException {
        instanceDao.upsert(scmInstance);
        eurekaStateListener.updateCache(scmInstance);
    }

    @Override
    public List<ScmInstance> listInstances() throws ScmServiceCenterException {
        return instanceDao.findAll();
    }

    @Override
    public void deleteInstance(String ipAddr, int port, String username, String userType)
            throws ScmServiceCenterException {
        instanceDao.delete(ipAddr, port, username, userType);
        eurekaStateListener.evictCache(ipAddr + ":" + port);
    }

    @Override
    public void stopInstance(String ipAddr, int port) throws ScmServiceCenterException {
        instanceDao.stopInstance(ipAddr, port);
        eurekaStateListener.evictCache(ipAddr + ":" + port);
    }
}
