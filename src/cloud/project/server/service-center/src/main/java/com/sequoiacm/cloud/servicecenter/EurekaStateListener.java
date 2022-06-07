package com.sequoiacm.cloud.servicecenter;

import com.netflix.appinfo.InstanceInfo;
import com.sequoiacm.cloud.servicecenter.exception.ScmServiceCenterException;
import com.sequoiacm.cloud.servicecenter.model.ScmInstance;
import com.sequoiacm.cloud.servicecenter.service.InstanceService;
import org.bson.BasicBSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cloud.netflix.eureka.server.event.EurekaInstanceRegisteredEvent;
import org.springframework.cloud.netflix.eureka.server.event.EurekaInstanceRenewedEvent;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;

@Component
@EnableScheduling
public class EurekaStateListener {
    private Logger logger = LoggerFactory.getLogger(EurekaStateListener.class);

    private final Map<String, ScmInstance> scmInstanceCache = new HashMap<>();
    private static final int CACHE_REFRESH_INTERVAL = 1000 * 30;
    private final ReadWriteLock cacheLock = new ReentrantReadWriteLock();

    @Autowired
    private InstanceService instanceService;

    private static final String METADATA_REGION = "region";
    private static final String METADATA_ZONE = "zone";
    private static final String METADATA_MANAGEMENT_PORT = "management.port";

    @EventListener
    public void listenRegisterEvent(EurekaInstanceRegisteredEvent event) {
        InstanceInfo instanceInfo = event.getInstanceInfo();
        if (instanceInfo == null) {
            return;
        }
        if (needIgnoreInstance(instanceInfo)) {
            return;
        }
        ScmInstance scmInstance = transferToScmInstanceInfo(instanceInfo);
        if (!hasChange(scmInstance)) {
            return;
        }
        logger.debug("receive register event, save eureka instance:{}", scmInstance);
        try {
            instanceService.save(scmInstance);
        }
        catch (Exception e) {
            logger.error("failed to save eureka instance.", e);
        }

    }

    private boolean hasChange(ScmInstance scmInstance) {
        Lock lock = cacheLock.readLock();
        try {
            lock.lock();
            String ipHost = scmInstance.getIpAddr() + ":" + scmInstance.getPort();
            ScmInstance cachedInstance = scmInstanceCache.get(ipHost);
            return !scmInstance.equals(cachedInstance);
        }
        finally {
            lock.unlock();
        }
    }

    private boolean needIgnoreInstance(InstanceInfo instanceInfo) {
        // 节点下线时，也会触发Register事件，其status为DOWN，需要进行忽略
        if (instanceInfo.getStatus() != InstanceInfo.InstanceStatus.UP
                && instanceInfo.getStatus() != InstanceInfo.InstanceStatus.STARTING) {
            return true;
        }
        return false;
    }

    @EventListener
    public void listenRenewedEven(EurekaInstanceRenewedEvent event) {
        InstanceInfo instanceInfo = event.getInstanceInfo();
        if (instanceInfo == null) {
            return;
        }
        ScmInstance scmInstance = transferToScmInstanceInfo(instanceInfo);
        if (needIgnoreInstance(instanceInfo)) {
            return;
        }
        if (!hasChange(scmInstance)) {
            return;
        }
        logger.debug("receive renewed event, save eureka instance:{}", scmInstance);
        try {
            instanceService.save(scmInstance);
        }
        catch (Exception e) {
            logger.warn("failed to save eureka instance.", e);
        }

    }

    @Scheduled(initialDelay = CACHE_REFRESH_INTERVAL, fixedDelay = CACHE_REFRESH_INTERVAL)
    public void refreshCache() throws ScmServiceCenterException {
        logger.debug("refresh scmInstance cache");
        List<ScmInstance> scmInstances = instanceService.listInstances();
        Lock lock = cacheLock.writeLock();
        try {
            lock.lock();
            scmInstanceCache.clear();
            for (ScmInstance scmInstance : scmInstances) {
                String ipPort = scmInstance.getIpAddr() + ":" + scmInstance.getPort();
                scmInstanceCache.put(ipPort, scmInstance);
            }
        }
        finally {
            lock.unlock();
        }
    }

    public void evictCache(String ipAndPort) {
        Lock lock = cacheLock.writeLock();
        try {
            lock.lock();
            scmInstanceCache.remove(ipAndPort);
        }
        finally {
            lock.unlock();
        }
    }

    public void updateCache(ScmInstance scmInstance) {
        Lock lock = cacheLock.writeLock();
        try {
            lock.lock();
            scmInstanceCache.put(scmInstance.getIpAddr() + ":" + scmInstance.getPort(),
                    scmInstance);
        }
        finally {
            lock.unlock();
        }
    }

    private ScmInstance transferToScmInstanceInfo(InstanceInfo instanceInfo) {
        ScmInstance scmInstance = new ScmInstance();
        scmInstance.setIpAddr(instanceInfo.getIPAddr());
        scmInstance.setPort(instanceInfo.getPort());
        scmInstance.setHostName(instanceInfo.getHostName());
        Map<String, String> metadata = instanceInfo.getMetadata();
        scmInstance.setMetadata(new BasicBSONObject(metadata));
        scmInstance.setManualStopped(false);
        scmInstance.setServiceName(instanceInfo.getAppName().toLowerCase());
        scmInstance.setRegion(metadata.get(METADATA_REGION));
        scmInstance.setZone(metadata.get(METADATA_ZONE));
        scmInstance.setHealthCheckUrl(instanceInfo.getHealthCheckUrl());
        scmInstance.setManagementPort(Integer.parseInt(metadata.get(METADATA_MANAGEMENT_PORT)));
        return scmInstance;

    }

}
