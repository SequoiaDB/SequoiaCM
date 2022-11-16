package com.sequoiacm.contentserver.pipeline.file.module;

public class CreateFileMetaResult {
    private FileMeta newFile;

    public CreateFileMetaResult(FileMeta newFile) {
        this.newFile = newFile;
    }

    public FileMeta getNewFile() {
        return newFile;
    }

    public void setNewFile(FileMeta newFile) {
        this.newFile = newFile;
    }


}
