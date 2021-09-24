package com.sequoiacm.schedule.core.meta;

import com.sequoiacm.schedule.core.ScheduleServer;
import com.sequoiacm.schedule.entity.FileServerEntity;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock.WriteLock;

public class NodeMgr {
    private static final Logger logger = LoggerFactory.getLogger(ScheduleServer.class);
    private Map<Integer, Map<String, FileServerEntity>> serverMapBySiteId = new HashMap<>();
    private Map<String, FileServerEntity> serverMapByName = new HashMap<>();
    private ReentrantReadWriteLock nodeReadWriterLock = new ReentrantReadWriteLock();

    public void initNodeSreverMap(Map<Integer, Map<String, FileServerEntity>> ServerMapBySiteId,
            Map<String, FileServerEntity> ServerMapByName) {
        this.serverMapBySiteId = ServerMapBySiteId;
        this.serverMapByName = ServerMapByName;
    }

    public List<FileServerEntity> getServersBySiteId(int siteId) {
        Lock rLock = nodeReadWriterLock.readLock();
        rLock.lock();
        try {
            Map<String, FileServerEntity> tmpServerMap = serverMapBySiteId.get(siteId);
            List<FileServerEntity> contentServers = new ArrayList<>();
            if (tmpServerMap != null) {
                contentServers.addAll(tmpServerMap.values());
            }
            return contentServers;
        }
        finally {
            rLock.unlock();
        }
    }

    public FileServerEntity getServersById(int id) {
        Lock rLock = nodeReadWriterLock.readLock();
        rLock.lock();
        try {
            for (FileServerEntity s : serverMapByName.values()) {
                if (s.getId() == id) {
                    return s;
                }
            }
            return null;
        }
        finally {
            rLock.unlock();
        }
    }

    public void removeNode(String nodeName) {
        Lock wLock = nodeReadWriterLock.writeLock();
        wLock.lock();
        try {
            FileServerEntity serverInfo = serverMapByName.remove(nodeName);

            // remove server node in site
            if (serverInfo != null) {
                Map<String, FileServerEntity> tmpServerMap = serverMapBySiteId
                        .get(serverInfo.getSiteId());
                if (tmpServerMap != null) {
                    tmpServerMap.remove(nodeName);
                    // node size=0, remove site
                    if (tmpServerMap.size() == 0) {
                        serverMapBySiteId.remove(serverInfo.getSiteId());
                    }
                }
                logger.info("remove node cache:nodeName={}", nodeName);
            }
        }
        finally {
            wLock.unlock();
        }
    }

    public void reloadNode(FileServerEntity serverNode) {
        Lock wLock = nodeReadWriterLock.writeLock();
        wLock.lock();
        try {
            serverMapByName.put(serverNode.getName(), serverNode);

            Map<String, FileServerEntity> tmpServerMap = serverMapBySiteId
                    .get(serverNode.getSiteId());
            if (tmpServerMap == null) {
                tmpServerMap = new HashMap<>();
                tmpServerMap.put(serverNode.getName(), serverNode);
                serverMapBySiteId.put(serverNode.getSiteId(), tmpServerMap);
            }
            else {
                tmpServerMap.put(serverNode.getName(), serverNode);
            }

            logger.info("reload node cache:nodeName={}", serverNode.getName());
        }
        finally {
            wLock.unlock();
        }
    }

    public void removeNodesBySiteId(int siteId) {
        WriteLock wLock = nodeReadWriterLock.writeLock();
        wLock.lock();
        try {
            Map<String, FileServerEntity> serverNodeMap = serverMapBySiteId.remove(siteId);

            if (serverNodeMap != null) {
                Set<String> nodeNameSet = serverNodeMap.keySet();
                for (String nodeName : nodeNameSet) {
                    serverMapByName.remove(nodeName);
                    logger.info("remove node cache in site: nodeName={}, siteId={}", nodeName,
                            siteId);
                }
            }
        }
        finally {
            wLock.unlock();
        }
    }
}
