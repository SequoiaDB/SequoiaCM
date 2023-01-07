package com.sequoiacm.infrastructure.tool.operator;

import com.sequoiacm.infrastructure.tool.element.ScmNodeInfo;
import com.sequoiacm.infrastructure.tool.element.ScmNodeInfoDetail;
import com.sequoiacm.infrastructure.tool.element.ScmNodeType;
import com.sequoiacm.infrastructure.tool.element.ScmNodeTypeList;
import com.sequoiacm.infrastructure.tool.exception.ScmBaseExitCode;
import com.sequoiacm.infrastructure.tool.exception.ScmToolsException;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class ScmServiceNodeOperatorGroup {
    private Map<ScmNodeType, ScmServiceNodeOperator> operatorMap = new HashMap<>();
    private ScmNodeTypeList typeList = new ScmNodeTypeList();

    public ScmServiceNodeOperatorGroup(List<ScmServiceNodeOperator> operators) {
        for (ScmServiceNodeOperator op : operators) {
            operatorMap.put(op.getNodeType(), op);
            typeList.add(op.getNodeType());
        }
    }

    public ScmNodeTypeList getSupportTypes() {
        return typeList;
    }

    public ScmNodeInfo getNodeInfo(int port) throws ScmToolsException {
        for (ScmServiceNodeOperator op : operatorMap.values()) {
            ScmNodeInfo node = op.getNodeInfo(port);
            if (node != null) {
                return node;
            }
        }
        throw new ScmToolsException("no such node: port=" + port, ScmBaseExitCode.INVALID_ARG);
    }

    public ScmNodeInfoDetail getNodeDetail(int port) throws ScmToolsException {
        for (ScmServiceNodeOperator op : operatorMap.values()) {
            ScmNodeInfoDetail node = op.getNodeInfoDetail(port);
            if (node != null) {
                return node;
            }
        }
        throw new ScmToolsException("no such node: port=" + port, ScmBaseExitCode.INVALID_ARG);
    }

    public Map<Integer, ScmNodeInfo> getAllNode() throws ScmToolsException {
        Map<Integer, ScmNodeInfo> ret = new HashMap<>();
        for (ScmServiceNodeOperator op : operatorMap.values()) {
            List<ScmNodeInfo> nodeInfoList = op.getAllNodeInfo();
            for (ScmNodeInfo node : nodeInfoList) {
                ret.put(node.getPort(), node);
            }
        }
        return ret;
    }

    public List<ScmNodeInfoDetail> getAllNodeDetail() throws ScmToolsException {
        List<ScmNodeInfoDetail> ret = new ArrayList<>();
        for (ScmServiceNodeOperator op : operatorMap.values()) {
            List<ScmNodeInfoDetail> nodes = op.getAllNodeInfoDetail();
            ret.addAll(nodes);
        }
        return ret;
    }

    public Map<Integer, ScmNodeInfo> getNodesByType(ScmNodeType type) throws ScmToolsException {
        ScmServiceNodeOperator op = getOperator(type);

        Map<Integer, ScmNodeInfo> ret = new HashMap<>();
        List<ScmNodeInfo> nodeInfoList = op.getAllNodeInfo();
        for (ScmNodeInfo node : nodeInfoList) {
            ret.put(node.getPort(), node);
        }
        return ret;

    }

    public void init(String installPath) throws ScmToolsException {
        for (ScmServiceNodeOperator op : operatorMap.values()) {
            op.init(installPath);
        }
    }

    public int getNodePid(int port) throws ScmToolsException {
        for (ScmServiceNodeOperator op : operatorMap.values()) {
            ScmNodeInfoDetail detail = op.getNodeInfoDetail(port);
            if (detail != null) {
                return detail.getPid();
            }
        }
        throw new ScmToolsException("no such node: port=" + port, ScmBaseExitCode.INVALID_ARG);
    }

    public void startNode(int port) throws ScmToolsException {
        ScmNodeInfo node = getNodeInfo(port);
        ScmServiceNodeOperator op = getOperator(node.getNodeType());
        op.start(port);
    }

    public void stopNode(int port, boolean force) throws ScmToolsException {
        ScmNodeInfo node = getNodeInfo(port);
        ScmServiceNodeOperator op = getOperator(node.getNodeType());
        op.stop(port, force);
    }

    private ScmServiceNodeOperator getOperator(ScmNodeType type) throws ScmToolsException {
        ScmServiceNodeOperator op = operatorMap.get(type);
        if (op == null) {
            throw new ScmToolsException("no such type: type=" + type.getName(),
                    ScmBaseExitCode.SYSTEM_ERROR);
        }
        return op;
    }

    public String getHealthDesc(int port) throws ScmToolsException {
        ScmNodeInfo node = getNodeInfo(port);
        ScmServiceNodeOperator op = getOperator(node.getNodeType());
        return op.getHealthDesc(port);
    }
}
