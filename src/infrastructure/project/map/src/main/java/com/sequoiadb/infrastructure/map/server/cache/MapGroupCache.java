package com.sequoiadb.infrastructure.map.server.cache;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantReadWriteLock;

import com.sequoiadb.infrastructure.map.server.model.MapMeta;

class MapGroupCache {
    private Map<String, MapMeta> mapMetaMap = new HashMap<String, MapMeta>();
    private ReentrantReadWriteLock rwLock = new ReentrantReadWriteLock();
    private String groupName;

    public MapGroupCache(String groupName) {
        this.groupName = groupName;
    }

    public String getGroupName() {
        return groupName;
    }

    public MapMeta get(String mapName) {
        Lock rlock = rwLock.readLock();
        try {
            rlock.lock();
            MapMeta mapMeta = mapMetaMap.get(mapName);
            if (mapMeta != null) {
                mapMeta.updateLastTime();
            }
            return mapMeta;
        }
        finally {
            rlock.unlock();
        }
    }

    public List<MapMeta> list() {
        Lock rlock = rwLock.readLock();
        try {
            rlock.lock();
            return new ArrayList<MapMeta>(mapMetaMap.values());
        }
        finally {
            rlock.unlock();
        }
    }

    public long size() {
        Lock rlock = rwLock.readLock();
        try {
            rlock.lock();
            return mapMetaMap.size();
        }
        finally {
            rlock.unlock();
        }
    }

    public void put(MapMeta mapMeta) {
        Lock wLock = rwLock.writeLock();
        try {
            wLock.lock();
            mapMeta.updateLastTime();
            mapMetaMap.put(mapMeta.getName(), mapMeta);
        }
        finally {
            wLock.unlock();
        }
    }

    public void remove(String mapName) {
        Lock wLock = rwLock.writeLock();
        try {
            wLock.lock();
            mapMetaMap.remove(mapName);
        }
        finally {
            wLock.unlock();
        }
    }
}
