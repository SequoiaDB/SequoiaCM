package com.sequoiacm.contentserver.pipeline.file.module;

public class FileUploadConf {
    private FileExistStrategy existStrategy;
    private boolean isNeedMd5;

    public FileUploadConf(FileExistStrategy existStrategy, boolean isNeedMd5) {
        this.existStrategy = existStrategy;
        this.isNeedMd5 = isNeedMd5;
    }

    public boolean isNeedMd5() {
        return isNeedMd5;
    }

    public FileExistStrategy getExistStrategy() {
        return existStrategy;
    }
}
