package com.sequoiacm.fulltext.server.fileidx;

public abstract class FileIdxDao {
    protected final String ws;
    protected final String esIdxLocation;
    protected final String fileId;

    public FileIdxDao(String ws, String fileId, String esIdxLocation) {
        this.ws = ws;
        this.fileId = fileId;
        this.esIdxLocation = esIdxLocation;
    }

    public abstract int processFileCount();

    public abstract void process() throws Exception;

    public String getWsName() {
        return ws;
    }

    public String getFileId() {
        return fileId;
    }

    public String getEsIdxLocation() {
        return esIdxLocation;
    }
}
