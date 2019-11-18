package com.sequoiacm.client.element;

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
     * Create a instance of ScmScheduleCleanFileContent.
     *
     * @param content
     *            a bson containing information about
     *            ScmScheduleCleanFileContent.
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

        if (scope != ScopeType.SCOPE_CURRENT) {
            try {
                ScmArgChecker.File.checkHistoryFileMatcher(extraCondition);
            }
            catch (InvalidArgumentException e) {
                throw new ScmInvalidArgumentException("invalid condition", e);
            }
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

    @Override
    public BSONObject toBSONObject() {
        BSONObject obj = new BasicBSONObject();
        obj.put(ScmAttributeName.Schedule.CONTENT_CLEAN_SITE, siteName);
        obj.put(ScmAttributeName.Schedule.CONTENT_MAX_STAY_TIME, maxStayTime);
        obj.put(ScmAttributeName.Schedule.CONTENT_EXTRA_CONDITION, extraCondition);
        obj.put(ScmAttributeName.Schedule.CONTENT_SCOPE, scope.getScope());
        obj.put(ScmAttributeName.Schedule.CONTENT_MAX_EXEC_TIME, maxExecTime);
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
