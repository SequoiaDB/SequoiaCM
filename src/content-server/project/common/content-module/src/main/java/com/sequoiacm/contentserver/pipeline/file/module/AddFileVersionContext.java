package com.sequoiacm.contentserver.pipeline.file.module;

import com.sequoiacm.contentserver.model.ScmVersion;
import com.sequoiacm.metasource.TransactionContext;

public class AddFileVersionContext {
    private String fileId;
    private FileMeta newVersion;

    // 目前由 bucketFilter 置位，当新增null版本时，删除旧的null版本
    private ScmVersion shouldDeleteVersion;

    private FileMeta deletedVersion;
    private FileMeta currentLatestVersion;
    private String ws;
    private TransactionContext transactionContext;

    public TransactionContext getTransactionContext() {
        return transactionContext;
    }

    public void setTransactionContext(TransactionContext transactionContext) {
        this.transactionContext = transactionContext;
    }

    public ScmVersion getShouldDeleteVersion() {
        return shouldDeleteVersion;
    }

    public void setShouldDeleteVersion(ScmVersion shouldDeleteVersion) {
        this.shouldDeleteVersion = shouldDeleteVersion;
    }

    public String getFileId() {
        return fileId;
    }

    public void setFileId(String fileId) {
        this.fileId = fileId;
    }

    public FileMeta getNewVersion() {
        return newVersion;
    }

    public void setNewVersion(FileMeta newVersion) {
        this.newVersion = newVersion;
    }

    public FileMeta getDeletedVersion() {
        return deletedVersion;
    }

    public void setDeletedVersion(FileMeta deletedVersion) {
        this.deletedVersion = deletedVersion;
    }

    public FileMeta getCurrentLatestVersion() {
        return currentLatestVersion;
    }

    public void setCurrentLatestVersion(FileMeta currentLatestVersion) {
        this.currentLatestVersion = currentLatestVersion;
    }

    public String getWs() {
        return ws;
    }

    public void setWs(String ws) {
        this.ws = ws;
    }
}
