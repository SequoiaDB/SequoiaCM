package com.sequoiacm.client.element.bizconf;

import com.sequoiacm.client.exception.ScmInvalidArgumentException;
import com.sequoiacm.common.FieldName;
import org.bson.BSONObject;
import org.bson.BasicBSONObject;

/**
 * Tag lib option.
 */
public class ScmTagLibMetaOption {
    private String tagLibDomain;

    /**
     * Create a tag lib option with specified arg.
     * 
     * @param tagLibDomain
     *            tag lib domain.
     */
    public ScmTagLibMetaOption(String tagLibDomain) {
        this.tagLibDomain = tagLibDomain;
    }

    /**
     * Get tag lib domain.
     * 
     * @return tag lib domain.
     */
    public String getTagLibDomain() {
        return tagLibDomain;
    }

    /**
     * Set tag lib domain.
     * 
     * @param tagLibDomain
     *            tag lib domain.
     */
    public void setTagLibDomain(String tagLibDomain) throws ScmInvalidArgumentException {
        if (tagLibDomain == null) {
            throw new ScmInvalidArgumentException("tagLibDomain is null");
        }
        this.tagLibDomain = tagLibDomain;
    }

    @Override
    public String toString() {
        return "ScmTagLibOption{" + "tagLibDomain='" + tagLibDomain + '\'' + '}';
    }

    BSONObject getBSON() {
        return new BasicBSONObject(FieldName.FIELD_CLWORKSPACE_TAG_LIB_META_OPTION_DOMAIN,
                tagLibDomain);
    }
}
