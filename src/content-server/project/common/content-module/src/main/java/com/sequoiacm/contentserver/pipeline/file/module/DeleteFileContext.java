package com.sequoiacm.contentserver.pipeline.file.module;

import com.sequoiacm.metasource.TransactionContext;

import java.util.ArrayList;
import java.util.List;

public class DeleteFileContext {
    private String fileId;
    private String ws;
    private TransactionContext transactionContext;


    private List<FileMeta> deletedHistoryVersions = new ArrayList<>();
    private FileMeta deletedLatestVersion;

    public String getFileId() {
        return fileId;
    }

    public void setFileId(String fileId) {
        this.fileId = fileId;
    }

    public String getWs() {
        return ws;
    }

    public void setWs(String ws) {
        this.ws = ws;
    }

    public TransactionContext getTransactionContext() {
        return transactionContext;
    }

    public void setTransactionContext(TransactionContext transactionContext) {
        this.transactionContext = transactionContext;
    }

    public List<FileMeta> getDeletedHistoryVersions() {
        return deletedHistoryVersions;
    }

    public void setDeletedHistoryVersions(List<FileMeta> deletedHistoryVersions) {
        this.deletedHistoryVersions = deletedHistoryVersions;
    }

    public FileMeta getDeletedLatestVersion() {
        return deletedLatestVersion;
    }

    public void setDeletedLatestVersion(FileMeta deletedLatestVersion) {
        this.deletedLatestVersion = deletedLatestVersion;
    }
}
