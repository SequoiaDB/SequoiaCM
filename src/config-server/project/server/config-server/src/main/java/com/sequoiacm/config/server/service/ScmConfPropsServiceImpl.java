package com.sequoiacm.config.server.service;

import java.net.InetAddress;
import java.util.*;
import java.util.Map.Entry;
import java.util.concurrent.Future;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cloud.client.ServiceInstance;
import org.springframework.cloud.client.discovery.DiscoveryClient;
import org.springframework.stereotype.Service;

import com.sequoiacm.config.framework.lock.ScmLockManager;
import com.sequoiacm.config.framework.lock.ScmLockPathFactory;
import com.sequoiacm.config.server.common.ScmTargetType;
import com.sequoiacm.config.server.module.ScmUpdateConfPropsResult;
import com.sequoiacm.config.server.module.ScmUpdateConfPropsResultSet;
import com.sequoiacm.infrastructure.config.core.exception.ScmConfError;
import com.sequoiacm.infrastructure.config.core.exception.ScmConfigException;
import com.sequoiacm.infrastructure.lock.ScmLock;

@Service
public class ScmConfPropsServiceImpl implements ScmConfPropsService {
    private static final Logger logger = LoggerFactory.getLogger(ScmConfPropsServiceImpl.class);
    @Autowired
    private DiscoveryClient discoveryClient;

    @Autowired
    private AsyncUpdateConfPropsExecutor asyncExecutor;

    @Override
    public ScmUpdateConfPropsResultSet updateConfProps(ScmTargetType type, List<String> targets,
            Map<String, String> updateProps, List<String> deleteProps, boolean acceptUnknownProps)
            throws ScmConfigException {
        logger.info(
                "update conf props: type={}, targets={}, updateProps={}, deleteProps={}, acceptUnknownProps={}",
                type, targets, updateProps, deleteProps, acceptUnknownProps);
        ScmLock lock = ScmLockManager.getInstance()
                .acquiresLock(ScmLockPathFactory.createGlobalConfigPropLockPath());
        try {
            // serviceName map instances
            Map<String, List<ServiceInstance>> instancesMap = getInstancesByType(type, targets);
            for (Map.Entry<String, List<ServiceInstance>> e : instancesMap.entrySet()) {
                logger.info("service instances: serviceName={}, instance={}", e.getKey(),
                        instanceToString(e.getValue()));
            }

            // serviceName map instances exec result future
            Map<String, Future<List<ScmUpdateConfPropsResult>>> futures = new HashMap<>();

            // concurrently refresh conf in every services.
            for (Entry<String, List<ServiceInstance>> entry : instancesMap.entrySet()) {
                // serially refresh conf the instances of the same service.
                Future<List<ScmUpdateConfPropsResult>> future = asyncExecutor.updateConfProp(
                        entry.getValue(), updateProps, deleteProps, acceptUnknownProps);
                futures.put(entry.getKey(), future);
            }

            ScmUpdateConfPropsResultSet resultSet = new ScmUpdateConfPropsResultSet();

            for (Entry<String, Future<List<ScmUpdateConfPropsResult>>> entry : futures.entrySet()) {
                Future<List<ScmUpdateConfPropsResult>> future = entry.getValue();
                try {
                    resultSet.addResults(future.get());
                }
                catch (Exception e) {
                    String serviceName = entry.getKey();
                    List<ServiceInstance> failedInstances = instancesMap.get(serviceName);
                    for (ServiceInstance failedInstance : failedInstances) {
                        logger.warn("failed to get execution result:serviceName={},url={}:{}",
                                failedInstance.getServiceId(), failedInstance.getHost(),
                                failedInstance.getPort());
                        resultSet.addResult(
                                new ScmUpdateConfPropsResult(failedInstance.getServiceId(),
                                        failedInstance.getHost() + ":" + failedInstance.getPort(),
                                        "failed to wait for execution result:" + e.getMessage()));
                    }
                    logger.warn("failed to get execution result", e);
                }
            }
            logger.info("update conf result:{}", resultSet);
            return resultSet;
        }
        finally {
            lock.unlock();
        }
    }

