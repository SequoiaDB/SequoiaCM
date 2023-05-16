package com.sequoiacm.common.module;

import com.sequoiacm.common.FieldName;
import com.sequoiacm.infrastructure.common.BsonUtils;
import org.bson.BSONObject;

public class TagName {
    private TagType tagType;

    // tags
    private String tag;

    // customTag
    private String tagKey;
    private String tagValue;

    public static TagName tags(String tag) {
        TagName tagName = new TagName();
        tagName.setTagType(TagType.TAGS);
        tagName.setTag(tag);
        return tagName;
    }

    public static TagName customTag(String tagKey, String tagValue) {
        TagName tagName = new TagName();
        tagName.setTagType(TagType.CUSTOM_TAG);
        tagName.setTagKey(tagKey);
        tagName.setTagValue(tagValue);
        return tagName;
    }

    public static TagName fromRecord(BSONObject record) {
        TagName tagName = new TagName();
        String tagTypeStr = BsonUtils.getStringChecked(record, FieldName.TagLib.TAG_TYPE);
        tagName.setTagType(TagType.fromFileField(tagTypeStr));
        if (tagName.getTagType() == TagType.TAGS) {
            tagName.setTag(BsonUtils.getStringChecked(record, FieldName.TagLib.TAG));
        }
        else if (tagName.getTagType() == TagType.CUSTOM_TAG) {
            BSONObject customTag = BsonUtils.getBSONChecked(record, FieldName.TagLib.CUSTOM_TAG);
            tagName.setTagKey(
                    BsonUtils.getStringChecked(customTag, FieldName.TagLib.CUSTOM_TAG_TAG_KEY));
            tagName.setTagValue(
                    BsonUtils.getStringChecked(customTag, FieldName.TagLib.CUSTOM_TAG_TAG_VALUE));
        }
        else {
            throw new IllegalArgumentException("unknown tag type: " + record);
        }
        return tagName;
    }

    private TagName() {
    }

    @Override
    public String toString() {
        if (tagType == TagType.CUSTOM_TAG) {
            return "{" + tagKey + ": \"" + tagValue + "\"}";
        }
        else if (tagType == TagType.TAGS) {
            return "\"" + tag + "\"";
        }
        else {
            return "TagName{" + "tagType=" + tagType + ", tag='" + tag + '\'' + ", tagKey='"
                    + tagKey + '\'' + ", tagValue='" + tagValue + '\'' + '}';
        }
    }

    public TagType getTagType() {
        return tagType;
    }

    public void setTagType(TagType tagType) {
        this.tagType = tagType;
    }

    public String getTag() {
        return tag;
    }

    public void setTag(String tag) {
        this.tag = tag;
    }

    public String getTagKey() {
        return tagKey;
    }

    public void setTagKey(String tagKey) {
        this.tagKey = tagKey;
    }

    public String getTagValue() {
        return tagValue;
    }

    public void setTagValue(String tagValue) {
        this.tagValue = tagValue;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o)
            return true;
        if (o == null || getClass() != o.getClass())
            return false;

        TagName tagName = (TagName) o;

        if (tagType != tagName.tagType)
            return false;
        if (tag != null ? !tag.equals(tagName.tag) : tagName.tag != null)
            return false;
        if (tagKey != null ? !tagKey.equals(tagName.tagKey) : tagName.tagKey != null)
            return false;
        return tagValue != null ? tagValue.equals(tagName.tagValue) : tagName.tagValue == null;
    }

    @Override
    public int hashCode() {
        int result = tagType != null ? tagType.hashCode() : 0;
        result = 31 * result + (tag != null ? tag.hashCode() : 0);
        result = 31 * result + (tagKey != null ? tagKey.hashCode() : 0);
        result = 31 * result + (tagValue != null ? tagValue.hashCode() : 0);
        return result;
    }
}
