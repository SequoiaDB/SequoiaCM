package com.sequoiadb.infrastructure.map.server.cache;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantReadWriteLock;

import org.springframework.stereotype.Component;

import com.sequoiadb.infrastructure.map.ScmMapServerException;
import com.sequoiadb.infrastructure.map.server.model.MapMeta;

@Component
public class MapMetaCache {
    private Map<String, MapGroupCache> mapGroupMap = new HashMap<String, MapGroupCache>();
    private ReentrantReadWriteLock groupRwLock = new ReentrantReadWriteLock();

    private List<MapGroupCache> _lisGroup() {
        Lock rlock = groupRwLock.readLock();
        try {
            rlock.lock();
            return new ArrayList<MapGroupCache>(mapGroupMap.values());
        }
        finally {
            rlock.unlock();
        }
    }

    private void checkRemoveGroup(MapGroupCache mapGroup) {
        Lock wLock = groupRwLock.writeLock();
        try {
            wLock.lock();
            if (mapGroup != null && mapGroup.size() == 0) {
                mapGroupMap.remove(mapGroup.getGroupName());
            }
        }
        finally {
            wLock.unlock();
        }
    }

    public void put(MapMeta mapMeta) {
        Lock rlock = groupRwLock.readLock();
        try {
            rlock.lock();
            MapGroupCache mapGroup = mapGroupMap.get(mapMeta.getGroupName());
            if (mapGroup != null) {
                mapGroup.put(mapMeta);
                return;
            }
        }
        finally {
            rlock.unlock();
        }

        Lock wlock = groupRwLock.writeLock();
        try {
            wlock.lock();
            MapGroupCache mapGroup = mapGroupMap.get(mapMeta.getGroupName());
            if (mapGroup == null) {
                mapGroup = new MapGroupCache(mapMeta.getGroupName());
                mapGroupMap.put(mapGroup.getGroupName(), mapGroup);
            }
            mapGroup.put(mapMeta);
        }
        finally {
            wlock.unlock();
        }
    }

    public void remove(String groupName, String mapName) {
        MapGroupCache mapGroup = null;
        Lock rlock = groupRwLock.readLock();
        try {
            rlock.lock();
            mapGroup = mapGroupMap.get(groupName);
            if (mapGroup == null) {
                return;
            }
            mapGroup.remove(mapName);
        }
        finally {
            rlock.unlock();
        }
        checkRemoveGroup(mapGroup);
    }

    public MapMeta get(String groupName, String mapName) {
        Lock rlock = groupRwLock.readLock();
        try {
            rlock.lock();
            MapGroupCache mapGroup = mapGroupMap.get(groupName);
            if (mapGroup != null) {
                return mapGroup.get(mapName);
            }
            return null;
        }
        finally {
            rlock.unlock();
        }
    }

    public void checkAndClear(long maxResidualTime) throws ScmMapServerException {
        List<MapGroupCache> groupList = _lisGroup();
        for (MapGroupCache mapGroupCache : groupList) {
            List<MapMeta> mapMetaList = mapGroupCache.list();
            // check and clear metaMap
            long currentTime = System.currentTimeMillis();
            for (MapMeta mapMeta : mapMetaList) {
                if (currentTime - mapMeta.getLastTime() > maxResidualTime) {
                    mapGroupCache.remove(mapMeta.getName());
                }
            }
            checkRemoveGroup(mapGroupCache);
        }
    }

}
