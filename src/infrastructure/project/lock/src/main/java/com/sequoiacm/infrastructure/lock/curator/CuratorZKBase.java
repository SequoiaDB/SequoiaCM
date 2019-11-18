package com.sequoiacm.infrastructure.lock.curator;

import java.util.List;

import org.apache.curator.framework.CuratorFramework;
import org.apache.zookeeper.data.Stat;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class CuratorZKBase {
    private static final Logger logger = LoggerFactory.getLogger(CuratorZKBase.class);

    public static void deleteNode(CuratorFramework client, String path) throws Exception {
        try {
            client.delete().forPath(path);
        }
        catch (Exception e) {
            logger.error("Fail to deleteNode:path={}", path);
            throw e;
        }
    }

    public static Stat exists(CuratorFramework client, String path) throws Exception {
        try {
            return client.checkExists().forPath(path);
        }
        catch (Exception e) {
            logger.error("Fail to checkExists:path={}", path);
            throw e;
        }
    }

    public static List<String> getChildren(CuratorFramework client, String path) throws Exception {
        try {
            return client.getChildren().forPath(path);
        }
        catch (Exception e) {
            logger.error("Fail to getChildren:path={}", path);
            throw e;
        }
    }

    public static void closeClient(CuratorFramework client) {
        try {
            if (null != client) {
                client.close();
            }
        }
        catch (Exception e) {
            logger.warn("Fail to close curator client!", e);
        }
    }

}
