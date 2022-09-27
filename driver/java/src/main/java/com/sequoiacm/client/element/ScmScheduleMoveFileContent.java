package com.sequoiacm.client.element;

import com.sequoiacm.client.common.ScmDataCheckLevel;
import com.sequoiacm.client.common.ScmType;
import com.sequoiacm.client.core.ScmAttributeName;
import com.sequoiacm.client.exception.ScmException;
import com.sequoiacm.client.exception.ScmInvalidArgumentException;
import com.sequoiacm.common.InvalidArgumentException;
import com.sequoiacm.common.ScmArgChecker;
import org.bson.BSONObject;
import org.bson.BasicBSONObject;

/**
 * Schedule move file content.
 */
public class ScmScheduleMoveFileContent implements ScmScheduleContent {
    private String sourceSiteName;
    private String targetSiteName;
    private String maxStayTime;
    private ScmDataCheckLevel dataCheckLevel = ScmDataCheckLevel.WEEK;
    private boolean quickStart;
    private boolean isRecycleSpace;
    private BSONObject extraCondition;
    private ScmType.ScopeType scope = ScmType.ScopeType.SCOPE_CURRENT;
    private long maxExecTime = 0;

    /**
     * 
     * @param sourceSiteName
     *            source site name.
     * @param targetSiteName
     *            target site name.
     * @param maxStayTime
     *            file max stay time.
     * @param extraCondition
     *            file extra condition.
     * @param scope
     *            file scope.
     * 
     */
    public ScmScheduleMoveFileContent(String sourceSiteName, String targetSiteName,
            String maxStayTime, BSONObject extraCondition, ScmType.ScopeType scope)
            throws ScmException {
        this.sourceSiteName = sourceSiteName;
        this.targetSiteName = targetSiteName;
        this.extraCondition = extraCondition;
        this.scope = scope;
        if (scope != ScmType.ScopeType.SCOPE_CURRENT) {
            try {
                ScmArgChecker.File.checkHistoryFileMatcher(extraCondition);
            }
            catch (InvalidArgumentException e) {
                throw new ScmInvalidArgumentException("invalid condition", e);
            }
        }
        this.maxStayTime = maxStayTime;
    }

    /**
     * 
     * @param sourceSiteName
     *            source site name.
     * @param targetSiteName
     *            target site name.
     * @param extraCondition
     *            file extra condition.
     * @param scope
     *            file scope.
     * @param maxStayTime
     *            file max stat time.
     * @param maxExecTime
     *            task max exec time(0 means unlimited).
     * @param dataCheckLevel
     *            file data check level.
     * @param isRecycleSpace
     *            is need recycle space.
     * @param quickStart
     *            is need quick start.
     */
    public ScmScheduleMoveFileContent(String sourceSiteName, String targetSiteName,
            String maxStayTime, BSONObject extraCondition, ScmType.ScopeType scope,
            long maxExecTime, ScmDataCheckLevel dataCheckLevel, boolean isRecycleSpace,
            boolean quickStart) throws ScmException {
        this(sourceSiteName, targetSiteName, maxStayTime, extraCondition, scope);
        this.maxExecTime = maxExecTime;
        this.dataCheckLevel = dataCheckLevel;
        if (dataCheckLevel == null) {
            throw new ScmInvalidArgumentException("dataCheckLevel is null");
        }
        this.isRecycleSpace = isRecycleSpace;
        this.quickStart = quickStart;
    }

    public ScmScheduleMoveFileContent(BSONObject content) throws ScmException {
        Object temp = null;
        temp = content.get(ScmAttributeName.Schedule.CONTENT_MOVE_SOURCE_SITE);
        if (null != temp) {
            setSourceSiteName((String) temp);
        }

        temp = content.get(ScmAttributeName.Schedule.CONTENT_MOVE_TARGET_SITE);
        if (null != temp) {
            setTargetSiteName((String) temp);
        }
        temp = content.get(ScmAttributeName.Schedule.CONTENT_MAX_STAY_TIME);
        if (null != temp) {
            setMaxStayTime((String) temp);
        }

        temp = content.get(ScmAttributeName.Schedule.CONTENT_EXTRA_CONDITION);
        if (null != temp) {
            setExtraCondition((BSONObject) temp);
        }

        temp = content.get(ScmAttributeName.Schedule.CONTENT_MAX_EXEC_TIME);
        if (null != temp) {
            setMaxExecTime(((Number) temp).longValue());
        }

        temp = content.get(ScmAttributeName.Schedule.CONTENT_SCOPE);
        if (null != temp) {
            this.scope = ScmType.ScopeType.getScopeType((Integer) temp);
        }

        temp = content.get(ScmAttributeName.Schedule.CONTENT_QUICK_START);
        if (null != temp) {
            setQuickStart((Boolean) temp);
        }
        temp = content.get(ScmAttributeName.Schedule.CONTENT_IS_RECYCLE_SPACE);
        if (null != temp) {
            setRecycleSpace((Boolean) temp);
        }
        temp = content.get(ScmAttributeName.Schedule.CONTENT_DATA_CHECK_LEVEL);
        if (null != temp) {
            setDataCheckLevel(ScmDataCheckLevel.getType((String) temp));
        }
    }

