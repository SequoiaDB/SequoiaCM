package com.sequoiacm.client.element;

import com.sequoiacm.common.FieldName;
import org.bson.BSONObject;

/**
 * Config for move task.
 */
public class ScmMoveTaskConfig extends ScmFileTaskConfigBase {

    private boolean isRecycleSpace;

    private String targetSite;

    /**
     * Get the target site name for move.
     * 
     * @return the target site name.
     */
    public String getTargetSite() {
        return targetSite;
    }

    /**
     * Set the target site name for move.
     * 
     * @param targetSite
     *            the target site name.
     */
    public void setTargetSite(String targetSite) {
        this.targetSite = targetSite;
    }

    /**
     * Is need recycle space.
     *
     * @return Is need recycle space.
     */
    public boolean isRecycleSpace() {
        return isRecycleSpace;
    }

    /**
     * Set need recycle space.
     *
     * @param recycleSpace
     *            Is need recycle space.
     */
    public void setRecycleSpace(boolean recycleSpace) {
        isRecycleSpace = recycleSpace;
    }

    @Override
    public String toString() {
        return "ScmMoveTaskConfig{" + "isRecycleSpace=" + isRecycleSpace + ", targetSite='"
                + targetSite + '\'' + "} " + super.toString();
    }

    @Override
    public BSONObject getBSONObject() {
        BSONObject bsonObject = super.getBSONObject();
        bsonObject.put(FieldName.Task.FIELD_OPTION_IS_RECYCLE_SPACE, isRecycleSpace);
        bsonObject.put(FieldName.Task.FIELD_TARGET_SITE, targetSite);
        return bsonObject;
    }
}
