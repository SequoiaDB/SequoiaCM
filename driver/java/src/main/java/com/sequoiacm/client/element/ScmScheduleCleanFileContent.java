package com.sequoiacm.client.element;

import com.sequoiacm.client.common.ScmDataCheckLevel;
import org.bson.BSONObject;
import org.bson.BasicBSONObject;

import com.sequoiacm.client.common.ScmType.ScopeType;
import com.sequoiacm.client.core.ScmAttributeName;
import com.sequoiacm.client.exception.ScmException;
import com.sequoiacm.client.exception.ScmInvalidArgumentException;
import com.sequoiacm.common.InvalidArgumentException;
import com.sequoiacm.common.ScmArgChecker;

/**
 * Schedule clean file content.
 */
public class ScmScheduleCleanFileContent implements ScmScheduleContent {
    private String siteName;
    private String maxStayTime;
    private BSONObject extraCondition;
    private ScopeType scope = ScopeType.SCOPE_CURRENT;
    private long maxExecTime = 0;
    private ScmDataCheckLevel dataCheckLevel = ScmDataCheckLevel.WEEK;
    private boolean quickStart;
    private boolean isRecycleSpace;
    private String existenceTime;

    /**
     * Create an instance of ScmScheduleCleanFileContent with specified args.
     *
     * @param siteName
     *            site name.
     * @param maxStayTime
     *            file max stay time.
     * @param extraCondition
     *            extra file condition.
     * @throws ScmException
     *             if error happens.
     */
    public ScmScheduleCleanFileContent(String siteName, String maxStayTime,
            BSONObject extraCondition) throws ScmException {
        this(siteName, maxStayTime, extraCondition, ScopeType.SCOPE_CURRENT);
    }

    /**
     * Create an instance of ScmScheduleCleanFileContent with specified args.
     *
     * @param siteName
     *            site name.
     * @param maxStayTime
     *            file max stay time.
     * @param extraCondition
     *            extra file condition.
     * @param scope
     *            file scope.
     * @throws ScmException
     *             if error happens.
     */
    public ScmScheduleCleanFileContent(String siteName, String maxStayTime,
            BSONObject extraCondition, ScopeType scope) throws ScmException {
        this.siteName = siteName;
        this.maxStayTime = maxStayTime;
        this.extraCondition = extraCondition;
        this.scope = scope;
        if (scope != ScopeType.SCOPE_CURRENT) {
            try {
                ScmArgChecker.File.checkHistoryFileMatcher(extraCondition);
            }
            catch (InvalidArgumentException e) {
                throw new ScmInvalidArgumentException("invlid condition", e);
            }
        }
    }

    /**
     * Create an instance of ScmScheduleCleanFileContent with specified args.
     *
     * @param siteName
     *            site name.
     * @param maxStayTime
     *            file max stay time.
     * @param extraCondition
     *            extra file condition.
     * @param scope
     *            file scope.
     * @param maxExecTime
     *            every task max exec time.
     * @throws ScmException
     *             if error hapens.
     */
    public ScmScheduleCleanFileContent(String siteName, String maxStayTime,
            BSONObject extraCondition, ScopeType scope, long maxExecTime) throws ScmException {
        this(siteName, maxStayTime, extraCondition, scope);
        this.maxExecTime = maxExecTime;
    }

    /**
     * Create an instance of ScmScheduleCleanFileContent with specified args.
     *
     * @param siteName
     *            site name.
     * @param maxStayTime
     *            file max stay time.
     * @param extraCondition
     *            extra file condition.
     * @param scope
     *            file scope.
     * @param existenceTime
     *            file existence time.
     * @throws ScmException
     *             if error hapens.
     */
    public ScmScheduleCleanFileContent(String siteName, String maxStayTime,
            BSONObject extraCondition, ScopeType scope, String existenceTime) throws ScmException {
        this(siteName, maxStayTime, extraCondition, scope);
        this.existenceTime = existenceTime;
    }

    /**
     *
     * @param siteName
     *            site name.
     * @param maxStayTime
     *            file max stay time.
     * @param extraCondition
     *            extra file condition.
     * @param scope
     *            file scope.
     * @param maxExecTime
     *            every task max exec time.
     * @param dataCheckLevel
     *            data check level.
     * @param quickStart
     *            is quick start.
     * @param isRecycleSpace
     *            is recycle space.
     * @throws ScmException
     *             if error happens.
     */
    public ScmScheduleCleanFileContent(String siteName, String maxStayTime,
            BSONObject extraCondition, ScopeType scope, long maxExecTime,
            ScmDataCheckLevel dataCheckLevel, boolean quickStart, boolean isRecycleSpace)
            throws ScmException {
        this(siteName, maxStayTime, extraCondition, scope);
        this.maxExecTime = maxExecTime;
        this.dataCheckLevel = dataCheckLevel;
        if (dataCheckLevel == null) {
            throw new ScmInvalidArgumentException("dataCheckLevel is null");
        }
        this.quickStart = quickStart;
        this.isRecycleSpace = isRecycleSpace;
    }

