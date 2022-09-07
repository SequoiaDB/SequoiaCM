package com.sequoiacm.infrastructure.lock.curator;

import java.util.List;

import com.sequoiacm.infrastructure.lock.exception.ZkPacketLenOutOfRangeException;
import org.apache.curator.framework.CuratorFramework;
import org.apache.zookeeper.KeeperException;
import org.apache.zookeeper.KeeperException.NoNodeException;
import org.apache.zookeeper.KeeperException.NotEmptyException;
import org.apache.zookeeper.data.Stat;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class CuratorZKBase {
    private static final Logger logger = LoggerFactory.getLogger(CuratorZKBase.class);

    public static void deleteNode(CuratorFramework client, String path) throws Exception {
        try {
            client.delete().forPath(path);
        }
        catch (NoNodeException e) {
            throw e;
        }
        catch (NotEmptyException e) {
            throw e;
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
        catch (NoNodeException e) {
            throw e;
        }
        catch (KeeperException.ConnectionLossException e) {
            // 列取子节点时，可能会由于子节点数过多超过 buffer 大小，抛出 ConnectionLossException 异常，这里将其转换为自定义的 ZkPacketLenOutRangeException
            // 由于 ConnectionLossException 是一个通用的异常，这个转换并不保证一定准确
            throw new ZkPacketLenOutOfRangeException(e);
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
