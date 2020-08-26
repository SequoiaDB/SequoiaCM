package com.sequoiacm.contentserver.cache;

import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantReadWriteLock;

import com.sequoiacm.common.CommonDefine;
import com.sequoiacm.contentserver.common.ScmSystemUtils;
import com.sequoiacm.exception.ScmServerException;

public class ScmDirCache {
    public final static long DEFAULT_VERSION = -1;
    private String wsName;
    private ScmLRUMap id2PathCaches;
    private ScmLRUMap path2IdCaches;
    private long version = 0;
    private ReentrantReadWriteLock cacheLock = new ReentrantReadWriteLock();

    public ScmDirCache(String wsName, int maxSize) {
        this.wsName = wsName;
        id2PathCaches = new ScmLRUMap(maxSize);
        path2IdCaches = new ScmLRUMap(maxSize);
    }

    public String getWsName() {
        return wsName;
    }

    public ScmDirCacheInfo getDirByPath(String path) {
        Lock readLock = cacheLock.readLock();
        readLock.lock();
        try {
            String id = path2IdCaches.get(path);
            if (id != null) {
                return new ScmDirCacheInfo(id, path, version);
            }
            return null;
        }
        finally {
            readLock.unlock();
        }
    }

    public ScmDirCacheInfo getDirById(String id) {
        Lock readLock = cacheLock.readLock();
        readLock.lock();
        try {
            String path = id2PathCaches.get(id);
            if (path != null) {
                return new ScmDirCacheInfo(id, path, version);
            }
            return null;
        }
        finally {
            readLock.unlock();
        }
    }

    public void put(String id, String path, long version) {
        Lock writeLock = cacheLock.writeLock();
        writeLock.lock();
        try {
            if (version < this.version) {
                return;
            }
            if (version > this.version) {
                _clearUpdateVersion(version);
            }
            _put(id, path);
        }
        finally {
            writeLock.unlock();
        }
    }

    private void _put(String id, String path) {
        id2PathCaches.put(id, path);
        path2IdCaches.put(path, id);
    }

    public void removeById(String id, long version) {
        Lock writeLock = cacheLock.writeLock();
        writeLock.lock();
        try {
            if (version < this.version) {
                return;
            }
            if (version > this.version) {
                _clearUpdateVersion(version);
                return;
            }
            _removeById(id);
        }
        finally {
            writeLock.unlock();
        }
    }

    private void _removeById(String id) {
        String path = id2PathCaches.remove(id);
        if (path != null) {
            path2IdCaches.remove(path);
        }
    }

    public void removeByPath(String path, long version) {
        Lock writeLock = cacheLock.writeLock();
        writeLock.lock();
        try {
            if (version < this.version) {
                return;
            }
            if (version > this.version) {
                _clearUpdateVersion(version);
                return;
            }
            _removeByPath(path);
        }
        finally {
            writeLock.unlock();
        }
    }

    private void _removeByPath(String path) {
        String id = path2IdCaches.remove(path);
        if (id != null) {
            id2PathCaches.remove(id);
        }
    }

    public void checkClear(long version) {
        Lock writeLock = cacheLock.writeLock();
        writeLock.lock();
        try {
            if (version > this.version) {
                _clearUpdateVersion(version);
            }
        }
        finally {
            writeLock.unlock();
        }

    }

    private void _clearUpdateVersion(long version) {
        id2PathCaches.clear();
        path2IdCaches.clear();
        _updateVersion(version);
    }

    private void _updateVersion(long version) {
        this.version = version;
    }

    public void deleteDir(String id, String path, long version) {
        Lock writeLock = cacheLock.writeLock();
        writeLock.lock();
        try {
            if (version - 1 < this.version) {
                return;
            }

            if (version - 1 > this.version) {
                _clearUpdateVersion(version);
                return;
            }

            // dbVersion - 1 == cacheVersion
            if (id != null) {
                _removeById(id);
            }
            if (path != null) {
                _removeByPath(path);
            }
            _updateVersion(version);
        }
        finally {
            writeLock.unlock();
        }
    }

    public void renameDir(String id, String newName, long version) throws ScmServerException {
        Lock writeLock = cacheLock.writeLock();
        writeLock.lock();
        try {
            if (version - 1 < this.version) {
                return;
            }

            String oldPath = null;
            if (version - 1 == this.version) {
                oldPath = id2PathCaches.get(id);
            }

            _clearUpdateVersion(version);
            // rewrite rename path cache after clear
            if (oldPath != null) {
                StringBuilder newPath = new StringBuilder(ScmSystemUtils.dirname(oldPath));
                newPath.append(newName).append(CommonDefine.Directory.SCM_DIR_SEP);
                _put(id, newPath.toString());
            }

        }
        finally {
            writeLock.unlock();
        }
    }

    public void moveDir(String id, String targetId, long version) throws ScmServerException {
        Lock writeLock = cacheLock.writeLock();
        writeLock.lock();
        try {
            if (version - 1 < this.version) {
                return;
            }

            String oldPath = null;
            String parentPath = null;
            if (version - 1 == this.version) {
                oldPath = id2PathCaches.get(id);
                parentPath = id2PathCaches.get(targetId);
            }

            _clearUpdateVersion(version);

            // rewrite move path cache after clear
            if (parentPath != null) {
                _put(targetId, parentPath);
                if (oldPath != null) {
                    StringBuilder newPath = new StringBuilder(parentPath);
                    newPath.append(ScmSystemUtils.basename(oldPath))
                            .append(CommonDefine.Directory.SCM_DIR_SEP);
                    _put(id, newPath.toString());
                }
            }
        }
        finally {
            writeLock.unlock();
        }
    }
}