    /**
     * Create a instance of ScmScheduleCleanFileContent.
     *
     * @param content
     *            a bson containing information about ScmScheduleCleanFileContent.
     * @throws ScmException
     *             if error happens.
     */
    public ScmScheduleCleanFileContent(BSONObject content) throws ScmException {
        Object temp = null;
        temp = content.get(ScmAttributeName.Schedule.CONTENT_CLEAN_SITE);
        if (null != temp) {
            setSiteName((String) temp);
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
            this.scope = ScopeType.getScopeType((Integer) temp);
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
        temp = content.get(ScmAttributeName.Schedule.CONTENT_EXISTENCE_TIME);
        if (null != temp) {
            setExistenceTime((String) temp);
        }
    }

    /**
     * Gets every task max exec time in millisecond.
     *
     * @return time in millisecond.
     */
    public long getMaxExecTime() {
        return maxExecTime;
    }

    /**
     * Gets every task max exec time in millisecond.
     *
     * @param maxExecTime
     *            time in millisecond.
     */
    public void setMaxExecTime(long maxExecTime) {
        this.maxExecTime = maxExecTime;
    }

    /**
     * Gets site name.
     *
     * @return site name.
     */
    public String getSiteName() {
        return siteName;
    }

    /**
     * Sets site name.
     *
     * @param siteName
     *            site name.
     */
    public void setSiteName(String siteName) {
        this.siteName = siteName;
    }

    /**
     * Gets file max stay time.
     *
     * @return time string.
     */
    public String getMaxStayTime() {
        return maxStayTime;
    }

    /**
     * Sets file max stay time.
     *
     * @param maxStayTime
     *            time string.
     */
    public void setMaxStayTime(String maxStayTime) {
        this.maxStayTime = maxStayTime;
    }

    /**
     * Gets file existence time.
     *
     * @return time string.
     */
    public String getExistenceTime() {
        return existenceTime;
    }

    /**
     * Sets file max existence time.
     *
     * @param existenceTime
     *            time string.
     */
    public void setExistenceTime(String existenceTime) {
        this.existenceTime = existenceTime;
    }

    /**
     * Gets extra file condition.
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
     *            file condition
     */
    public void setExtraCondition(BSONObject extraCondition) {
        this.extraCondition = extraCondition;
    }

    /**
     * Gets file scope.
     *
     * @return file scope.
     */
    public ScopeType getScope() {
        return scope;
    }

    /**
     * Sets file scope.
     *
     * @param scope
     *            file scope.
     */
    public void setScope(ScopeType scope) {
        this.scope = scope;
    }

    /**
     * Get the data check level of the clean file schedule.
     *
     * @return the data check level.
     */
    public ScmDataCheckLevel getDataCheckLevel() {
        return dataCheckLevel;
    }

    /**
     * Set the data check level of the clean file schedule.
     *
     * @param dataCheckLevel
     *            the data check level.
     */
    public void setDataCheckLevel(ScmDataCheckLevel dataCheckLevel) {
        this.dataCheckLevel = dataCheckLevel;
    }

    /**
     * Get whether enable quick start of the clean file schedule.
     *
     * @return whether quick start.
     */
    public boolean isQuickStart() {
        return quickStart;
    }

    /**
     * Set need quick start for the clean file schedule.
     *
     * @param quickStart
     *            Is need quick start.
     */
    public void setQuickStart(boolean quickStart) {
        this.quickStart = quickStart;
    }

    /**
     * Get whether enable space recycle of the clean file schedule.
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

    @Override
    public BSONObject toBSONObject() {
        BSONObject obj = new BasicBSONObject();
        obj.put(ScmAttributeName.Schedule.CONTENT_CLEAN_SITE, siteName);
        obj.put(ScmAttributeName.Schedule.CONTENT_MAX_STAY_TIME, maxStayTime);
        obj.put(ScmAttributeName.Schedule.CONTENT_EXTRA_CONDITION, extraCondition);
        obj.put(ScmAttributeName.Schedule.CONTENT_SCOPE, scope.getScope());
        obj.put(ScmAttributeName.Schedule.CONTENT_MAX_EXEC_TIME, maxExecTime);
        obj.put(ScmAttributeName.Schedule.CONTENT_DATA_CHECK_LEVEL, dataCheckLevel.getName());
        obj.put(ScmAttributeName.Schedule.CONTENT_IS_RECYCLE_SPACE, isRecycleSpace);
        obj.put(ScmAttributeName.Schedule.CONTENT_QUICK_START, quickStart);
        if (null != existenceTime) {
            obj.put(ScmAttributeName.Schedule.CONTENT_EXISTENCE_TIME, existenceTime);
        }
        return obj;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }

        if (!(o instanceof ScmScheduleCleanFileContent)) {
            return false;
        }

        ScmScheduleCleanFileContent right = (ScmScheduleCleanFileContent) o;

        if (siteName.equals(right.siteName) && maxStayTime.equals(right.maxStayTime)) {
            if (extraCondition != null && right.extraCondition != null) {
                return extraCondition.toString().equals(right.extraCondition.toString());
            }

            return extraCondition == right.extraCondition;
        }

        return false;
    }

}
