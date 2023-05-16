package com.sequoiacm.contentserver.pipeline.file.module;

import com.sequoiacm.contentserver.metasourcemgr.ScmMetaSourceHelper;
import com.sequoiacm.exception.ScmError;
import com.sequoiacm.exception.ScmServerException;
import org.bson.BSONObject;
import org.bson.BasicBSONObject;

import java.util.Objects;

// 使用 $set 更新文件属性
public class FileMetaDefaultUpdater extends FileMetaUpdater {

    private final Object value;

    protected FileMetaDefaultUpdater(String field, Object value, boolean isGlobal, int majorVersion,
            int minorVersion) {
        super(field, isGlobal, majorVersion, minorVersion);
        this.value = value;
    }

    // 更新所有版本一致的文件属性
    public static FileMetaDefaultUpdater globalFieldUpdater(String field, Object value) {
        return new FileMetaDefaultUpdater(field, value, true, -1, -1);
    }

    // 更新版本私有的文件属性
    public static FileMetaDefaultUpdater versionFieldUpdater(String field, Object value,
            int majorVersion, int minorVersion) {
        return new FileMetaDefaultUpdater(field, value, false, majorVersion, minorVersion);
    }

    @Override
    public void injectFileUpdater(BSONObject fileTableUpdater) throws ScmServerException {
        BSONObject set = (BSONObject) computeIfAbsent(fileTableUpdater, "$set",
                k -> new BasicBSONObject());
        Object oldValue = set.put(getFileField(), value);
        if (oldValue != null && !Objects.equals(value, oldValue)) {
            throw new ScmServerException(ScmError.SYSTEM_ERROR,
                    "conflict update: " + getFileField() + ", myValue=" + value + ", otherValue="
                            + oldValue + ", updater=" + fileTableUpdater);
        }
    }

    @Override
    public void injectBucketRelationUpdater(BSONObject bucketRelationUpdater)
            throws ScmServerException {
        String mappingFiled = ScmMetaSourceHelper.getBucketFileMappingField(getFileField());
        if (mappingFiled == null) {
            return;
        }

        BSONObject set = (BSONObject) computeIfAbsent(bucketRelationUpdater, "$set",
                k -> new BasicBSONObject());
        Object oldValue = set.put(mappingFiled, value);
        if (oldValue != null && !Objects.equals(value, oldValue)) {
            throw new ScmServerException(ScmError.SYSTEM_ERROR,
                    "conflict update: " + getFileField() + ", myValue=" + value + ", otherValue="
                            + oldValue + ", updater=" + bucketRelationUpdater);
        }
    }

    @Override
    public void injectDirRelationUpdater(BSONObject dirRelationUpdater) throws ScmServerException {
        String mappingFiled = ScmMetaSourceHelper.getDirRelMappingField(getFileField());
        if (mappingFiled == null) {
            return;
        }

        BSONObject set = (BSONObject) computeIfAbsent(dirRelationUpdater, "$set",
                k -> new BasicBSONObject());
        Object oldValue = set.put(mappingFiled, value);
        if (oldValue != null && !Objects.equals(value, oldValue)) {
            throw new ScmServerException(ScmError.SYSTEM_ERROR,
                    "conflict update: " + getFileField() + ", myValue=" + value + ", otherValue="
                            + oldValue + ", updater=" + dirRelationUpdater);
        }
    }

    public Object getValue() {
        return value;
    }
}
