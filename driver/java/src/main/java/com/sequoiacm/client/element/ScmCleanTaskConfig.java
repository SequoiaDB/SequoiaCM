package com.sequoiacm.client.element;

import com.sequoiacm.common.FieldName;
import org.bson.BSONObject;

/**
 * Config for clean task.
 */
public class ScmCleanTaskConfig extends ScmFileTaskConfigBase {

    private boolean isRecycleSpace;

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
        return "ScmCleanTaskConfig{" + "isRecycleSpace=" + isRecycleSpace + "} " + super.toString();
    }

    @Override
    public BSONObject getBSONObject() {
        BSONObject bsonObject = super.getBSONObject();
        bsonObject.put(FieldName.Task.FIELD_OPTION_IS_RECYCLE_SPACE, isRecycleSpace);
        return bsonObject;
    }
}
