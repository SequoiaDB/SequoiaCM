package com.sequoiacm.client.element.bizconf;

import org.bson.BSONObject;

import com.sequoiacm.client.exception.ScmInvalidArgumentException;

/**
 * Super class of all meta location.
 */
public abstract class ScmMetaLocation extends ScmLocation {

    /**
     * Create a meta location with specified arg.
     *
     * @param siteName
     *            site name.
     * @throws ScmInvalidArgumentException
     *             if error happens.
     */
    public ScmMetaLocation(String siteName) throws ScmInvalidArgumentException {
        super(siteName);
    }

    /**
     * Create a meta location with specified arg.
     *
     * @param obj
     *            a bson containing information about meta location.
     * @throws ScmInvalidArgumentException
     *             if error happens.
     */
    public ScmMetaLocation(BSONObject obj) throws ScmInvalidArgumentException {
        super(obj);
    }

    @Override
    public boolean equals(Object obj) {
        if (obj instanceof ScmMetaLocation) {
            ScmMetaLocation right = (ScmMetaLocation) obj;
            if (getType() == right.getType()) {
                return getBSONObject().equals(right.getBSONObject());
            }
        }
        return false;
    }

}
