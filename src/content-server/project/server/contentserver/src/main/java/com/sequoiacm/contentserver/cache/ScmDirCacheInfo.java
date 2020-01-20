package com.sequoiacm.contentserver.cache;

public class ScmDirCacheInfo {
    private String id;
    private String path;
    private long version;

    public ScmDirCacheInfo(String id, String path, long version) {
        this.id = id;
        this.path = path;
        this.version = version;
    }

    public String getId() {
        return id;
    }

    public String getPath() {
        return path;
    }

    public long getVersion() {
        return version;
    }
}
