package com.sequoiacm.contentserver.pipeline.file.module;

import java.util.ArrayList;
import java.util.List;

public class DeleteFileResult {
    private List<FileMeta> deletedVersion = new ArrayList<>();

    public DeleteFileResult() {
    }

    public List<FileMeta> getDeletedVersion() {
        return deletedVersion;
    }

    public void setDeletedVersion(List<FileMeta> deletedVersion) {
        this.deletedVersion = deletedVersion;
    }
}
