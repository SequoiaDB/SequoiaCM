package com.sequoiacm.contentserver.pipeline.file.module;

import com.sequoiacm.common.module.TagName;
import com.sequoiacm.contentserver.dao.ScmFileVersionHelper;
import com.sequoiacm.contentserver.model.FileFieldExtraDefine;
import com.sequoiacm.contentserver.model.ScmVersion;
import com.sequoiacm.common.module.TagInfo;
import com.sequoiacm.common.module.TagType;
import com.sequoiacm.exception.ScmError;
import com.sequoiacm.exception.ScmServerException;
import org.bson.BSONObject;
import org.bson.BasicBSONObject;
import org.bson.types.BasicBSONList;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.function.Function;

// 描述一个文件属性的更新，key表示字段名，isGlobal
// 表示是否为全局属性（所有版本一致的属性如bucketId），version表示要更新哪个版本的该字段（isGlobal为false时有效）
public abstract class FileMetaUpdater {
    private String fileField;
    private boolean isGlobal;
    private ScmVersion version;
    private boolean isUserField;

    public FileMetaUpdater(String fileField, int majorVersion, int minorVersion) {
        this(fileField, ScmFileVersionHelper.isUnifiedField(fileField), majorVersion, minorVersion);
    }

    public FileMetaUpdater(String fileField, boolean isGlobal, int majorVersion, int minorVersion) {
        this.fileField = fileField;
        this.isGlobal = isGlobal;
        version = new ScmVersion(majorVersion, minorVersion);
        isUserField = FileFieldExtraDefine.isUserField(fileField);
    }

    // 文件表的更新语句
    public abstract void injectFileUpdater(BSONObject fileTableUpdater) throws ScmServerException;

    // 桶文件关系表更新语句
    public abstract void injectBucketRelationUpdater(BSONObject bucketRelationUpdater)
            throws ScmServerException;

    // 目录文件关系表更新语句
    public abstract void injectDirRelationUpdater(BSONObject dirRelationUpdater)
            throws ScmServerException;

    // 获取 BSON 中的值，如果不存在则使用 function 计算并放入 BSON 中
    protected Object computeIfAbsent(BSONObject bson, String key,
            Function<String, Object> function) {
        Object value = bson.get(key);
        if (value == null) {
            value = function.apply(key);
            bson.put(key, value);
        }
        return value;
    }

    public ScmVersion getVersion() {
        return version;
    }

    public int getMajorVersion() {
        return version.getMajorVersion();
    }

    public int getMinorVersion() {
        return version.getMinorVersion();
    }

    public String getFileField() {
        return fileField;
    }

    public boolean isGlobal() {
        return isGlobal;
    }

    public boolean isUserField() {
        return isUserField;
    }
}

class FileMetaSetTagUpdaterV2 extends FileMetaUpdater {

    private final List<TagInfo> value;
    private final TagType type;
    private List<Long> tagIdList;

    public FileMetaSetTagUpdaterV2(TagType type, List<TagInfo> value, int majorVersion,
            int minorVersion) {
        super(type.getFileField(), majorVersion, minorVersion);
        this.value = value;
        this.type = type;
        tagIdList = new ArrayList<>();
        for (TagInfo tagInfo : value) {
            tagIdList.add(tagInfo.getTagId());
        }
    }

    @Override
    public void injectFileUpdater(BSONObject fileTableUpdater) throws ScmServerException {
        BSONObject set = (BSONObject) computeIfAbsent(fileTableUpdater, "$set",
                k -> new BasicBSONObject());
        Object oldValue = set.put(getFileField(), tagIdList);
        if (oldValue != null && !Objects.equals(tagIdList, oldValue)) {
            throw new ScmServerException(ScmError.SYSTEM_ERROR,
                    "conflict update: " + getFileField() + ", myValue=" + value + ", otherValue="
                            + oldValue + ", updater=" + fileTableUpdater);
        }
    }

    @Override
    public void injectBucketRelationUpdater(BSONObject bucketRelationUpdater)
            throws ScmServerException {
    }

    @Override
    public void injectDirRelationUpdater(BSONObject dirRelationUpdater) throws ScmServerException {
    }
}

class FileMetaSetTagUpdaterV1 extends FileMetaUpdater {

    private final TagType type;
    private BSONObject value;

    public FileMetaSetTagUpdaterV1(TagType type, List<TagName> tagNames, int majorVersion,
            int minorVersion) {
        super(type.getFileField(), majorVersion, minorVersion);
        this.type = type;

        if (type == TagType.TAGS) {
            BasicBSONList tagsList = new BasicBSONList();
            for (TagName tagName : tagNames) {
                tagsList.add(tagName.getTag());
            }
            value = tagsList;
        }
        else if (type == TagType.CUSTOM_TAG) {
            value = new BasicBSONObject();
            for (TagName tagName : tagNames) {
                value.put(tagName.getTagKey(), tagName.getTagValue());
            }
        }
        else {
            throw new IllegalArgumentException("invalid tag type: " + type);
        }
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
    }

    @Override
    public void injectDirRelationUpdater(BSONObject dirRelationUpdater) throws ScmServerException {
    }
}

class FileMetaAddTagUpdater extends FileMetaUpdater {
    private final TagInfo tagInfo;

    public FileMetaAddTagUpdater(TagInfo tagInfo, int majorVersion, int minorVersion) {
        super(tagInfo.getTagName().getTagType().getFileField(), majorVersion, minorVersion);
        this.tagInfo = tagInfo;
    }

    @Override
    public void injectFileUpdater(BSONObject fileTableUpdater) throws ScmServerException {
        BSONObject addToSet = (BSONObject) computeIfAbsent(fileTableUpdater, "$addtoset",
                k -> new BasicBSONObject());
        BasicBSONList tagIdArr = (BasicBSONList) computeIfAbsent(addToSet, getFileField(),
                k -> new BasicBSONList());
        tagIdArr.add(tagInfo.getTagId());
    }

    @Override
    public void injectBucketRelationUpdater(BSONObject bucketRelationUpdater)
            throws ScmServerException {
    }

    @Override
    public void injectDirRelationUpdater(BSONObject dirRelationUpdater) throws ScmServerException {
    }
}

class FileMetaRemoveTagUpdater extends FileMetaUpdater {

    private final TagInfo tagInfo;

    public FileMetaRemoveTagUpdater(TagInfo tagInfo, int majorVersion, int minorVersion) {
        super(tagInfo.getTagName().getTagType().getFileField(), majorVersion, minorVersion);
        this.tagInfo = tagInfo;
    }

    @Override
    public void injectFileUpdater(BSONObject fileTableUpdater) throws ScmServerException {
        BSONObject pullAll = (BSONObject) computeIfAbsent(fileTableUpdater, "$pull_all",
                k -> new BasicBSONObject());
        BasicBSONList tagIdArr = (BasicBSONList) computeIfAbsent(pullAll, getFileField(),
                k -> new BasicBSONList());
        tagIdArr.add(tagInfo.getTagId());
    }

    @Override
    public void injectBucketRelationUpdater(BSONObject bucketRelationUpdater)
            throws ScmServerException {
    }

    @Override
    public void injectDirRelationUpdater(BSONObject dirRelationUpdater) throws ScmServerException {
    }
}