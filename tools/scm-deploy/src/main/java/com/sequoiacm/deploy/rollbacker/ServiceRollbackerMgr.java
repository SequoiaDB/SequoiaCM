package com.sequoiacm.deploy.rollbacker;

import com.sequoiacm.deploy.common.RefUtil;
import com.sequoiacm.deploy.module.ServiceType;
import com.sequoiacm.deploy.module.StatusInfo;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class ServiceRollbackerMgr {
    private Map<ServiceType, ServiceRollbacker> rollbackerMap = new HashMap<>();

    private static volatile ServiceRollbackerMgr instance;

    public static ServiceRollbackerMgr getInstance() {
        if (instance != null) {
            return instance;
        }
        synchronized (ServiceRollbackerMgr.class) {
            if (instance != null) {
                return instance;
            }
            instance = new ServiceRollbackerMgr();
            return instance;
        }
    }

    private ServiceRollbackerMgr() {
        // 优先注册 @Rollbacker 注解修饰的 ServiceRollbacker 实现类
        List<ServiceRollbacker> rollbackers = RefUtil.initInstancesAnnotatedWith(Rollbacker.class);
        for (ServiceRollbacker rollbacker : rollbackers) {
            rollbackerMap.put(rollbacker.getType(), rollbacker);
        }

        // 缺省使用 ServiceRollbacker 实例
        for (ServiceType type : ServiceType.values()) {
            if (rollbackerMap.get(type) == null) {
                rollbackerMap.put(type, new ServiceRollbackerBase(type));
            }
        }
    }

    public void rollback(StatusInfo statusInfo) throws Exception {
        ServiceRollbacker rollbacker = getRollbacker(statusInfo.getType());
        rollbacker.rollback(statusInfo);
    }

    private ServiceRollbacker getRollbacker(ServiceType type){
        ServiceRollbacker rollbacker = rollbackerMap.get(type);
        if (rollbacker == null) {
            throw new IllegalArgumentException("no such service rollbacker:" + type);
        }
        return rollbacker;
    }

}
