package com.sequoiacm.client.element;

import java.util.HashSet;
import java.util.Set;

import com.alibaba.fastjson.JSON;
import com.sequoiacm.client.exception.ScmException;
import com.sequoiacm.exception.ScmError;

/**
 * Class of ScmTags.
 *
 * @since 2.1
 */
public class ScmTags {
    private Set<String> tags = new HashSet<String>();

    /**
     * Add tags.
     *
     * @param tags
     *            tag set
     * @throws ScmException
     * @since 2.1
     */
    public void addTags(Set<String> tags) throws ScmException {
        if (null != tags) {
            for (String tag : tags) {
                addTag(tag);
            }
        }
    }

    /**
     * Add tag.
     *
     * @param tag
     * @throws ScmException
     * 
     * @since 2.1
     */
    public void addTag(String tag) throws ScmException {
        checkTag(tag);
        tags.add(tag);
    }

    /**
     * transform tags set.
     *
     * @return tags set
     * 
     * @since 2.1
     */
    public Set<String> toSet() {
        return new HashSet<String>(tags);
    }

    /**
     * remove tag.
     *
     * @param tag
     * 
     * @since 2.1
     */
    public void removeTag(String tag) {
        tags.remove(tag);
    }

    /**
     * Is contains specified tag.
     *
     * @param tag.
     * 
     * @return true or false.
     */
    public boolean contains(String tag) {
        return tags.contains(tag);
    }

    @Override
    public String toString() {
        return JSON.toJSONString(tags);
    }

    private void checkTag(String tag) throws ScmException {
        if (tag == null || tag.length() == 0) {
            throw new ScmException(ScmError.INVALID_ARGUMENT, "tag can not be set null or empty");
        }
    }
}
