package com.sequoiacm.infrastructure.tool.operator;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.web.client.RestTemplate;

import com.sequoiacm.infrastructure.tool.element.ScmNodeInfo;
import com.sequoiacm.infrastructure.tool.element.ScmNodeInfoDetail;
import com.sequoiacm.infrastructure.tool.element.ScmNodeProcessInfo;
import com.sequoiacm.infrastructure.tool.element.ScmNodeType;
import com.sequoiacm.infrastructure.tool.exception.ScmToolsException;
import com.sequoiacm.infrastructure.tool.exec.ScmExecutorWrapper;

public abstract class DefaultNodeOperator extends ScmServiceNodeOperator {
    private final ScmNodeType nodeType;
    private ScmExecutorWrapper executor;
    private final List<String> healthEndpoints;
    private final RestTemplate restTemplate;
    private String installPath;
    private static final Logger logger = LoggerFactory.getLogger(DefaultNodeOperator.class);

    public DefaultNodeOperator(ScmNodeType nodeType, String... healthEndpoints)
            throws ScmToolsException {
        this.nodeType = nodeType;
        this.healthEndpoints = Arrays.asList(healthEndpoints);
        SimpleClientHttpRequestFactory factory = new SimpleClientHttpRequestFactory();
        factory.setConnectTimeout(5000);
        factory.setReadTimeout(5000);
        restTemplate = new RestTemplate(factory);
    }

    public DefaultNodeOperator(ScmNodeType nodeType) throws ScmToolsException {
        this(nodeType, "internal/v1/health", "/health");
    }

    @Override
    public void init(String installPath) throws ScmToolsException {
        this.installPath = installPath;
        this.executor = new ScmExecutorWrapper(nodeType, installPath);
    }

    // 启动本机的制定节点，非阻塞启动
    @Override
    public void start(int port) throws ScmToolsException {
        ScmNodeInfo node = executor.getNodeCheck(port);
        executor.startNode(node);
    }

    // 停止本机的指定节点，非阻塞停止
    @Override
    public void stop(int port, boolean force) throws ScmToolsException {
        executor.stopNode(port, force);
    }

    @Override
    public List<ScmNodeInfoDetail> getAllNodeInfoDetail() throws ScmToolsException {
        ArrayList<ScmNodeInfoDetail> ret = new ArrayList<>();
        Map<Integer, ScmNodeInfo> allNode = executor.getAllNode();
        Map<String, ScmNodeProcessInfo> runningNode = executor.getNodeStatus();
        for (ScmNodeInfo nodeInfo : allNode.values()) {
            ScmNodeProcessInfo processInfo = runningNode.get(nodeInfo.getConfPath());
            if (processInfo == null) {
                ret.add(new ScmNodeInfoDetail(nodeInfo, ScmNodeInfoDetail.NOT_RUNNING));
                continue;
            }
            ret.add(new ScmNodeInfoDetail(nodeInfo, processInfo.getPid()));
        }

        return ret;
    }

    @Override
    public ScmNodeInfoDetail getNodeInfoDetail(int port) throws ScmToolsException {
        ScmNodeInfo nodeInfo = executor.getNode(port);
        if (nodeInfo == null) {
            return null;
        }
        Map<String, ScmNodeProcessInfo> runningNode = executor.getNodeStatus();
        ScmNodeProcessInfo processInfo = runningNode.get(nodeInfo.getConfPath());
        if (processInfo == null) {
            return new ScmNodeInfoDetail(nodeInfo, ScmNodeInfoDetail.NOT_RUNNING);

        }
        return new ScmNodeInfoDetail(nodeInfo, processInfo.getPid());
    }

    @Override
    public String getHealthDesc(int port) {
        Map<String, String> exceptionMsgMap = null;
        for (String healthEndpoint : healthEndpoints) {
            try {
                Map<?, ?> resp = restTemplate
                        .getForObject("http://localhost:" + port + "/" + healthEndpoint, Map.class);
                return resp.get("status").toString().trim();
            }
            catch (Exception e) {
                if (exceptionMsgMap == null) {
                    exceptionMsgMap = new HashMap<>();
                }
                String msg = e.getMessage() + " " + Arrays.toString(e.getStackTrace());
                exceptionMsgMap.put(healthEndpoint, msg);
            }
        }
        return exceptionMsgMap.toString();
    }

    @Override
    public ScmNodeType getNodeType() {
        return nodeType;
    }

    @Override
    public List<ScmNodeInfo> getAllNodeInfo() throws ScmToolsException {
        Map<Integer, ScmNodeInfo> allNode = executor.getAllNode();
        ArrayList<ScmNodeInfo> ret = new ArrayList<>();
        ret.addAll(allNode.values());
        return ret;
    }

    @Override
    public ScmNodeInfo getNodeInfo(int port) throws ScmToolsException {
        return executor.getNode(port);
    }
}
