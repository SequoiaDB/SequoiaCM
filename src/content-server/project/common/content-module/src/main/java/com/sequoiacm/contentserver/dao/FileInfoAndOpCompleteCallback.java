package com.sequoiacm.contentserver.dao;

import com.sequoiacm.contentserver.listener.OperationCompleteCallback;
import com.sequoiacm.contentserver.pipeline.file.module.FileMeta;

public class FileInfoAndOpCompleteCallback {
    private final FileMeta fileInfo;
    private final OperationCompleteCallback callback;

    public FileInfoAndOpCompleteCallback(FileMeta fileInfo, OperationCompleteCallback callback) {
        this.fileInfo = fileInfo;
        this.callback = callback;
    }

    public FileMeta getFileInfo() {
        return fileInfo;
    }

    public OperationCompleteCallback getCallback() {
        return callback;
    }
}
