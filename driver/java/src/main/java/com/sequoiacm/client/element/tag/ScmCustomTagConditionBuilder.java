package com.sequoiacm.client.element.tag;

import com.sequoiacm.client.exception.ScmInvalidArgumentException;
import com.sequoiacm.common.FieldName;
import org.bson.BSONObject;
import org.bson.BasicBSONObject;
import org.bson.types.BasicBSONList;

import java.util.ArrayList;
import java.util.List;

/**
 * Builder for {@link ScmTagCondition}.
 */
public class ScmCustomTagConditionBuilder {
    private List<BSONObject> matcherList = new ArrayList<BSONObject>();

    ScmCustomTagConditionBuilder() {
    }

    /**
     * File custom tag must be contained the tag key and tag value.
     * 
     * @param tagKey
     *            tag key
     * @param tagValue
     *            tag value
     * @return this
     * @throws ScmInvalidArgumentException
     *             if tag key or tag value is null or empty.
     */
    public ScmCustomTagConditionBuilder contains(String tagKey, String tagValue)
            throws ScmInvalidArgumentException {
        return contains(tagKey, tagValue, false, false);
    }

    /**
     * File custom tag must be contained the tag key and tag value.
     * 
     * @param tagKey
     *            tag key
     * @param tagValue
     *            tag value
     * @param valueIgnoreCase
     *            if true, tag value ignore case
     * @param enabledWildcard
     *            if true, tag value support wildcard: * and ?
     * @return this
     * @throws ScmInvalidArgumentException
     *             if tag key or tag value is null or empty.
     */
    public ScmCustomTagConditionBuilder contains(String tagKey, String tagValue,
            boolean valueIgnoreCase, boolean enabledWildcard) throws ScmInvalidArgumentException {
        matcherList.add(
                createBsonMatcher("$contains", tagKey, tagValue, valueIgnoreCase, enabledWildcard));
        return this;
    }

    /**
     * File custom tag must be not contained the tag key and tag value.
     * 
     * @param tagKey
     *            tag key
     * @param tagValue
     *            tag value
     * @param valueIgnoreCase
     *            if true, tag value ignore case
     * @param enabledWildcard
     *            if true, tag value support wildcard: * and ?
     * @return this
     * @throws ScmInvalidArgumentException
     *             if tag key or tag value is null or empty.
     */
    public ScmCustomTagConditionBuilder notContains(String tagKey, String tagValue,
            boolean valueIgnoreCase, boolean enabledWildcard) throws ScmInvalidArgumentException {
        matcherList.add(createBsonMatcher("$not_contains", tagKey, tagValue, valueIgnoreCase,
                enabledWildcard));
        return this;
    }

    /**
     * File custom tag must be not contained the tag key and tag value.
     * 
     * @param tagKey
     *            tag key
     * @param tagValue
     *            tag value
     * @return this
     * @throws ScmInvalidArgumentException
     *             if tag key or tag value is null or empty.
     */
    public ScmCustomTagConditionBuilder notContains(String tagKey, String tagValue)
            throws ScmInvalidArgumentException {
        return notContains(tagKey, tagValue, false, false);
    }

    /**
     * Build {@link ScmTagCondition}.
     * 
     * @return {@link ScmTagCondition}
     * @throws ScmInvalidArgumentException
     *             if no custom tag is specified.
     */
    public ScmTagCondition build() throws ScmInvalidArgumentException {
        if (matcherList.size() == 0) {
            throw new ScmInvalidArgumentException("no custom tag is specified.");
        }

        if (matcherList.size() == 1) {
            return new ScmTagCondition(matcherList.get(0));
        }
        return new ScmTagCondition(new BasicBSONObject("$and", matcherList));
    }

    private BSONObject createBsonMatcher(String matcherKey, String tagKey, String tagValue,
            boolean valueIgnoreCase, boolean enabledWildcard) throws ScmInvalidArgumentException {
        if (tagKey == null) {
            throw new ScmInvalidArgumentException("tag key is null or empty: " + tagKey);
        }

        if (tagValue == null) {
            throw new ScmInvalidArgumentException("tag value is null or empty: " + tagValue);
        }

        BasicBSONObject matcher = new BasicBSONObject();
        matcher.put(matcherKey, new BasicBSONObject(tagKey, tagValue));
        matcher.put("$ignore_case", valueIgnoreCase);
        matcher.put("$enable_wildcard", enabledWildcard);
        return new BasicBSONObject(FieldName.FIELD_CLFILE_CUSTOM_TAG, matcher);
    }
}
