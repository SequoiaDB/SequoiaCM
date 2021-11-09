package com.sequoiacm.deploy.cleaner;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.sequoiacm.deploy.common.RefUtil;
import com.sequoiacm.deploy.module.HostInfo;
import com.sequoiacm.deploy.module.InstallPackType;

public class ServiceCleanerMgr {
    private Map<InstallPackType, ServiceCleaner> cleanerMap;

    private static volatile ServiceCleanerMgr instance;

    public static ServiceCleanerMgr getInstance() throws Exception {
        if (instance != null) {
            return instance;
        }
        synchronized (ServiceCleanerMgr.class) {
            if (instance != null) {
                return instance;
            }
            instance = new ServiceCleanerMgr();
            return instance;
        }
    }

    private ServiceCleanerMgr() throws Exception {
        List<ServiceCleaner> cleaners = RefUtil.initInstancesAnnotatedWith(Cleaner.class);
        this.cleanerMap = new HashMap<>();
        for (ServiceCleaner cleanner : cleaners) {
            cleanerMap.put(cleanner.getType(), cleanner);
        }
    }

    public void clean(HostInfo host, InstallPackType type, boolean dryRun) {
        ServiceCleaner c = getCleaner(type);
        c.clean(host, dryRun);
    }

    private ServiceCleaner getCleaner(InstallPackType type) {
        ServiceCleaner c = cleanerMap.get(type);
        if (c == null) {
            throw new IllegalArgumentException("no such service cleaner:" + type);
        }
        return c;
    }
}
