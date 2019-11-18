package com.sequoiacm.client.element.metadata;

import org.bson.BSONObject;
import org.bson.BasicBSONObject;

/**
 * Supper class of attribute role.
 *
 */
public abstract class ScmAttrRule {

    protected BSONObject rule;

    /**
     * Create a instance of ScmAttrRule.
     */
    public ScmAttrRule() {
        rule = new BasicBSONObject();
    }

    /**
     * Sets a rule with specified key and value.
     *
     * @param key
     *            key.
     * @param value
     *            value.
     */
    protected void setRule(String key, Object value) {
        rule.put(key, value);
    }

    /**
     * To string format.
     *
     * @return string format rule.
     */
    public String toStringFormat() {
        return rule.toString();
    }

    /**
     * Transform to Bson object.
     *
     * @return bson.
     */
    public BSONObject toBSONObject() {
        return rule;
    }

    @Override
    public String toString() {
        return toStringFormat();
    }
}
