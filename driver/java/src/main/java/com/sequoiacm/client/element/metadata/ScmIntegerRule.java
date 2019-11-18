package com.sequoiacm.client.element.metadata;

import org.bson.BSONObject;

import com.sequoiacm.client.exception.ScmException;
import com.sequoiacm.common.MetaDataDefine;
import com.sequoiacm.exception.ScmError;

/**
 * Scm interger value check rule.
 *
 */
public class ScmIntegerRule extends ScmAttrRule {

    public static final String KEY_MIN = MetaDataDefine.CheckRuleName.MIN;
    public static final String KEY_MAX = MetaDataDefine.CheckRuleName.MAX;

    /**
     * Create integer value check rule.
     */
    public ScmIntegerRule() {
        setMinimum(Integer.MIN_VALUE);
        setMaximum(Integer.MAX_VALUE);
    }

    /**
     * Create integer value check rule with specified maximum and minimum.
     *
     * @param min
     *            min.
     * @param max
     *            max.
     */
    public ScmIntegerRule(int min, int max) {
        setMinimum(min);
        setMaximum(max);
    }

    /**
     * Create a integer value check rule with specified bson object.
     *
     * @param rule
     *            a bson containing information about integer value check rule.
     * @throws ScmException
     *             if error happens.
     */
    public ScmIntegerRule(BSONObject rule) throws ScmException {
        this();
        Object obj = null;
        String key = null;
        try {
            if (rule.containsField(KEY_MIN)) {
                key = KEY_MIN;
                obj = rule.get(KEY_MIN);
                String min = String.valueOf(obj);
                setMinimum(Integer.parseInt(min));
            }
            if (rule.containsField(KEY_MAX)) {
                key = KEY_MAX;
                obj = rule.get(KEY_MAX);
                String max = String.valueOf(obj);
                setMaximum(Integer.parseInt(max));
            }
        }
        catch (Exception e) {
            throw new ScmException(ScmError.INVALID_ARGUMENT,
                    "invalid rule value: " + key + "=" + obj);
        }
    }

    /**
     * Sets minimum.
     *
     * @param min
     *            min.
     */
    public void setMinimum(int min) {
        setRule(KEY_MIN, min);
    }

    /**
     * Sets maximum.
     *
     * @param max
     *            max.
     */
    public void setMaximum(int max) {
        setRule(KEY_MAX, max);
    }

    /**
     * Gets the minimum.
     *
     * @return min.
     */
    public int getMinimum() {
        Object obj = rule.get(KEY_MIN);
        String min = String.valueOf(obj);
        return Integer.parseInt(min);
    }

    /**
     * Gets the maximum.
     *
     * @return max.
     */
    public int getMaximum() {
        Object obj = rule.get(KEY_MAX);
        String max = String.valueOf(obj);
        return Integer.parseInt(max);
    }
}
