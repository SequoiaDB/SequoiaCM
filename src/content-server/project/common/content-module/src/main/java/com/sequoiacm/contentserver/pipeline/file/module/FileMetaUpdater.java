package com.sequoiacm.contentserver.pipeline.file.module;

import com.sequoiacm.contentserver.dao.ScmFileVersionHelper;
import com.sequoiacm.contentserver.model.FileFieldExtraDefine;
import com.sequoiacm.contentserver.model.ScmVersion;

// 描述一个文件属性的更新，key表示字段名，value表示新值，isGlobal
// 表示是否为全局属性（所有版本一致的属性如bucketId），version表示要更新哪个版本的该字段（isGlobal为false时有效）
public class FileMetaUpdater {
    private String key;
    private Object value;
    private boolean isGlobal;
    private ScmVersion version;
    private boolean isUserField;

    public FileMetaUpdater(String key, Object value, int majorVersion, int minorVersion) {
        this(key, value, ScmFileVersionHelper.isUnifiedField(key), majorVersion, minorVersion);
    }

    public FileMetaUpdater(String key, Object value) {
        this(key, value, ScmFileVersionHelper.isUnifiedField(key), -1, -1);
    }

    public FileMetaUpdater(String key, Object value, boolean isGlobal, int majorVersion,
            int minorVersion) {
        this.key = key;
        this.value = value;
        this.isGlobal = isGlobal;
        version = new ScmVersion(majorVersion, minorVersion);
        isUserField = FileFieldExtraDefine.isUserField(key);
    }

    public String getKey() {
        return key;
    }

    public Object getValue() {
        return value;
    }

    public boolean isGlobal() {
        return isGlobal;
    }

    public int getMajorVersion() {
        return version.getMajorVersion();
    }

    public int getMinorVersion() {
        return version.getMinorVersion();
    }

    public ScmVersion getVersion() {
        return version;
    }

    public boolean isUserField() {
        return isUserField;
    }
}
