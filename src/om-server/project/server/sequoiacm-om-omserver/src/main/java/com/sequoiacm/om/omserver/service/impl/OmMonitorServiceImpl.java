package com.sequoiacm.om.omserver.service.impl;

import com.sequoiacm.infrastructure.common.timer.ScmTimer;
import com.sequoiacm.infrastructure.common.timer.ScmTimerFactory;
import com.sequoiacm.infrastructure.common.timer.ScmTimerTask;
import com.sequoiacm.om.omserver.common.InstanceStatus;
import com.sequoiacm.om.omserver.config.ScmOmServerConfig;
import com.sequoiacm.om.omserver.dao.OmMonitorDao;
import com.sequoiacm.om.omserver.exception.ScmInternalException;
import com.sequoiacm.om.omserver.exception.ScmOmServerError;
import com.sequoiacm.om.omserver.exception.ScmOmServerException;
import com.sequoiacm.om.omserver.module.monitor.*;
import com.sequoiacm.om.omserver.service.OmMonitorService;
import com.sequoiacm.om.omserver.session.ScmOmSession;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;
import java.util.*;
import java.util.concurrent.*;

@Component
public class OmMonitorServiceImpl implements OmMonitorService {

    private Logger logger = LoggerFactory.getLogger(OmMonitorServiceImpl.class);
    private static final int BATCH_SIZE = 10;

    private static ThreadPoolExecutor executors = null;

    static {
        executors = new ThreadPoolExecutor(5, 5, 20, TimeUnit.SECONDS,
                new ArrayBlockingQueue<Runnable>(20), Executors.defaultThreadFactory(),
                new ThreadPoolExecutor.CallerRunsPolicy());
    }

    private static final String URL_PREFIX = "http://";

    @Autowired
    private ScmOmServerConfig serverConfig;

    @Autowired
    private OmMonitorDao omMonitorDao;

    @Value("${info.app.version}")
    private String version;

    private ScmTimer serviceCenterUrlsRefreshTimer;
    private ScmTimer instancesRefreshTimer;
    private ScmTimer instancesHealthCheckTimer;

    private final Map<String, OmMonitorInstanceInfo> instanceInfoCache = new ConcurrentHashMap<>();

    private final List<String> serviceCenterUrlsCache = new CopyOnWriteArrayList<>();

    @PostConstruct
    public void init() throws Exception {
        refreshServiceCenterUrls();
        refreshInstances();
        checkInstancesHealth();
        startTask();
    }

    private void startTask() {
        logger.info("start service center urls refresh task.");
        serviceCenterUrlsRefreshTimer = ScmTimerFactory.createScmTimer();
        long interval = serverConfig.getServiceCenterUrlsUpdateInterval() * 1000L;
        serviceCenterUrlsRefreshTimer.schedule(new ScmTimerTask() {
            @Override
            public void run() {
                refreshServiceCenterUrls();
            }
        }, interval, interval);

        logger.info("start instances refresh task.");
        instancesRefreshTimer = ScmTimerFactory.createScmTimer();
        interval = serverConfig.getInstanceUpdateInterval() * 1000L;
        instancesRefreshTimer.schedule(new ScmTimerTask() {
            @Override
            public void run() {
                refreshInstances();
            }
        }, interval, interval);

        logger.info("start instances health check task.");
        instancesHealthCheckTimer = ScmTimerFactory.createScmTimer();
        interval = serverConfig.getHealthCheckInterval() * 1000L;
        instancesHealthCheckTimer.schedule(new ScmTimerTask() {
            @Override
            public void run() {
                checkInstancesHealth();
            }
        }, interval, interval);

    }

    private void refreshServiceCenterUrls() {
        try {
            List<String> urls = omMonitorDao.getServiceCenterUrls();
            serviceCenterUrlsCache.clear();
            serviceCenterUrlsCache.addAll(urls);
        }
        catch (Exception e) {
            logger.warn("failed to refresh serviceCenterUrls.", e);
        }
    }

    private void refreshInstances() {
        try {
            if (serviceCenterUrlsCache.isEmpty()) {
                return;
            }
            List<OmMonitorInstanceBasicInfo> list = omMonitorDao
                    .getInstanceList(serviceCenterUrlsCache);
            updateInstanceCache(list);
        }
        catch (Exception e) {
            logger.warn(e.getMessage(), e);
        }
    }

    private void updateInstanceCache(List<OmMonitorInstanceBasicInfo> basicInfoList) {
        Set<String> prepareDeleteKeys = new HashSet<>(instanceInfoCache.keySet());
        for (OmMonitorInstanceBasicInfo basicInfo : basicInfoList) {
            String instanceId = getInstanceId(basicInfo);
            prepareDeleteKeys.remove(instanceId);
            OmMonitorInstanceInfo oldInstanceInfo = instanceInfoCache.get(instanceId);
            OmMonitorInstanceInfo newInstance = transferToOmMonitorInstanceInfo(basicInfo);
            if (oldInstanceInfo == null) {
                instanceInfoCache.put(instanceId, newInstance);
            }
            else {
                newInstance.setStatus(oldInstanceInfo.getStatus());
                instanceInfoCache.put(instanceId, newInstance);
            }
        }
        for (String instanceId : prepareDeleteKeys) {
            instanceInfoCache.remove(instanceId);
        }
    }

