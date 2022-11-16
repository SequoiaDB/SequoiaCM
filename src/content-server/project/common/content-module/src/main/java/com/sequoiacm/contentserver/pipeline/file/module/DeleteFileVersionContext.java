package com.sequoiacm.contentserver.pipeline.file.module;

import com.sequoiacm.metasource.TransactionContext;

public class DeleteFileVersionContext  {
    private String fileId;
    private int majorVersion;
    private int minorVersion;
    private String ws;
    private TransactionContext transactionContext;

    private FileMeta deletedVersion;
    private FileMeta latestVersionBeforeDelete;
    private FileMeta latestVersionAfterDelete;
    
    public TransactionContext getTransactionContext() {
        return transactionContext;
    }

    public void setTransactionContext(TransactionContext transactionContext) {
        this.transactionContext = transactionContext;
    }

    public FileMeta getLatestVersionAfterDelete() {
        return latestVersionAfterDelete;
    }

    public void setLatestVersionAfterDelete(FileMeta latestVersionAfterDelete) {
        this.latestVersionAfterDelete = latestVersionAfterDelete;
    }

    public FileMeta getLatestVersionBeforeDelete() {
        return latestVersionBeforeDelete;
    }

    public void setLatestVersionBeforeDelete(FileMeta latestVersionBeforeDelete) {
        this.latestVersionBeforeDelete = latestVersionBeforeDelete;
    }

    public String getFileId() {
        return fileId;
    }

    public void setFileId(String fileId) {
        this.fileId = fileId;
    }

    public int getMajorVersion() {
        return majorVersion;
    }

    public void setMajorVersion(int majorVersion) {
        this.majorVersion = majorVersion;
    }

    public int getMinorVersion() {
        return minorVersion;
    }

    public void setMinorVersion(int minorVersion) {
        this.minorVersion = minorVersion;
    }

    public String getWs() {
        return ws;
    }

    public void setWs(String ws) {
        this.ws = ws;
    }

    public FileMeta getDeletedVersion() {
        return deletedVersion;
    }

    public void setDeletedVersion(FileMeta deletedVersion) {
        this.deletedVersion = deletedVersion;
    }
}
