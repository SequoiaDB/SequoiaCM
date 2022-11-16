package com.sequoiacm.contentserver.pipeline.file.module;

import com.sequoiacm.contentserver.pipeline.file.module.FileMeta;

public class UpdateFileMetaResult {
    private FileMeta latestVersionAfterUpdate;
    private FileMeta specifiedReturnVersion;

    public FileMeta getLatestVersionAfterUpdate() {
        return latestVersionAfterUpdate;
    }

    public void setLatestVersionAfterUpdate(FileMeta latestVersionAfterUpdate) {
        this.latestVersionAfterUpdate = latestVersionAfterUpdate;
    }

    public void setSpecifiedReturnVersion(FileMeta specifiedReturnVersion) {
        this.specifiedReturnVersion = specifiedReturnVersion;
    }

    public FileMeta getSpecifiedReturnVersion() {
        return specifiedReturnVersion;
    }
}
