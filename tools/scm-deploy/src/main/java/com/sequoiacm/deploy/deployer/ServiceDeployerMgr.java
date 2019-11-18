package com.sequoiacm.deploy.deployer;

import java.util.HashMap;
import java.util.List;

import com.sequoiacm.deploy.common.RefUtil;
import com.sequoiacm.deploy.module.ServiceType;

public class ServiceDeployerMgr {
    private HashMap<ServiceType, ServiceDeployer> deployersMap;

    private static volatile ServiceDeployerMgr instance;

    public static ServiceDeployerMgr getInstance() throws Exception {
        if (instance != null) {
            return instance;
        }
        synchronized (ServiceDeployerMgr.class) {
            if (instance != null) {
                return instance;
            }
            instance = new ServiceDeployerMgr();
            return instance;
        }
    }

    private ServiceDeployerMgr() throws Exception {
        List<ServiceDeployer> deployers = RefUtil.initInstancesAnnotatedWith(Deployer.class);
        this.deployersMap = new HashMap<>();
        for (ServiceDeployer deployer : deployers) {
            deployersMap.put(deployer.getServiceType(), deployer);
        }
    }

    public ServiceDeployer getDeployer(ServiceType type) {
        ServiceDeployer d = deployersMap.get(type);
        if (d == null) {
            throw new IllegalArgumentException("no such deployer:" + type);
        }
        return d;
    }
}