    /**
     * Get source site name.
     *
     * @return source site name.
     */
    public String getSourceSiteName() {
        return sourceSiteName;
    }

    /**
     * Set source site name.
     *
     * @param sourceSiteName
     *            source site name.
     */
    public void setSourceSiteName(String sourceSiteName) {
        this.sourceSiteName = sourceSiteName;
    }

    /**
     * Get target site name.
     *
     * @return target site name.
     */
    public String getTargetSiteName() {
        return targetSiteName;
    }

    /**
     * Set target site name.
     *
     * @param targetSiteName
     *            target site name.
     */
    public void setTargetSiteName(String targetSiteName) {
        this.targetSiteName = targetSiteName;
    }

    /**
     * Get file max stay time.
     *
     * @return file max stay time.
     */
    public String getMaxStayTime() {
        return maxStayTime;
    }

    /**
     * Set file max stay time.
     *
     * @param maxStayTime
     *            file max stay time.
     */
    public void setMaxStayTime(String maxStayTime) {
        this.maxStayTime = maxStayTime;
    }

    /**
     * Get the data check level of the move file schedule.
     *
     * @return the data check level.
     */
    public ScmDataCheckLevel getDataCheckLevel() {
        return dataCheckLevel;
    }

    /**
     * Set the data check level of the move file schedule.
     *
     * @param dataCheckLevel
     *            the data check level.
     */
    public void setDataCheckLevel(ScmDataCheckLevel dataCheckLevel) {
        this.dataCheckLevel = dataCheckLevel;
    }

    /**
     * Get whether enable quick start of the move file schedule.
     *
     * @return whether quick start.
     */
    public boolean isQuickStart() {
        return quickStart;
    }

    /**
     * Set need quick start for the move file schedule.
     *
     * @param quickStart
     *            Is need quick start.
     */
    public void setQuickStart(boolean quickStart) {
        this.quickStart = quickStart;
    }

    /**
     * Get whether enable space recycle of the move file schedule.
     *
     * @return whether space recycle.
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

    /**
     * Get extra file condition.
     *
     * @return extra file condition.
     */
    public BSONObject getExtraCondition() {
        return extraCondition;
    }

    /**
     * Sets extra file condition.
     *
     * @param extraCondition
     *            extra file condition.
     */
    public void setExtraCondition(BSONObject extraCondition) {
        this.extraCondition = extraCondition;
    }

    /**
     * Get file scope.
     *
     * @return file scope.
     */
    public ScmType.ScopeType getScope() {
        return scope;
    }

    /**
     * Set file scope.
     *
     * @param scope
     *            file scope.
     */
    public void setScope(ScmType.ScopeType scope) {
        this.scope = scope;
    }

    /**
     * Get every task max exec time in millisecond.
     *
     * @return every task max exec time.
     */
    public long getMaxExecTime() {
        return maxExecTime;
    }

    /**
     * Set every task max exec time in millisecond.
     *
     * @param maxExecTime
     *            every task max exec time.
     */
    public void setMaxExecTime(long maxExecTime) {
        this.maxExecTime = maxExecTime;
    }

    @Override
    public BSONObject toBSONObject() {
        BSONObject obj = new BasicBSONObject();
        obj.put(ScmAttributeName.Schedule.CONTENT_COPY_SOURCE_SITE, sourceSiteName);
        obj.put(ScmAttributeName.Schedule.CONTENT_COPY_TARGET_SITE, targetSiteName);
        obj.put(ScmAttributeName.Schedule.CONTENT_MAX_STAY_TIME, maxStayTime);
        obj.put(ScmAttributeName.Schedule.CONTENT_EXTRA_CONDITION, extraCondition);
        obj.put(ScmAttributeName.Schedule.CONTENT_SCOPE, scope.getScope());
        obj.put(ScmAttributeName.Schedule.CONTENT_MAX_EXEC_TIME, maxExecTime);
        obj.put(ScmAttributeName.Schedule.CONTENT_DATA_CHECK_LEVEL, dataCheckLevel.getName());
        obj.put(ScmAttributeName.Schedule.CONTENT_IS_RECYCLE_SPACE, isRecycleSpace);
        obj.put(ScmAttributeName.Schedule.CONTENT_QUICK_START, quickStart);
        return obj;
    }
}
