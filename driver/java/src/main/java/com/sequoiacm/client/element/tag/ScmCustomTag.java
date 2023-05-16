package com.sequoiacm.client.element.tag;

import com.sequoiacm.client.exception.ScmException;
import com.sequoiacm.client.util.BsonUtils;
import com.sequoiacm.common.FieldName;
import org.bson.BSONObject;

/**
 * The information of ScmCustomTag.
 */
public class ScmCustomTag {
    private final String key;
    private final String value;
    private final long id;

    /**
     * Create ScmCustomTag, for internal use.
     * 
     * @param obj
     *            a bson containing basic information about scm custom tag.
     * @throws ScmException
     *             if error happens.
     */
    public ScmCustomTag(BSONObject obj) throws ScmException {
        BSONObject keyValue = BsonUtils.getBSONObjectChecked(obj, FieldName.TagLib.CUSTOM_TAG);
        key = BsonUtils.getStringChecked(keyValue, FieldName.TagLib.CUSTOM_TAG_TAG_KEY);
        value = BsonUtils.getStringChecked(keyValue, FieldName.TagLib.CUSTOM_TAG_TAG_VALUE);
        id = BsonUtils.getNumberChecked(obj, FieldName.TagLib.TAG_ID).longValue();
    }

    /**
     * Get key of custom tag.
     * 
     * @return key of custom tag.
     */
    public String getKey() {
        return key;
    }

    /**
     * Get value of custom tag.
     * 
     * @return value of custom tag.
     */
    public String getValue() {
        return value;
    }

    /**
     * Get id of custom tag.
     * 
     * @return id of custom tag.
     */
    public long getId() {
        return id;
    }

    @Override
    public String toString() {
        return "ScmCustomTag{" + "key='" + key + '\'' + ", value='" + value + '\'' + ", id=" + id
                + '}';
    }
}
