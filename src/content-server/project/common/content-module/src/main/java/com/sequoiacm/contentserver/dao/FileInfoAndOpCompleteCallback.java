package com.sequoiacm.contentserver.dao;

import com.sequoiacm.contentserver.listener.OperationCompleteCallback;
import org.bson.BSONObject;

public class FileInfoAndOpCompleteCallback {
    private final BSONObject fileInfo;
    private final OperationCompleteCallback callback;

    public FileInfoAndOpCompleteCallback(BSONObject fileInfo, OperationCompleteCallback callback) {
        this.fileInfo = fileInfo;
        this.callback = callback;
    }

    public BSONObject getFileInfo() {
        return fileInfo;
    }

    public OperationCompleteCallback getCallback() {
        return callback;
    }
}
