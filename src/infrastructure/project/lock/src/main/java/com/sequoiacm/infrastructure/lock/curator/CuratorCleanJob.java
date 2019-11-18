package com.sequoiacm.infrastructure.lock.curator;

import java.text.SimpleDateFormat;
import java.util.List;

import org.apache.curator.framework.CuratorFramework;
import org.apache.zookeeper.data.Stat;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.sequoiacm.infrastructure.common.timer.ScmTimerTask;

public class CuratorCleanJob extends ScmTimerTask {
    private static final Logger logger = LoggerFactory.getLogger(CuratorCleanJob.class);
    private String zkConnStr;
    private long maxResidualTime;
    private CuratorFramework curatorClient;
    private static final String DIR_SEPERATOR = "/";

    public CuratorCleanJob(String zkConnStr, long maxResidualTime) {
        this.zkConnStr = zkConnStr;
        this.maxResidualTime = maxResidualTime;
    }

    public static String formatTime(long time) {
        SimpleDateFormat fomat = new SimpleDateFormat("HH:mm:ss");
        return fomat.format(time);
    }

    @Override
    public void run() {
        logger.info("cleanup start");
        try {
            List<String> typeNodes = null;
            curatorClient = CuratorLockTools.createClient(zkConnStr);
            String rootPath = CuratorLockTools.getRootPath();
            typeNodes = CuratorZKBase.getChildren(curatorClient, rootPath);
            if (!typeNodes.isEmpty()) {
                clearNodeRecursed(rootPath);
            }
            logger.info("cleanup end");
        }
        catch (Exception e) {
            logger.error("Execute CuratorCleanJob failed", e);
        }
        finally {
            CuratorZKBase.closeClient(curatorClient);
            curatorClient = null;
        }
    }

    public void clearNodeRecursed(String path) throws Exception {
        List<String> childNodes = null;
        try {
            childNodes = CuratorZKBase.getChildren(curatorClient, path);
        }
        catch (Exception e) {
            logger.error("Fail to getChildren:path={}", path);
            throw e;
        }

        if (childNodes.size() == 0) {
            Stat ckStat = CuratorZKBase.exists(curatorClient, path);
            long ephemeralOwner = ckStat.getEphemeralOwner();
            if (ephemeralOwner == 0) {
                long zkNodeMtimeMS = ckStat.getMtime();
                long sysTimeMS = System.currentTimeMillis();
                long nodeLifeTimeMS = sysTimeMS - zkNodeMtimeMS;
                if (nodeLifeTimeMS > maxResidualTime) {
                    try {
                        CuratorZKBase.deleteNode(curatorClient, path);
                    }
                    catch (Exception e) {
                        logger.warn("Fail to deleteNode,path={}", path, e);
                    }
                }
            }
        }
        else {
            for (String childNode : childNodes) {
                String childParentPath = path + DIR_SEPERATOR + childNode;
                clearNodeRecursed(childParentPath);
            }
        }
    }

}