    private void checkInstancesHealth() {
        List<OmMonitorInstanceInfo> instances = new ArrayList<>(instanceInfoCache.values());
        int batchCount = (int) Math.ceil(instances.size() / (float) BATCH_SIZE);
        List<Future<?>> futureList = new ArrayList<>();
        for (int i = 0; i < batchCount; i++) {
            int start = i * BATCH_SIZE;
            int currentBatchSize = Math.min(BATCH_SIZE, instances.size() - start);
            final List<OmMonitorInstanceInfo> instanceBatch = new ArrayList<>(currentBatchSize);
            for (int j = 0; j < currentBatchSize; j++) {
                instanceBatch.add(instances.get(start + j));
            }
            Future<?> future = executors.submit(new Runnable() {
                @Override
                public void run() {
                    checkInstancesHealth(instanceBatch);
                }
            });
            futureList.add(future);
        }

        for (Future<?> ft : futureList) {
            try {
                ft.get();
            }
            catch (InterruptedException | ExecutionException e) {
                logger.error("execution error", e);
            }
        }

    }

    private void checkInstancesHealth(List<OmMonitorInstanceInfo> instances) {
        for (OmMonitorInstanceInfo instance : instances) {
            InstanceStatus status = omMonitorDao.getInstanceStatus(instance);
            instance.setStatus(status.getStatusText());
        }
    }

    public Map<String, Object> getVersionInfo() {
        Map<String, Object> result = new HashMap<>();
        result.put("version", version);
        return result;
    }

    public OmMonitorInstanceInfo checkAndGetInstance(String instanceId)
            throws ScmOmServerException {
        OmMonitorInstanceInfo instanceInfo = instanceInfoCache.get(instanceId);
        if (instanceInfo != null) {
            return instanceInfo;
        }
        throw new ScmOmServerException(ScmOmServerError.INVALID_ARGUMENT,
                "instance is not exist:" + instanceId);
    }

    public OmHeapInfo getHeapInfo(String instanceId) throws ScmOmServerException {
        OmMonitorInstanceInfo instanceInfo = checkAndGetInstance(instanceId);
        return omMonitorDao.getHeapInfo(instanceInfo.getManagementUrl());
    }

    public OmConnectionInfo getConnectionInfo(String instanceId) throws ScmOmServerException {
        OmMonitorInstanceInfo instanceInfo = checkAndGetInstance(instanceId);
        return omMonitorDao.getConnectionInfo(instanceInfo.getManagementUrl());
    }

    public OmThreadInfo getThreadInfo(String instanceId) throws ScmOmServerException {
        OmMonitorInstanceInfo instanceInfo = checkAndGetInstance(instanceId);
        return omMonitorDao.getThreadInfo(instanceInfo.getManagementUrl());
    }

    public OmProcessInfo getProcessInfo(String instanceId) throws ScmOmServerException {
        OmMonitorInstanceInfo instanceInfo = checkAndGetInstance(instanceId);
        return omMonitorDao.getProcessInfo(instanceInfo.getManagementUrl());
    }

    public Map<String, String> getConfigInfo(String instanceId) throws ScmOmServerException {
        OmMonitorInstanceInfo instanceInfo = checkAndGetInstance(instanceId);
        return omMonitorDao.getConfigInfo(instanceInfo.getManagementUrl());
    }

    @Override
    public void deleteInstance(ScmOmSession session, String instanceId)
            throws ScmOmServerException, ScmInternalException {
        OmMonitorInstanceInfo instanceInfo = checkAndGetInstance(instanceId);
        if (instanceInfo.getStatus().equals(InstanceStatus.UP.getStatusText())) {
            throw new ScmOmServerException(ScmOmServerError.UNSUPPORT_OPERATION,
                    "instance with status 'UP' cannot be removed");
        }
        omMonitorDao.deleteInstances(serviceCenterUrlsCache, instanceInfo.getIpAddr(),
                instanceInfo.getPort(), session);
        instanceInfoCache.remove(instanceId);
    }

    public String getInstanceId(OmMonitorInstanceBasicInfo instance) {
        return instance.getIpAddr() + ":" + instance.getPort();
    }

    public List<OmMonitorInstanceInfo> listInstances() throws ScmOmServerException {
        List<OmMonitorInstanceInfo> list = new ArrayList<>(instanceInfoCache.values());
        Collections.sort(list, new Comparator<OmMonitorInstanceInfo>() {
            @Override
            public int compare(OmMonitorInstanceInfo o1, OmMonitorInstanceInfo o2) {
                return o1.getServiceName().compareTo(o2.getServiceName());
            }
        });
        return list;
    }

    private OmMonitorInstanceInfo transferToOmMonitorInstanceInfo(
            OmMonitorInstanceBasicInfo instance) {
        String instanceId = getInstanceId(instance);
        String statusText = InstanceStatus.UP.getStatusText();
        String managementUrl = URL_PREFIX + instance.getIpAddr() + ":"
                + instance.getManagementPort();
        return new OmMonitorInstanceInfo(instanceId, statusText, managementUrl, instance);
    }

}
