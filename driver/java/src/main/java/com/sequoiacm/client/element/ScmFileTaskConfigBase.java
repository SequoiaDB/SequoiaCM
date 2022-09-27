package com.sequoiacm.client.element;

import com.sequoiacm.client.common.ScmDataCheckLevel;
import com.sequoiacm.client.common.ScmType;
import com.sequoiacm.client.core.ScmWorkspace;
import com.sequoiacm.client.exception.ScmException;
import com.sequoiacm.client.exception.ScmInvalidArgumentException;
import com.sequoiacm.common.FieldName;
import org.bson.BSONObject;
import org.bson.BasicBSONObject;

/**
 * Task config base.
 */
public abstract class ScmFileTaskConfigBase implements ScmTaskConfig {

    private ScmWorkspace workspace;

    private BSONObject condition;

    private ScmType.ScopeType scope = ScmType.ScopeType.SCOPE_CURRENT;

    private long maxExecTime;

    private ScmDataCheckLevel dataCheckLevel = ScmDataCheckLevel.WEEK;

    private boolean quickStart;

    /**
     * Get the workspace.
     *
     * @return the workspace object.
     */
    public ScmWorkspace getWorkspace() {
        return workspace;
    }

    /**
     * Set the workspace of the task.
     *
     * @param workspace
     *            the workspace object.
     */
    public void setWorkspace(ScmWorkspace workspace) {
        this.workspace = workspace;
    }

    /**
     * Get the query condition of the task.
     *
     * @return the query condition.
     */
    public BSONObject getCondition() {
        return condition;
    }

    /**
     * Set the query condition of the task.
     *
     * @param condition
     *            the query condition.
     */
    public void setCondition(BSONObject condition) {
        this.condition = condition;
    }

    /**
     * Set the query condition of the task.
     *
     * @param condition
     *            the query condition.
     */
    public void setCondition(ScmFileCondition condition) throws ScmException {
        if (condition == null) {
            throw new ScmInvalidArgumentException("condition is null");
        }
        this.condition = condition.toBSONObject();
    }

    /**
     * Get the scope of the task.
     *
     * @return the scope.
     */
    public ScmType.ScopeType getScope() {
        return scope;
    }

    /**
     * Set the scope of the task.
     *
     * @param scope
     *            the scope.
     */
    public void setScope(ScmType.ScopeType scope) {
        this.scope = scope;
    }

    /**
     * Get the max exec time of the task(unit: ms).
     *
     * @return the max exec time.
     */
    public long getMaxExecTime() {
        return maxExecTime;
    }

    /**
     * Set the max exec time of the task.
     *
     * @param maxExecTime
     *            the max exec time.
     */
    public void setMaxExecTime(long maxExecTime) {
        this.maxExecTime = maxExecTime;
    }

    /**
     * Get the data check level for files.
     *
     * @return the data check level.
     */
    public ScmDataCheckLevel getDataCheckLevel() {
        return dataCheckLevel;
    }

    /**
     * Set the data check level for files.
     *
     * @param dataCheckLevel
     *            the data check level.
     */
    public void setDataCheckLevel(ScmDataCheckLevel dataCheckLevel) {
        this.dataCheckLevel = dataCheckLevel;
    }

    /**
     * Is need quick start.
     *
     * @return Is need quick start.
     */
    public boolean isQuickStart() {
        return quickStart;
    }

    /**
     * Set need quick start.
     *
     * @param quickStart
     *            Is need quick start.
     */
    public void setQuickStart(boolean quickStart) {
        this.quickStart = quickStart;
    }

    @Override
    public String toString() {
        return "ScmFileTaskConfigBase{" + "workspace=" + workspace + ", condition=" + condition
                + ", scope=" + scope + ", maxExecTime=" + maxExecTime + ", dataCheckLevel="
                + dataCheckLevel + ", quickStart=" + quickStart + '}';
    }

    @Override
    public BSONObject getBSONObject() {
        BSONObject bsonObject = new BasicBSONObject();
        bsonObject.put(FieldName.Task.FIELD_WORKSPACE,
                workspace == null ? null : workspace.getName());
        bsonObject.put(FieldName.Task.FIELD_CONTENT, condition);
        bsonObject.put(FieldName.Task.FIELD_SCOPE, scope == null ? null : scope.getScope());
        bsonObject.put(FieldName.Task.FIELD_MAX_EXEC_TIME, maxExecTime);
        bsonObject.put(FieldName.Task.FIELD_OPTION_QUICK_START, quickStart);
        bsonObject.put(FieldName.Task.FIELD_OPTION_DATA_CHECK_LEVEL,
                dataCheckLevel == null ? null : dataCheckLevel.getName());
        return bsonObject;
    }
}
