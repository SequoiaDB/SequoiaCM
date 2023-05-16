package com.sequoiacm.client.element.tag;

import com.sequoiacm.client.exception.ScmException;
import com.sequoiacm.client.util.BsonUtils;
import com.sequoiacm.common.FieldName;
import org.bson.BSONObject;

/**
 * The information of ScmTag.
 */
public class ScmTag {
    private final String name;
    private final long id;

    /**
     * Create ScmTag, for internal use.
     * 
     * @param b
     *            a bson containing basic information about scm tag.
     * @throws ScmException
     *             if error happens.
     */
    public ScmTag(BSONObject b) throws ScmException {
        id = BsonUtils.getNumberChecked(b, FieldName.TagLib.TAG_ID).longValue();
        name = BsonUtils.getStringChecked(b, FieldName.TagLib.TAG);
    }

    /**
     * Get name of tag.
     * 
     * @return
     */
    public String getName() {
        return name;
    }

    /**
     * Get id of tag.
     * 
     * @return
     */
    public long getId() {
        return id;
    }

    @Override
    public String toString() {
        return "ScmTag{" + "name='" + name + '\'' + ", id=" + id + '}';
    }
}
