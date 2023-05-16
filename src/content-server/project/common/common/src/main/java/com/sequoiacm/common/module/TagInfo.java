package com.sequoiacm.common.module;

import com.sequoiacm.common.FieldName;
import com.sequoiacm.infrastructure.common.BsonUtils;
import org.bson.BSONObject;
import org.bson.BasicBSONObject;

import java.util.Objects;

public class TagInfo {

    private long tagId;
    private TagName tagName;

    public TagInfo() {
    }

    public TagInfo(BSONObject bsonObject) {
        tagId = BsonUtils.getNumberChecked(bsonObject, FieldName.TagLib.TAG_ID).longValue();
        tagName = TagName.fromRecord(bsonObject);

    }

    public long getTagId() {
        return tagId;
    }

    public void setTagId(long tagId) {
        this.tagId = tagId;
    }

    public TagName getTagName() {
        return tagName;
    }

    public TagType getTagType() {
        return tagName.getTagType();
    }

    public void setTagName(TagName tagName) {
        this.tagName = tagName;
    }

    public BSONObject toBson() {
        BSONObject bsonObject = new BasicBSONObject();
        bsonObject.put(FieldName.TagLib.TAG_ID, tagId);
        bsonObject.put(FieldName.TagLib.TAG_TYPE, tagName.getTagType().getFileField());

        if (getTagType() == TagType.TAGS) {
            bsonObject.put(FieldName.TagLib.TAG, tagName.getTag());
        }
        else if (getTagType() == TagType.CUSTOM_TAG) {
            BasicBSONObject customTag = new BasicBSONObject();
            customTag.put(FieldName.TagLib.CUSTOM_TAG_TAG_KEY, tagName.getTagKey());
            customTag.put(FieldName.TagLib.CUSTOM_TAG_TAG_VALUE, tagName.getTagValue());
            bsonObject.put(FieldName.TagLib.CUSTOM_TAG, customTag);
        }
        else {
            throw new IllegalArgumentException("Unknown tag type: " + this);
        }
        return bsonObject;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        TagInfo tagInfo = (TagInfo) o;

        if (tagId != tagInfo.tagId) return false;
        return tagName != null ? tagName.equals(tagInfo.tagName) : tagInfo.tagName == null;
    }

    @Override
    public int hashCode() {
        int result = (int) (tagId ^ (tagId >>> 32));
        result = 31 * result + (tagName != null ? tagName.hashCode() : 0);
        return result;
    }

    @Override
    public String toString() {
        return "TagInfo{" + "tagId=" + tagId + ", tagName=" + tagName + '}';
    }
}
