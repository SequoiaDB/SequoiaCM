package com.sequoiacm.contentserver.pipeline.file.module;

import com.sequoiacm.metasource.TransactionContext;

public class CreateFileContext {
    private String ws;
    private FileMeta fileMeta;
    private TransactionContext transactionContext;

    public String getWs() {
        return ws;
    }

    public void setWs(String ws) {
        this.ws = ws;
    }

    public FileMeta getFileMeta() {
        return fileMeta;
    }

    public void setFileMeta(FileMeta fileMeta) {
        this.fileMeta = fileMeta;
    }

    public TransactionContext getTransactionContext() {
        return transactionContext;
    }

    public void setTransactionContext(TransactionContext transactionContext) {
        this.transactionContext = transactionContext;
    }
}
