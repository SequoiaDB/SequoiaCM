package com.sequoiacm.daemon.manager;

import com.sequoiacm.daemon.common.CommonUtils;
import com.sequoiacm.daemon.element.ScmNodeInfo;
import com.sequoiacm.daemon.exception.ScmExitCode;
import com.sequoiacm.daemon.exec.ScmExecutor;
import com.sequoiacm.daemon.operator.NodeOperator;
import com.sequoiacm.daemon.operator.ScmNodeOperator;
import com.sequoiacm.daemon.operator.ZkNodeOperator;
import com.sequoiacm.infrastructure.tool.element.ScmServerScriptEnum;
import com.sequoiacm.infrastructure.tool.exception.ScmToolsException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class ScmNodeMgr {
    private static final Logger logger = LoggerFactory.getLogger(ScmNodeMgr.class);
    private final Map<ScmServerScriptEnum, NodeOperator> ops = new HashMap<>();
    private static volatile ScmNodeMgr instance;

    public static ScmNodeMgr getInstance() throws ScmToolsException {
        if (instance != null) {
            return instance;
        }
        synchronized (ScmNodeMgr.class) {
            if (instance != null) {
                return instance;
            }
            instance = new ScmNodeMgr();
            return instance;
        }
    }

    private ScmNodeMgr() throws ScmToolsException {
        ScmExecutor executor = CommonUtils.getExecutor();
        ZkNodeOperator zkOp = new ZkNodeOperator(executor);
        ScmNodeOperator scmOp = new ScmNodeOperator(executor);
        ops.put(ScmServerScriptEnum.ZOOKEEPER, zkOp);
        ops.put(ScmServerScriptEnum.SEQUOIACM, scmOp);
        ops.put(ScmServerScriptEnum.CONFIGSERVER, scmOp);
        ops.put(ScmServerScriptEnum.ADMINSERVER, scmOp);
        ops.put(ScmServerScriptEnum.AUTHSERVER, scmOp);
        ops.put(ScmServerScriptEnum.FULLTEXTSERVER, scmOp);
        ops.put(ScmServerScriptEnum.GATEWAY, scmOp);
        ops.put(ScmServerScriptEnum.MQSERVER, scmOp);
        ops.put(ScmServerScriptEnum.OMSERVER, scmOp);
        ops.put(ScmServerScriptEnum.S3SERVER, scmOp);
        ops.put(ScmServerScriptEnum.SCHEDULESERVER, scmOp);
        ops.put(ScmServerScriptEnum.SERVICECENTER, scmOp);
        ops.put(ScmServerScriptEnum.SERVICETRACE, scmOp);
    }

    public boolean isNodeRunning(ScmNodeInfo node) throws ScmToolsException {
        NodeOperator operator = ops.get(node.getServerType());
        if (operator == null) {
            throw new ScmToolsException("Failed to check node running,caused by: no such operator",
                    ScmExitCode.INVALID_ARG);
        }
        try {
            return operator.isNodeRunning(node);
        }
        catch (ScmToolsException e) {
            throw new ScmToolsException("Failed to check node running,node:" + node.toString(),
                    e.getExitCode(), e);
        }
    }

    public void startNode(ScmNodeInfo node) throws ScmToolsException {
        NodeOperator operator = ops.get(node.getServerType());
        if (operator == null) {
            throw new ScmToolsException("Failed to start node,caused by: no such operator",
                    ScmExitCode.INVALID_ARG);
        }
        try {
            operator.startNode(node);
        }
        catch (ScmToolsException e) {
            throw new ScmToolsException("Failed to start scm node,node:" + node.toString(),
                    e.getExitCode(), e);
        }
    }

    public List<ScmNodeInfo> generateNodeInfo(File serviceDir) throws ScmToolsException {
        List<ScmNodeInfo> nodeInfoList = null;

        String dirName = serviceDir.getName();
        ScmServerScriptEnum serverScriptEnum = ScmServerScriptEnum.getEnumByDirName(dirName);
        NodeOperator operator = ops.get(serverScriptEnum);
        if (operator == null) {
            logger.info(dirName + " isn't a scm node");
        }
        else {
            try {
                nodeInfoList = operator.getNodeInfos(serviceDir);
            }
            catch (ScmToolsException e) {
                throw new ScmToolsException(
                        "Failed to generate node info in dir:" + serviceDir.getAbsolutePath(),
                        e.getExitCode(), e);
            }
        }
        return nodeInfoList == null ? new ArrayList<ScmNodeInfo>() : nodeInfoList;
    }

    public int getNodePort(ScmNodeInfo node) throws ScmToolsException {
        NodeOperator operator = ops.get(node.getServerType());
        if (operator == null) {
            throw new ScmToolsException("Failed to get node port,caused by: no such operator",
                    ScmExitCode.INVALID_ARG);
        }
        try {
            return operator.getNodePort(node);
        }
        catch (ScmToolsException e) {
            throw new ScmToolsException("Failed to get node port,node:" + node.toString(),
                    e.getExitCode(), e);
        }
    }
}
