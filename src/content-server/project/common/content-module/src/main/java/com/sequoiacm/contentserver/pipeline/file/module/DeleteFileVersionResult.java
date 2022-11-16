package com.sequoiacm.contentserver.pipeline.file.module;

public class DeleteFileVersionResult {
    private FileMeta deletedVersion;

    public FileMeta getDeletedVersion() {
        return deletedVersion;
    }

    public void setDeletedVersion(FileMeta deletedVersion) {
        this.deletedVersion = deletedVersion;
    }
}
