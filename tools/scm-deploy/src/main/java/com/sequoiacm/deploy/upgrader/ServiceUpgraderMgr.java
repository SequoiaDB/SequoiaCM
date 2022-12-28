package com.sequoiacm.deploy.upgrader;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.sequoiacm.deploy.common.RefUtil;
import com.sequoiacm.deploy.module.HostInfo;
import com.sequoiacm.deploy.module.NodeStatus;
import com.sequoiacm.deploy.module.ServiceType;
import com.sequoiacm.deploy.module.StatusInfo;

public class ServiceUpgraderMgr {
    private Map<ServiceType, ServiceUpgrader> upgraderMap = new HashMap<>();

    private static volatile ServiceUpgraderMgr instance;

    public static ServiceUpgraderMgr getInstance() {
        if (instance != null) {
            return instance;
        }
        synchronized (ServiceUpgraderMgr.class) {
            if (instance != null) {
                return instance;
            }
            instance = new ServiceUpgraderMgr();
            return instance;
        }
    }

    private ServiceUpgraderMgr() {
        // 优先注册 @Rollbacker 注解修饰的 ServiceRollbacker 实现类
        List<ServiceUpgrader> upgraders = RefUtil.initInstancesAnnotatedWith(Upgrader.class);
        for (ServiceUpgrader rollbacker : upgraders) {
            upgraderMap.put(rollbacker.getType(), rollbacker);
        }

        // 缺省使用 ServiceRollbacker 实例
        for (ServiceType type : ServiceType.values()) {
            if (upgraderMap.get(type) == null) {
                upgraderMap.put(type, new ServiceUpgraderBase(type));
            }
        }
    }

    public void upgrade(StatusInfo statusInfo) throws Exception {
        ServiceUpgrader upgrader = getUpgrader(statusInfo.getType());
        upgrader.upgrade(statusInfo);
    }

    public List<NodeStatus> getNodeStatus(HostInfo host, ServiceType serviceType) throws Exception {
        ServiceUpgrader upgrader = getUpgrader(serviceType);
        return upgrader.getNodeStatus(host);
    }

    private ServiceUpgrader getUpgrader(ServiceType type) {
        ServiceUpgrader upgrader = upgraderMap.get(type);
        if (upgrader == null) {
            throw new IllegalArgumentException("no such service upgrader:" + type);
        }
        return upgrader;
    }
}