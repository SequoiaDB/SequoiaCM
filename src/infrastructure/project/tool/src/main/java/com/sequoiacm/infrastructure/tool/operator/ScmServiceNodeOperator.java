package com.sequoiacm.infrastructure.tool.operator;

import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.sequoiacm.infrastructure.tool.element.ScmNodeInfo;
import com.sequoiacm.infrastructure.tool.element.ScmNodeInfoDetail;
import com.sequoiacm.infrastructure.tool.element.ScmNodeType;
import com.sequoiacm.infrastructure.tool.exception.ScmToolsException;

public abstract class ScmServiceNodeOperator {
    public static String HEALTH_STATUS_UP = "UP";
    private static Logger logger = LoggerFactory.getLogger(ScmServiceNodeOperator.class);

    public abstract void init(String installPath) throws ScmToolsException;

    // 启动本机本服务的所有节点，非阻塞启动
    public void startAll() throws ScmToolsException {
        List<ScmNodeInfoDetail> allNode = getAllNodeInfoDetail();
        for (ScmNodeInfoDetail node : allNode) {
            if (node.getPid() != ScmNodeInfoDetail.NOT_RUNNING) {
                continue;
            }

            try {
                start(node.getNodeInfo().getPort());
            }
            catch (ScmToolsException e) {
                logger.error("failed to start node: type={}, port={}",
                        node.getNodeInfo().getNodeType(), node.getNodeInfo().getPort(), e);
            }
        }
    }

    // 启动本机的制定节点，非阻塞启动
    public abstract void start(int port) throws ScmToolsException;

    // 停止本机的指定节点，非阻塞停止
    public abstract void stop(int port, boolean force) throws ScmToolsException;

    // 停止本机本服务的所有节点，非阻塞停止
    public void stopAll(boolean force) throws ScmToolsException {
        List<ScmNodeInfoDetail> allNode = getAllNodeInfoDetail();
        for (ScmNodeInfoDetail node : allNode) {
            if (node.getPid() == ScmNodeInfoDetail.NOT_RUNNING) {
                continue;
            }

            try {
                stop(node.getNodeInfo().getPort(), force);
            }
            catch (ScmToolsException e) {
                logger.error("failed to start node: type={}, port={}",
                        node.getNodeInfo().getNodeType(), node.getNodeInfo().getPort(), e);
            }
        }
    }

    public abstract List<ScmNodeInfoDetail> getAllNodeInfoDetail() throws ScmToolsException;

    // 返回空如果节点没找到
    public abstract ScmNodeInfoDetail getNodeInfoDetail(int port) throws ScmToolsException;

    // 返回空如果节点没找到
    public abstract ScmNodeInfo getNodeInfo(int port) throws ScmToolsException;

    public abstract List<ScmNodeInfo> getAllNodeInfo() throws ScmToolsException;

    // 返回 HEALTH_STATUS_UP 表示节点处于健康状态，其它表示不健康
    public abstract String getHealthDesc(int port);

    public abstract ScmNodeType getNodeType();
}
