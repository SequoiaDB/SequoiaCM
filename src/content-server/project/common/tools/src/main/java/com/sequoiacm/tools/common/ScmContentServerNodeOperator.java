package com.sequoiacm.tools.common;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

import com.sequoiacm.client.common.ScmType;
import com.sequoiacm.client.core.ScmConfigOption;
import com.sequoiacm.client.core.ScmFactory;
import com.sequoiacm.client.core.ScmSession;
import com.sequoiacm.client.core.ScmSystem;
import com.sequoiacm.client.element.ScmProcessInfo;
import com.sequoiacm.common.CommonDefine;
import com.sequoiacm.infrastructure.tool.element.ScmNodeInfo;
import com.sequoiacm.infrastructure.tool.element.ScmNodeInfoDetail;
import com.sequoiacm.infrastructure.tool.element.ScmNodeType;
import com.sequoiacm.infrastructure.tool.element.ScmNodeTypeEnum;
import com.sequoiacm.infrastructure.tool.element.ScmServerScriptEnum;
import com.sequoiacm.infrastructure.tool.exception.ScmToolsException;
import com.sequoiacm.infrastructure.tool.operator.ScmServiceNodeOperator;
import com.sequoiacm.tools.exec.ScmExecutorWrapper;

public class ScmContentServerNodeOperator extends ScmServiceNodeOperator {
    private final ScmNodeType type;
    private ScmExecutorWrapper execWrapper;

    public ScmContentServerNodeOperator() {
        this.type = new ScmNodeType(ScmNodeTypeEnum.CONTENTSERVER,
                ScmServerScriptEnum.CONTENTSERVER);
    }

    @Override
    public void init(String installPath) throws ScmToolsException {
        this.execWrapper = new ScmExecutorWrapper(installPath);
    }

    // 启动本机的制定节点，非阻塞启动
    @Override
    public void start(int port) throws ScmToolsException {
        execWrapper.startNode(port);
    }

    // 停止本机的指定节点，非阻塞停止
    @Override
    public void stop(int port, boolean force) throws ScmToolsException {
        execWrapper.stopNode(port, force);
    }

    @Override
    public List<ScmNodeInfoDetail> getAllNodeInfoDetail() throws ScmToolsException {
        ArrayList<ScmNodeInfoDetail> ret = new ArrayList<>();
        Map<Integer, String> allNode = execWrapper.getAllNode();
        Map<String, Integer> runningNode = execWrapper.getNodeStatus();
        for (Map.Entry<Integer, String> portAndConf : allNode.entrySet()) {
            Integer pid = runningNode.get(portAndConf.getValue());
            if (pid == null) {
                ret.add(new ScmNodeInfoDetail(new ScmNodeInfo(portAndConf.getValue(),
                        getNodeType(), portAndConf.getKey()), ScmNodeInfoDetail.NOT_RUNNING));
                continue;
            }
            ret.add(new ScmNodeInfoDetail(
                    new ScmNodeInfo(portAndConf.getValue(), getNodeType(), portAndConf.getKey()),
                    pid));
        }
        return ret;
    }

    @Override
    public ScmNodeInfoDetail getNodeInfoDetail(int port) throws ScmToolsException {
        ScmNodeInfo node = getNodeInfo(port);
        if (node == null) {
            return null;
        }
        int pid = execWrapper.getNodePid(port);
        return new ScmNodeInfoDetail(node, pid);
    }

    @Override
    public ScmNodeInfo getNodeInfo(int port) throws ScmToolsException {
        String conf = execWrapper.getNodeConfPath(port);
        return new ScmNodeInfo(conf, getNodeType(), port);
    }

    public Map<Integer, String> getAllNodeConf() throws ScmToolsException {
        return execWrapper.getAllNode();
    }

    @Override
    public List<ScmNodeInfo> getAllNodeInfo() throws ScmToolsException {
        List<ScmNodeInfo> ret = new ArrayList<>();
        Map<Integer, String> allNode = execWrapper.getAllNode();
        for (Map.Entry<Integer, String> entry : allNode.entrySet()) {
            ret.add(new ScmNodeInfo(entry.getValue(), getNodeType(), entry.getKey()));
        }
        return ret;
    }

    @Override
    public String getHealthDesc(int port) {
        String status = getNodeRunningStatus(port);
        if (status.equals(CommonDefine.ScmProcessStatus.SCM_PROCESS_STATUS_RUNING)) {
            return HEALTH_STATUS_UP;
        }
        return status;
    }

    @Override
    public ScmNodeType getNodeType() {
        return type;
    }

    private String getNodeRunningStatus(int port) {
        ScmSession ss = null;
        try {
            ss = ScmFactory.Session.createSession(ScmType.SessionType.NOT_AUTH_SESSION,
                    new ScmConfigOption("localhost:" + port));
        }
        catch (Exception e) {
            return "failed to connect,error=" + e.toString() + ", stacktrace:"
                    + Arrays.toString(e.getStackTrace());
        }

        try {
            ScmProcessInfo processInfo = ScmSystem.ProcessInfo.getProcessInfo(ss);
            if (processInfo.getRunningStatus() == null) {
                return "runningStatus is null";
            }
            return processInfo.getRunningStatus();
        }
        catch (Exception e) {
            return "failed to get node status,error=" + e.toString() + ", stacktrace:"
                    + Arrays.toString(e.getStackTrace());
        }
        finally {
            ss.close();
        }
    }
}
