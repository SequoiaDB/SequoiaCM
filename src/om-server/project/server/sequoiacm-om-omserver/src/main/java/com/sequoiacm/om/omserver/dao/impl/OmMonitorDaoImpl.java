package com.sequoiacm.om.omserver.dao.impl;

import com.sequoiacm.client.core.ScmSystem;
import com.sequoiacm.client.element.ScmServiceInstance;
import com.sequoiacm.client.exception.ScmException;
import com.sequoiacm.infrastructure.feign.ScmFeignException;
import com.sequoiacm.om.omserver.common.InstanceStatus;
import com.sequoiacm.om.omserver.dao.OmMonitorDao;
import com.sequoiacm.om.omserver.factory.ScmServiceCenterClientFactory;
import com.sequoiacm.om.omserver.remote.OmMonitorClient;
import com.sequoiacm.om.omserver.exception.ScmInternalException;
import com.sequoiacm.om.omserver.exception.ScmOmServerError;
import com.sequoiacm.om.omserver.exception.ScmOmServerException;
import com.sequoiacm.om.omserver.factory.OmMonitorClientFactory;
import com.sequoiacm.om.omserver.module.monitor.*;
import com.sequoiacm.om.omserver.remote.ScmServiceCenterFeignClient;
import com.sequoiacm.om.omserver.session.ScmOmSession;
import com.sequoiacm.om.omserver.session.ScmOmSessionFactory;
import feign.RetryableException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Repository;
import org.springframework.web.client.RestTemplate;

import java.util.*;

@Repository
public class OmMonitorDaoImpl implements OmMonitorDao {

    @Autowired
    private RestTemplate restTemplate;

    @Autowired
    private ScmOmSessionFactory sessionFactory;

    @Autowired
    private ScmServiceCenterClientFactory scmServiceCenterClientFactory;

    @Autowired
    private OmMonitorClientFactory monitorClientFactory;

    private static final String URL_PREFIX = "http://";
    public static final String SERVICE_CENTER_NAME = "service-center";

    private Logger logger = LoggerFactory.getLogger(OmMonitorDaoImpl.class);

    @Override
    public List<OmMonitorInstanceBasicInfo> getInstanceList(List<String> serviceCenterUrls)
            throws ScmOmServerException {
        for (String serviceCenterUrl : serviceCenterUrls) {
            try {
                return scmServiceCenterClientFactory.getClient(serviceCenterUrl).getInstanceList();
            }
            catch (Exception e) {
                logger.warn("failed to fetch instances from serviceCenterUrl:{}", serviceCenterUrl,
                        e);
            }
        }
        throw new ScmOmServerException(ScmOmServerError.SYSTEM_ERROR,
                "failed to get instance list, no service center available");
    }

    @Override
    public OmHeapInfo getHeapInfo(OmMonitorInstanceInfo instanceInfo, ScmOmSession session)
            throws ScmOmServerException {
        OmMonitorClient client = monitorClientFactory.getClient(instanceInfo, session);
        Map<String, Object> heapInfo = null;
        try {
            heapInfo = client.getHeapInfo();
        }
        catch (RetryableException e) {
            throw new ScmOmServerException(ScmOmServerError.SYSTEM_ERROR,
                    "failed to get heap info, node is unavailable, url:"
                            + instanceInfo.getManagementUrl(),
                    e);
        }
        catch (ScmFeignException e) {
            throw new ScmOmServerException(ScmOmServerError.SYSTEM_ERROR,
                    "failed to get heap info," + e.getMessage(), e);
        }
        long max = Long.parseLong(String.valueOf(heapInfo.get("heap")));
        long size = Long.parseLong(String.valueOf(heapInfo.get("heap.committed")));
        long used = Long.parseLong(String.valueOf(heapInfo.get("heap.used")));
        OmHeapInfo omHeapInfo = new OmHeapInfo();
        omHeapInfo.setMax(max);
        omHeapInfo.setSize(size);
        omHeapInfo.setUsed(used);
        return omHeapInfo;
    }

    @Override
    public OmConnectionInfo getConnectionInfo(OmMonitorInstanceInfo instanceInfo,
            ScmOmSession session) throws ScmOmServerException {
        OmMonitorClient client = monitorClientFactory.getClient(instanceInfo, session);
        try {
            return client.getConnectionInfo();
        }
        catch (RetryableException e) {
            throw new ScmOmServerException(ScmOmServerError.SYSTEM_ERROR,
                    "failed to get connection info, node is unavailable, url:"
                            + instanceInfo.getManagementUrl(),
                    e);
        }
        catch (ScmFeignException e) {
            throw new ScmOmServerException(ScmOmServerError.SYSTEM_ERROR,
                    "failed to get connection info," + e.getMessage(), e);
        }
    }

    @Override
    public OmThreadInfo getThreadInfo(OmMonitorInstanceInfo instanceInfo, ScmOmSession session)
            throws ScmOmServerException {
        OmMonitorClient client = monitorClientFactory.getClient(instanceInfo, session);
        try {
            return client.getThreadInfo();
        }
        catch (RetryableException e) {
            throw new ScmOmServerException(ScmOmServerError.SYSTEM_ERROR,
                    "failed to get thread info, node is unavailable, url:"
                            + instanceInfo.getManagementUrl(),
                    e);
        }
        catch (ScmFeignException e) {
            throw new ScmOmServerException(ScmOmServerError.SYSTEM_ERROR,
                    "failed to get thread info," + e.getMessage(), e);
        }
    }

