package com.sequoiacm.client.element.metadata;

import org.bson.BSONObject;

import com.sequoiacm.client.exception.ScmException;
import com.sequoiacm.common.MetaDataDefine;
import com.sequoiacm.exception.ScmError;

/**
 * Scm double value check rule.
 */
public class ScmDoubleRule extends ScmAttrRule {

    public final static String KEY_MIN = MetaDataDefine.CheckRuleName.MIN;
    public final static String KEY_MAX = MetaDataDefine.CheckRuleName.MAX;

    /**
     * Create a double value check rule.
     */
    public ScmDoubleRule() {
        setMinimum(Double.MIN_NORMAL);
        setMaximum(Double.MAX_VALUE);
    }

    /**
     * Create a double value check rule with specified max and min.
     *
     * @param min
     *            min.
     * @param max
     *            max.
     */
    public ScmDoubleRule(double min, double max) {
        setMinimum(min);
        setMaximum(max);
    }

    /**
     * Create a double value check rule with specified bson object.
     *
     * @param rule
     *            a bson containing information about doubel value check rule.
     * @throws ScmException
     *             if error happens.
     */
    public ScmDoubleRule(BSONObject rule) throws ScmException {
        this();
        Object obj = null;
        String key = null;
        try {
            if (rule.containsField(KEY_MIN)) {
                key = KEY_MIN;
                obj = rule.get(KEY_MIN);
                String min = String.valueOf(obj);
                setMinimum(Double.parseDouble(min));
            }
            if (rule.containsField(KEY_MAX)) {
                key = KEY_MAX;
                obj = rule.get(KEY_MAX);
                String max = String.valueOf(obj);
                setMaximum(Double.parseDouble(max));
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
    public void setMinimum(double min) {
        setRule(KEY_MIN, min);
    }

    /**
     * Sets maximum
     *
     * @param max
     *            max.
     */
    public void setMaximum(double max) {
        setRule(KEY_MAX, max);
    }

    /**
     * Gets minimum.
     *
     * @return min.
     */
    public double getMinimun() {
        Object obj = rule.get(KEY_MIN);
        String min = String.valueOf(obj);
        return Double.parseDouble(min);
    }

    /**
     * Gets maximum.
     *
     * @return max.
     */
    public double getMaximun() {
        Object obj = rule.get(KEY_MAX);
        String max = String.valueOf(obj);
        return Double.parseDouble(max);
    }
}
