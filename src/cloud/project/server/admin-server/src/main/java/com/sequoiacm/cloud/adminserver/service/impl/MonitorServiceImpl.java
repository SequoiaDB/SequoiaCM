package com.sequoiacm.cloud.adminserver.service.impl;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import org.bson.BSONObject;
import org.bson.BasicBSONObject;
import org.bson.types.BasicBSONList;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cloud.client.ServiceInstance;
import org.springframework.cloud.client.discovery.DiscoveryClient;
import org.springframework.stereotype.Service;

import com.sequoiacm.cloud.adminserver.common.MonitorDefine;
import com.sequoiacm.cloud.adminserver.dao.ContentServerDao;
import com.sequoiacm.cloud.adminserver.model.ContentServerInfo;
import com.sequoiacm.cloud.adminserver.model.HealthInfo;
import com.sequoiacm.cloud.adminserver.remote.MonitorServerClient;
import com.sequoiacm.cloud.adminserver.remote.MonitorServerClientFactory;
import com.sequoiacm.cloud.adminserver.service.IMonitorService;
import com.sequoiacm.infrastructure.monitor.model.WorkspaceFlow;

@Service
public class MonitorServiceImpl implements IMonitorService {

    private static final Logger logger = LoggerFactory.getLogger(MonitorServiceImpl.class);

    @Autowired
    private DiscoveryClient dc;

    @Autowired
    private ContentServerDao cd;

    @Override
    public BSONObject listHealth(String serviceName) throws Exception {
        List<String> services = new ArrayList<String>();
        if (serviceName != null) {
            services = dc.getServices();
            if (services.contains(serviceName)) {
                services.clear();
                services.add(serviceName);
            }
            else {
                throw new Exception(" unknown service name:" + serviceName);
            }
        }
        else {
            services = dc.getServices();
        }

        BasicBSONList resultList = new BasicBSONList();
        for (String name : services) {
            List<ServiceInstance> instances = dc.getInstances(name);
            for (ServiceInstance i : instances) {
                BasicBSONObject h = new BasicBSONObject();
                String node = i.getHost() + ":" + i.getPort();
                h.put("service_name", name);
                h.put("node_name", node);
                h.put("status", getStatus(node));
                resultList.add(h);
            }
        }

        return resultList;
    }

    @Override
    public BSONObject listHostInfo() throws Exception {
        BasicBSONList resultList = new BasicBSONList();
        List<String> services = dc.getServices();
        Map<String, List<String>> hosts = new HashMap<String, List<String>>();
        for (String s : services) {
            List<ServiceInstance> instances = dc.getInstances(s);
            for (ServiceInstance i : instances) {
                String h = i.getHost();
                List<String> ports = hosts.get(h);
                if (ports == null) {
                    ports = new ArrayList<String>();
                    ports.add(String.valueOf(i.getPort()));
                    hosts.put(h, ports);
                }
                else {
                    ports.add(String.valueOf(i.getPort()));
                }
            }
        }

        for (Entry<String, List<String>> entry : hosts.entrySet()) {
            String h = entry.getKey();
            List<String> ports = entry.getValue();
            for (int i = 0; i < ports.size(); i++) {
                BSONObject info = null;
                String node = h + ":" + ports.get(i);
                try {
                    MonitorServerClient fc = MonitorServerClientFactory
                            .getFeignClientByNodeUrl(node);
                    info = fc.getHostInfo();
                    logger.debug(" get host info by feign, node={}", node);
                }
                catch (Exception e) {
                    logger.warn(" fail to get host info by feign, node={}", node, e);
                    continue;
                }
                if (info == null || info.isEmpty()) {
                    continue;
                }
                info.put("hostname", h);
                resultList.add(info);
                break;
            }
        }
        return resultList;
    }

    private WorkspaceFlow getOrCreateWorkspaceFlow(Map<String, WorkspaceFlow> workspaceFlowMap,
            String wsName) {
        WorkspaceFlow workspaceFlow = workspaceFlowMap.get(wsName);
        if (workspaceFlow == null) {
            workspaceFlow = new WorkspaceFlow(wsName);
            workspaceFlowMap.put(wsName, workspaceFlow);
        }

        return workspaceFlow;
    }

    @Override
    public Collection<WorkspaceFlow> showFlow() throws Exception {
        Map<String, WorkspaceFlow> all = new HashMap<String, WorkspaceFlow>();
        List<ContentServerInfo> contentservers = cd.queryAll();

        for (ContentServerInfo i : contentservers) {
            String node = i.getNodeUrl();
            Collection<WorkspaceFlow> flowCollections = null;
            try {
                MonitorServerClient fc = MonitorServerClientFactory.getFeignClientByNodeUrl(node);
                flowCollections = fc.showFlow();
            }
            catch (Exception e) {
                logger.warn(" fail to show flow by feign, node={}", node, e);
                continue;
            }

            if (flowCollections == null || flowCollections.isEmpty()) {
                continue;
            }

            logger.debug("get flow by feign, node={}, flow={}", node, flowCollections);

            for (WorkspaceFlow flow : flowCollections) {
                WorkspaceFlow f = getOrCreateWorkspaceFlow(all, flow.getWorkspaceName());
                f.addDownloadSize(flow.getDownload().get());
                f.addUploadSize(flow.getUpload().get());
            }
        }

        return all.values();
    }

    @Override
    public BSONObject gaugeResponse() throws Exception {
        BasicBSONList resultList = new BasicBSONList();
        List<ServiceInstance> instances = dc.getInstances(MonitorDefine.GATEWAY);
        for (ServiceInstance i : instances) {
            String node = i.getHost() + ":" + String.valueOf(i.getPort());
            BSONObject nodeReq = null;
            try {
                MonitorServerClient fc = MonitorServerClientFactory.getFeignClientByNodeUrl(node);
                nodeReq = fc.gaugeResponse();
            }
            catch (Exception e) {
                logger.warn(" fail to gauge response by feign, node={}", node, e);
                continue;
            }
            if (nodeReq != null) {
                nodeReq.put("service_name", i.getServiceId());
                nodeReq.put("node_name", node);
            }
            resultList.add(nodeReq);
        }
        return resultList;
    }

    private String getStatus(String node) {
        String status = MonitorDefine.DEFAULT_SCM_SERVICE_STATUS;
        try {
            MonitorServerClient fc = MonitorServerClientFactory.getFeignClientByNodeUrl(node);
            HealthInfo h = fc.getHealth();
            status = h.getStatus();
        }
        catch (Exception e) {
            logger.warn("monitor get node status failed, node={}", node, e);
        }
        return status;
    }
}
