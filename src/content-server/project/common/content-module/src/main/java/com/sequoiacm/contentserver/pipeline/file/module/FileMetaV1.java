package com.sequoiacm.contentserver.pipeline.file.module;

import com.sequoiacm.common.FieldName;
import com.sequoiacm.contentserver.bucket.BucketInfoManager;
import com.sequoiacm.contentserver.common.ScmArgumentChecker;
import com.sequoiacm.exception.ScmServerException;
import com.sequoiacm.infrastructure.common.BsonUtils;
import org.bson.BSONObject;
import org.bson.BasicBSONObject;
import org.bson.types.BasicBSONList;

import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;

public class FileMetaV1 extends FileMeta {

    private Set<String> tags = new HashSet<>();
    private Map<String, String> customTag = new TreeMap<>();

    public FileMetaV1(BucketInfoManager bucketInfoManager) {
        super(bucketInfoManager);
    }

    @Override
    public void loadInfoFromRecord(BSONObject record) {
        super.loadBasicInfoFromRecord(record);

        tags = new HashSet<>();
        BasicBSONList tagsBson = BsonUtils.getArray(record, FieldName.FIELD_CLFILE_TAGS);
        if (tagsBson != null) {
            for (Object obj : tagsBson) {
                tags.add((String) obj);
            }
        }

        customTag = new TreeMap<>();
        BSONObject customTagBson = BsonUtils.getBSON(record, FieldName.FIELD_CLFILE_CUSTOM_TAG);
        if (customTag != null) {
            for (String key : customTagBson.keySet()) {
                customTag.put(key, (String) customTagBson.get(key));
            }
        }
    }

    @Override
    public void loadInfoFromUserInfo(String ws, BSONObject userFileInfo, String user,
            boolean checkProps) throws ScmServerException {
        super.loadBasicInfoFromUserInfo(ws, userFileInfo, user, checkProps);

        BSONObject tagsValue = BsonUtils.getBSON(userFileInfo, FieldName.FIELD_CLFILE_TAGS);
        tags = ScmArgumentChecker.checkAndCorrectTagsAsSet(tagsValue);

        BSONObject customTagBson = BsonUtils.getBSON(userFileInfo,
                FieldName.FIELD_CLFILE_CUSTOM_TAG);
        customTag = ScmArgumentChecker.checkAndCorrectCustomTag(customTagBson);
    }

    @Override
    public BSONObject toRecordBSON() {
        BSONObject bson = super.asRecordBSON();
        putTag(bson);
        return bson;
    }

    private void putTag(BSONObject bson) {
        BasicBSONList tagsBson = new BasicBSONList();
        tagsBson.addAll(tags);
        bson.put(FieldName.FIELD_CLFILE_TAGS, tagsBson);

        BasicBSONObject customTagBson = new BasicBSONObject(true);
        customTagBson.putAll(customTag);
        bson.put(FieldName.FIELD_CLFILE_CUSTOM_TAG, customTagBson);
    }

    @Override
    public BSONObject toUserInfoBSON() throws ScmServerException {
        BSONObject bson = super.asUserInfoBson();
        putTag(bson);
        return bson;
    }

    public Set<String> getTags() {
        return tags;
    }

    public void setTags(Set<String> tags) {
        this.tags = tags;
    }

    public Map<String, String> getCustomTag() {
        return customTag;
    }

    public void setCustomTag(Map<String, String> customTag) {
        this.customTag = customTag;
    }

    @Override
    public FileMeta clone() {
        FileMetaV1 clone = (FileMetaV1) super.clone();
        clone.tags = new HashSet<>(tags);
        clone.customTag = new TreeMap<>(customTag);
        return clone;
    }
}
