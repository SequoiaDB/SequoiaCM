package com.sequoiacm.daemon.manager;

import com.sequoiacm.daemon.common.DaemonDefine;
import com.sequoiacm.daemon.element.ScmNodeInfo;
import com.sequoiacm.daemon.lock.ScmFileLock;
import com.sequoiacm.daemon.lock.ScmFileResource;
import com.sequoiacm.daemon.lock.ScmFileResourceFactory;
import com.sequoiacm.infrastructure.tool.exception.ScmToolsException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

public class ScmTask {
    private static final Logger logger = LoggerFactory.getLogger(ScmTask.class);

    public static void doTask(ScmMonitorTableMgr tableMgr, ScmNodeMgr nodeMgr) {
        List<ScmNodeInfo> nodeList = new ArrayList<>();
        try {
            nodeList = tableMgr.readTable();
        }
        catch (ScmToolsException e) {
            logger.warn("Failed to read table while doing task,error:{}", e.getMessage(), e);
        }

        for (ScmNodeInfo node : nodeList) {
            try {
                String confPath = node.getConfPath();
                File nodeFile = new File(confPath);
                if (!nodeFile.exists()) {
                    tableMgr.removeNodeInfo(node);
                    continue;
                }

                if (node.getStatus().equals(DaemonDefine.NODE_STATUS_ON)) {
                    boolean isRunning = nodeMgr.isNodeRunning(node);
                    if (isRunning) {
                        continue;
                    }

                    // read again for getting the newly status of node from monitor table
                    // readTableOnce(on) => node stop(on->off) => tool found node failed,and start
                    // node(concurrency trouble)
                    // readTableOnce(on) => node stop(on->off) => tool found node failed,read
                    // again(off),continue
                    File tableFile = new File(tableMgr.getTablePath());
                    ScmFileResource resource = ScmFileResourceFactory.getInstance().createFileResource(tableFile, tableMgr.getBackUpPath());
                    ScmFileLock lock = resource.createLock();
                    lock.lock();
                    try {
                        List<ScmNodeInfo> newlyNodeList = resource.readFile();
                        ScmNodeInfo newlyNode = null;
                        for (ScmNodeInfo info : newlyNodeList) {
                            if (info.getPort() == node.getPort()) {
                                newlyNode = info;
                                break;
                            }
                        }
                        if (newlyNode != null
                                && newlyNode.getStatus().equals(DaemonDefine.NODE_STATUS_ON)) {
                            nodeMgr.startNode(node);
                            logger.info("Start node success,node:{}", node.toString());
                        }
                    }
                    finally {
                        lock.unlock();
                        resource.releaseFileResource();
                    }
                }
            }
            catch (ScmToolsException e) {
                logger.warn("Failed to monitor node:{},error:{}", node.toString(), e.getMessage(),
                        e);
            }
        }
    }
}
