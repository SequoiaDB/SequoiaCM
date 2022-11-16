package com.sequoiacm.contentserver.pipeline.file.module;

import java.util.List;

public class OverwriteFileMetaResult {
    private FileMeta newFile;
    private List<FileMeta> deletedVersion;

    public OverwriteFileMetaResult(FileMeta newFile, List<FileMeta> deletedVersion) {
        this.newFile = newFile;
        this.deletedVersion = deletedVersion;
    }

    public OverwriteFileMetaResult() {
    }

    public FileMeta getNewFile() {
        return newFile;
    }

    public void setNewFile(FileMeta newFile) {
        this.newFile = newFile;
    }

    public List<FileMeta> getDeletedVersion() {
        return deletedVersion;
    }

    public void setDeletedVersion(List<FileMeta> deletedVersion) {
        this.deletedVersion = deletedVersion;
    }
}