    private String instanceToString(List<ServiceInstance> value) {
        StringBuilder sb = new StringBuilder();
        for (ServiceInstance i : value) {
            sb.append(i.getHost()).append(":").append(i.getPort()).append(",");
        }
        if (sb.length() > 0) {
            return sb.substring(0, sb.length() - 1);
        }
        return "";
    }

    private Map<String, List<ServiceInstance>> getInstancesByType(ScmTargetType type,
            List<String> targets) throws ScmConfigException {
        // Map<ServiceName, ServiceInstanceList>
        Map<String, List<ServiceInstance>> instancesMap = new HashMap<>();

        if (type == ScmTargetType.INSTANCE) {
            List<String> hostPortUrls = changeToHostName(targets);
            List<String> services = discoveryClient.getServices();
            for (String service : services) {
                List<ServiceInstance> serviceInstances = discoveryClient.getInstances(service);
                if (serviceInstances == null || serviceInstances.isEmpty()) {
                    continue;
                }
                for (ServiceInstance serviceInstance : serviceInstances) {
                    String url = getHostPortUrl(serviceInstance);
                    if (hostPortUrls.contains(url)) {
                        hostPortUrls.remove(url);
                        List<ServiceInstance> instancesList = instancesMap.get(service);
                        if (instancesList == null) {
                            instancesList = new ArrayList<>();
                            instancesMap.put(service, instancesList);
                        }
                        instancesList.add(serviceInstance);
                    }
                }
            }

            if (hostPortUrls.size() != 0) {
                throw new ScmConfigException(ScmConfError.INVALID_ARG,
                        "no such instances:" + hostPortUrls);
            }

            return instancesMap;
        }

        List<String> services;
        if (type == ScmTargetType.ALL) {
            services = discoveryClient.getServices();
        }
        else {
            services = targets;
        }

        for (String service : services) {
            List<ServiceInstance> serviceInstances = discoveryClient.getInstances(service);
            if (serviceInstances == null || serviceInstances.isEmpty()) {
                if (type != ScmTargetType.ALL) {
                    // type is service, no instance for this service, it means
                    // service name is wrong.
                    throw new ScmConfigException(ScmConfError.INVALID_ARG,
                            "no instance for service:service=" + service);
                }
                continue;
            }

            instancesMap.put(service, serviceInstances);
        }

        return instancesMap;

    }

    private List<String> changeToHostName(List<String> urls) throws ScmConfigException {
        List<String> hostPortUrls = new ArrayList<>();
        for (String url : urls) {
            String[] arr = url.trim().split(":");
            if (arr.length != 2) {
                throw new ScmConfigException(ScmConfError.INVALID_ARG, "invalid server url:" + url);
            }
            try {
                String hostName = getHostName(arr[0]);
                String hostPortUrl = hostName + ":" + arr[1];

                // discard duplicate url.
                if (!hostPortUrls.contains(hostPortUrl)) {
                    hostPortUrls.add(hostPortUrl);
                }
            }
            catch (Exception e) {
                throw new ScmConfigException(ScmConfError.INVALID_ARG, "unknown url:" + url, e);
            }
        }
        return hostPortUrls;
    }

    private String getHostName(String ipOrHost) throws Exception {
        InetAddress ia = InetAddress.getByName(ipOrHost);
        return ia.getHostName();
    }

    private String getHostPortUrl(ServiceInstance instance) {
        try {
            return getHostName(instance.getHost()) + ":" + instance.getPort();
        }
        catch (Exception e) {
            logger.warn(
                    "unknown service instance in service center:serviceInstanceUrl={}:{},serviceName={}",
                    instance.getHost(), instance.getPort(), instance.getServiceId(), e);
            return instance.getHost() + instance.getPort();
        }
    }
}
