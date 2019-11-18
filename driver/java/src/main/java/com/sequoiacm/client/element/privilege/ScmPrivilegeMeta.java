package com.sequoiacm.client.element.privilege;

import org.bson.BSONObject;

import com.sequoiacm.client.exception.ScmException;
import com.sequoiacm.client.util.BsonUtils;
import com.sequoiacm.common.FieldName;

/**
 * Scm privilege meta.
 */
public class ScmPrivilegeMeta {
    private int version;

    /**
     * Create a instance of ScmPrivilegeMeta.
     *
     * @param obj
     *            a bson containing information about ScmprivilegeMeta.
     * @throws ScmException
     *             if error happens.
     */
    public ScmPrivilegeMeta(BSONObject obj) throws ScmException {
        version = BsonUtils.getIntegerChecked(obj, FieldName.Privilege.FIELD_META_VERSION);
    }

    /**
     * Gets the version.
     *
     * @return version.
     */
    public int getVersion() {
        return version;
    }
}
