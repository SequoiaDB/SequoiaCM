package com.sequoiacm.contentserver.pipeline.file.module;

public class AddFileMetaVersionResult {
    private FileMeta newVersion;
    private FileMeta deletedVersion;

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

    @Override
    public String toString() {
        return "AddFileMetaVersionResult{" + "newVersion=" + newVersion + ", deletedVersion="
                + deletedVersion + '}';
    }
}
