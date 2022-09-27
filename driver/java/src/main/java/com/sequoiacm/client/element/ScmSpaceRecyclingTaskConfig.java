package com.sequoiacm.client.element;

import com.sequoiacm.client.core.ScmWorkspace;
import com.sequoiacm.common.FieldName;
import org.bson.BSONObject;
import org.bson.BasicBSONObject;

/**
 * Config for space recycling task.
 */
public class ScmSpaceRecyclingTaskConfig implements ScmTaskConfig {


    private ScmWorkspace workspace;

    private ScmSpaceRecycleScope recycleScope;

    private long maxExecTime;

    /**
     * Get the workspace object.
     * 
     * @return the workspace object.
     */
    public ScmWorkspace getWorkspace() {
        return workspace;
    }

    /**
     * Set the workspace object.
     * 
     * @param workspace
     *            the workspace object.
     */
    public void setWorkspace(ScmWorkspace workspace) {
        this.workspace = workspace;
    }

    /**
     * Get the recycle scope.
     * 
     * @return the recycle scope.
     */
    public ScmSpaceRecycleScope getRecycleScope() {
        return recycleScope;
    }

    /**
     * Set the recycle scope.
     * 
     * @param recycleScope
     *            the recycle scope.
     */
    public void setRecycleScope(ScmSpaceRecycleScope recycleScope) {
        this.recycleScope = recycleScope;
    }

    /**
     * Get the max exec time of the task(unit: ms).
     * 
     * @return the max exec time of the task.
     */
    public long getMaxExecTime() {
        return maxExecTime;
    }

    /**
     * Set the max exec time of the task(unit: ms).
     * 
     * @param maxExecTime
     *            the max exec time of the task.
     */
    public void setMaxExecTime(long maxExecTime) {
        this.maxExecTime = maxExecTime;
    }

    @Override
    public String toString() {
        return "ScmSpaceRecyclingTaskConfig{" + "workspace=" + workspace + ", recycleScope="
                + recycleScope + ", maxExecTime=" + maxExecTime + '}';
    }

    @Override
    public BSONObject getBSONObject() {
        BSONObject bsonObject = new BasicBSONObject();
        bsonObject.put(FieldName.Task.FIELD_OPTION_RECYCLE_SCOPE,
                recycleScope == null ? null : recycleScope.getScope());
        bsonObject.put(FieldName.Task.FIELD_MAX_EXEC_TIME, maxExecTime);
        bsonObject.put(FieldName.Task.FIELD_WORKSPACE,
                workspace == null ? null : workspace.getName());
        return bsonObject;
    }
}
