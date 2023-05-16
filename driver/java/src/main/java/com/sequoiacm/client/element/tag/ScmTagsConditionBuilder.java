package com.sequoiacm.client.element.tag;

import com.sequoiacm.client.exception.ScmInvalidArgumentException;
import com.sequoiacm.common.FieldName;
import org.bson.BSONObject;
import org.bson.BasicBSONObject;

import java.util.ArrayList;
import java.util.List;

/**
 * Builder for {@link ScmTagCondition}.
 */
public class ScmTagsConditionBuilder {
    private List<BSONObject> matcherList = new ArrayList<BSONObject>();

    ScmTagsConditionBuilder() {
    }

    /**
     * File must be contained the tag.
     * 
     * @param tag
     *            tag
     * @return this
     * @throws ScmInvalidArgumentException
     *             tag is null or empty.
     */
    public ScmTagsConditionBuilder contains(String tag) throws ScmInvalidArgumentException {
        matcherList.add(createBsonMatcher("$contains", tag, false, false));
        return this;
    }

    /**
     * File must be contained the tag.
     * 
     * @param tag
     *            tag
     * @param ignoreCase
     *            if true, tag ignore case
     * @param enabledWildcard
     *            if true, tag support wildcard: * and ?
     * @return this
     * @throws ScmInvalidArgumentException
     *             tag is null or empty.
     */
    public ScmTagsConditionBuilder contains(String tag, boolean ignoreCase, boolean enabledWildcard)
            throws ScmInvalidArgumentException {
        matcherList.add(createBsonMatcher("$contains", tag, ignoreCase, enabledWildcard));
        return this;
    }

    /**
     * File must be not contained the tag.
     * 
     * @param tag
     *            tag
     * @return this
     * @throws ScmInvalidArgumentException
     *             tag is null or empty.
     */
    public ScmTagsConditionBuilder notContains(String tag) throws ScmInvalidArgumentException {
        matcherList.add(createBsonMatcher("$not_contains", tag, false, false));
        return this;
    }

    /**
     * File must be not contained the tag.
     * 
     * @param tag
     *            tag
     * @param ignoreCase
     *            if true, tag ignore case
     * @param enabledWildcard
     *            if true, tag support wildcard: * and ?
     * @return this
     * @throws ScmInvalidArgumentException
     *             tag is null or empty.
     */
    public ScmTagsConditionBuilder notContains(String tag, boolean ignoreCase,
            boolean enabledWildcard) throws ScmInvalidArgumentException {
        matcherList.add(createBsonMatcher("$not_contains", tag, ignoreCase, enabledWildcard));
        return this;
    }

    /**
     * Build {@link ScmTagCondition}.
     *
     * @return {@link ScmTagCondition}
     * @throws ScmInvalidArgumentException
     *             if no tag is specified.
     */
    public ScmTagCondition build() throws ScmInvalidArgumentException {
        if (matcherList.size() == 0) {
            throw new ScmInvalidArgumentException("no tag is specified.");
        }

        if (matcherList.size() == 1) {
            return new ScmTagCondition(matcherList.get(0));
        }
        return new ScmTagCondition(new BasicBSONObject("$and", matcherList));
    }

    private BSONObject createBsonMatcher(String matcherKey, String tag, boolean valueIgnoreCase,
            boolean enabledWildcard) throws ScmInvalidArgumentException {
        if (tag == null || tag.isEmpty()) {
            throw new ScmInvalidArgumentException("tag is null or empty");
        }
        BasicBSONObject matcher = new BasicBSONObject();
        matcher.put(matcherKey, tag);
        matcher.put("$ignore_case", valueIgnoreCase);
        matcher.put("$enable_wildcard", enabledWildcard);
        return new BasicBSONObject(FieldName.FIELD_CLFILE_TAGS, matcher);
    }
}
