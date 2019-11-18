package com.sequoiacm.client.element.metadata;

import org.bson.BSONObject;

import com.sequoiacm.client.exception.ScmException;
import com.sequoiacm.common.MetaDataDefine;
import com.sequoiacm.exception.ScmError;

/**
 * Scm string value check rule.
 */
public class ScmStringRule extends ScmAttrRule {

    public static final String KEY_MAXLENGTH = MetaDataDefine.CheckRuleName.MAX_LENGTH;

    /**
     * Create a string value check rule.
     */
    public ScmStringRule() {
        setMaxLength(-1);
    }

    /**
     * Create a string value check rule with specified max string length.
     *
     * @param maxLength
     *            max string length.
     */
    public ScmStringRule(int maxLength) {
        setMaxLength(maxLength);
    }

    /**
     * Create a string value check rule with specified bson object.
     *
     * @param rule
     *            a bson containing information about string check rule.
     * @throws ScmException
     *             if error happens.
     */
    public ScmStringRule(BSONObject rule) throws ScmException {
        this();
        if (rule.containsField(KEY_MAXLENGTH)) {
            Object obj = rule.get(KEY_MAXLENGTH);
            try {
                String maxlen = String.valueOf(obj);
                setMaxLength(Integer.parseInt(maxlen));
            }
            catch (Exception e) {
                throw new ScmException(ScmError.INVALID_ARGUMENT,
                        "invalid rule value: " + KEY_MAXLENGTH + "=" + obj);
            }
        }
    }

    /**
     * Sets the max string length.
     *
     * @param maxLength
     *            max length.
     */
    public void setMaxLength(int maxLength) {
        setRule(KEY_MAXLENGTH, maxLength);
    }

    /**
     * Gets the max string length.
     *
     * @return max string length.
     */
    public int getMaxLength() {
        Object obj = rule.get(KEY_MAXLENGTH);
        String len = String.valueOf(obj);
        return Integer.parseInt(len);
    }
}
