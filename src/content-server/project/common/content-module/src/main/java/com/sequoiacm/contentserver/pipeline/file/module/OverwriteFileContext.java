package com.sequoiacm.contentserver.pipeline.file.module;

import java.util.ArrayList;
import java.util.List;

public class OverwriteFileContext extends CreateFileContext {
    // 预期被覆盖的文件
    private FileMeta overwrittenFile;

    // filter 的检查阶段，将会置位该标记，表示预期被覆盖的文件是否与正在创建的文件冲突
    private boolean isOverwrittenFileConflict;
    private List<FileMeta> deleteVersion = new ArrayList<>();

    public void setOverwrittenFileConflict(boolean overwrittenFileConflict) {
        this.isOverwrittenFileConflict = overwrittenFileConflict;
    }

    public boolean isOverwrittenFileConflict() {
        return isOverwrittenFileConflict;
    }

    public List<FileMeta> getDeleteVersion() {
        return deleteVersion;
    }

    public FileMeta getOverwrittenFile() {
        return overwrittenFile;
    }

    public void setOverwrittenFile(FileMeta overwrittenFile) {
        this.overwrittenFile = overwrittenFile;
    }
}