    @Override
    public OmProcessInfo getProcessInfo(OmMonitorInstanceInfo instanceInfo, ScmOmSession session)
            throws ScmOmServerException {
        OmMonitorClient client = monitorClientFactory.getClient(instanceInfo, session);
        try {
            return client.getProcessInfo();
        }
        catch (RetryableException e) {
            throw new ScmOmServerException(ScmOmServerError.SYSTEM_ERROR,
                    "failed to get process info, node is unavailable, url:"
                            + instanceInfo.getManagementUrl(),
                    e);
        }
        catch (ScmFeignException e) {
            throw new ScmOmServerException(ScmOmServerError.SYSTEM_ERROR,
                    "failed to get process info," + e.getMessage(), e);
        }
    }

    @Override
    public Map<String, String> getConfigInfo(OmMonitorInstanceInfo instanceInfo,
            ScmOmSession session) throws ScmOmServerException {
        OmMonitorClient client = monitorClientFactory.getClient(instanceInfo, session);
        Map<?, ?> environmentInfo = null;
        try {
            environmentInfo = client.getEnvironmentInfo();
        }
        catch (RetryableException e) {
            throw new ScmOmServerException(ScmOmServerError.SYSTEM_ERROR,
                    "failed to get config info, node is unavailable, url:"
                            + instanceInfo.getManagementUrl(),
                    e);
        }
        catch (ScmFeignException e) {
            throw new ScmOmServerException(ScmOmServerError.SYSTEM_ERROR,
                    "failed to get config info," + e.getMessage(), e);
        }
        Map<?, ?> userConfig = findUserConfig(environmentInfo);
        Map<?, ?> classPathConfig = findClassPathConfig(environmentInfo);
        Map<String, String> configMap = new TreeMap<>();
        for (Map.Entry<?, ?> entry : classPathConfig.entrySet()) {
            configMap.put(String.valueOf(entry.getKey()), String.valueOf(entry.getValue()));
        }
        for (Map.Entry<?, ?> entry : userConfig.entrySet()) {
            configMap.put(String.valueOf(entry.getKey()), String.valueOf(entry.getValue()));
        }
        return configMap;
    }

    @Override
    public void deleteInstances(List<String> serviceCenterUrls, String ipAddr, int port,
            ScmOmSession session) throws ScmOmServerException {
        for (String serviceCenterUrl : serviceCenterUrls) {
            try {
                ScmServiceCenterFeignClient client = scmServiceCenterClientFactory
                        .getClient(serviceCenterUrl);
                client.deleteInstance(ipAddr, port, session.getSessionId());
                return;
            }
            catch (RetryableException e) {
                logger.warn("failed to delete instances from serviceCenterUrl :{}",
                        serviceCenterUrl, e);
            }
            catch (ScmFeignException e) {
                throw new ScmOmServerException(ScmOmServerError.SYSTEM_ERROR, e.getMessage(), e);
            }
        }
        throw new ScmOmServerException(ScmOmServerError.SYSTEM_ERROR,
                "failed to delete instance, no service center available");
    }

    @Override
    public List<String> getServiceCenterUrls() throws ScmInternalException {
        List<String> urls = new ArrayList<>();
        try {
            ScmOmSession session = sessionFactory.createSession();
            List<ScmServiceInstance> serviceInstanceList = ScmSystem.ServiceCenter
                    .getServiceInstanceList(session.getConnection(), SERVICE_CENTER_NAME);

            for (ScmServiceInstance instance : serviceInstanceList) {
                urls.add(URL_PREFIX + instance.getIp() + ":" + instance.getPort());
            }
        }
        catch (ScmException e) {
            throw new ScmInternalException(e.getError(), e.getMessage(), e);
        }
        return urls;

    }

    @Override
    public InstanceStatus getInstanceStatus(OmMonitorInstanceBasicInfo instance) {
        InstanceStatus status = InstanceStatus.UP;
        if (instance.isManualStopped()) {
            status = InstanceStatus.STOPPED;
        }
        else {
            try {
                // HealthCheckUrl 是不固定的，这里使用 restTemplate 发请求
                Map<?, ?> result = restTemplate.getForObject(instance.getHealthCheckUrl()
                        .replace(instance.getHostName(), instance.getIpAddr()), Map.class);
                if (!"UP".equalsIgnoreCase(String.valueOf(result.get("status")))) {
                    status = InstanceStatus.DOWN;
                }
            }
            catch (Exception e) {
                status = InstanceStatus.DOWN;
            }
        }
        if (status == InstanceStatus.DOWN) {
            logger.warn("instance is unhealthy:{}", instance);
        }
        return status;
    }

    private Map<?, ?> findClassPathConfig(Map<?, ?> config) {
        for (Map.Entry<?, ?> entry : config.entrySet()) {
            if (String.valueOf(entry.getKey()).contains("classpath:/")) {
                return (Map<?, ?>) entry.getValue();
            }
        }
        return new HashMap<>();
    }

    private Map<?, ?> findUserConfig(Map<?, ?> config) {
        for (Map.Entry<?, ?> entry : config.entrySet()) {
            if (String.valueOf(entry.getKey()).contains("file:/")) {
                return (Map<?, ?>) entry.getValue();
            }
        }
        return new HashMap<>();
    }

}
