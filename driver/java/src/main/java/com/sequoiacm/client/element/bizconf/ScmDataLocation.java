package com.sequoiacm.client.element.bizconf;

import org.bson.BSONObject;

import com.sequoiacm.client.exception.ScmInvalidArgumentException;

/**
 * Super class of all data location.
 */
public abstract class ScmDataLocation extends ScmLocation {

    /**
     * Create a data location with specified args.
     *
     * @param siteName
     *            site name.
     * @throws ScmInvalidArgumentException
     *             if error happens.
     */
    public ScmDataLocation(String siteName) throws ScmInvalidArgumentException {
        super(siteName);
    }

    /**
     * Create a data location with specified arg.
     *
     * @param obj
     *            a bson containing information about data location.
     * @throws ScmInvalidArgumentException
     *             if error happens.
     */
    public ScmDataLocation(BSONObject obj) throws ScmInvalidArgumentException {
        super(obj);
    }

    @Override
    public boolean equals(Object obj) {
        if (obj instanceof ScmDataLocation) {
            ScmDataLocation right = (ScmDataLocation) obj;
            if (getType() == right.getType()) {
                return getBSONObject().equals(right.getBSONObject());
            }
        }
        return false;

    }
}
