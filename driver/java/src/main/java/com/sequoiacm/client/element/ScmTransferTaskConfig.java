package com.sequoiacm.client.element;

import com.sequoiacm.common.FieldName;
import org.bson.BSONObject;

/**
 * Config for transfer task.
 */
public class ScmTransferTaskConfig extends ScmFileTaskConfigBase {

    private String targetSite;

    /**
     * Get the target site name for transfer.
     *
     * @return the target site name.
     */
    public String getTargetSite() {
        return targetSite;
    }

    /**
     * Set the target site name for transfer.
     *
     * @param targetSite
     *            the target site name.
     */
    public void setTargetSite(String targetSite) {
        this.targetSite = targetSite;
    }

    @Override
    public String toString() {
        return "ScmTransferTaskConfig{" + "targetSite='" + targetSite + '\'' + "} "
                + super.toString();
    }

    @Override
    public BSONObject getBSONObject() {
        BSONObject bsonObject = super.getBSONObject();
        bsonObject.put(FieldName.Task.FIELD_TARGET_SITE, targetSite);
        return bsonObject;
    }
}
