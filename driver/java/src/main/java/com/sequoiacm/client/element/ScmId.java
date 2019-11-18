package com.sequoiacm.client.element;

import com.sequoiacm.client.exception.ScmException;
import com.sequoiacm.exception.ScmError;

/**
 * Immutable class that represents GUID values. This class can be constructed
 * form a String.
 *
 * @since 2.1
 */
public class ScmId {

    private final int TOTAL_ID_LENGTH = 24;
    private String id;

    /**
     * Constructs an ID from a string representing its GUID value.
     *
     * @param id
     *            GUID value in string format from which to construct an ID.
     * @throws ScmException
     *             If error happens
     * @since 2.1
     */
    public ScmId(String id) throws ScmException {
        this(id, true);
    }

    public ScmId(String id, boolean isNeedCheck) throws ScmException {
        if(isNeedCheck) {
            if (!isValid(id)) {
                throw new ScmException(ScmError.INVALID_ID, "invalid id string:" + id);
            }
        }
        this.id = id;
    }

    /**
     * Sets the value of the Id property.
     *
     * @param id
     *            GUID value in string format from which to construct an ID.
     * @since 2.1
     */
    public void setId(String id) {
        this.id = id;
    }

    /**
     * Returns the string representation of this ID.
     *
     * @return the string representation of this ID
     * @since 2.1
     */
    public String get() {
        return id;
    }

    /**
     * Compares two IDs for logical equality.
     *
     * @param o
     *            Other scmid
     * @since 2.1
     */
    @Override
    public boolean equals(Object o) {
        if (o instanceof ScmId) {
            ScmId other = (ScmId) o;
            return this.id.equals(other.id);
        }
        return false;
    }

    private boolean isValid(String s) {
        if (s == null) {
            return false;
        }

        final int len = s.length();
        if (len != TOTAL_ID_LENGTH) {
            return false;
        }

        for (int i = 0; i < len; i++) {
            char c = s.charAt(i);
            if (c >= '0' && c <= '9') {
                continue;
            }

            if (c >= 'a' && c <= 'f') {
                continue;
            }

            if (c >= 'A' && c <= 'F') {
                continue;
            }

            return false;
        }
        return true;
    }

    @Override
    public String toString() {
        return id;
    }
}
