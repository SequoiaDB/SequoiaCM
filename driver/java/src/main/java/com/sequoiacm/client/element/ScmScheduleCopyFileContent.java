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
 * Schedule copy file content.
 */
public class ScmScheduleCopyFileContent implements ScmScheduleContent {
    private String sourceSiteName;
    private String targetSiteName;
    private String maxStayTime;
    private BSONObject extraCondition;
    private ScopeType scope = ScopeType.SCOPE_CURRENT;
    private long maxExecTime = 0;

    /**
     * Create an instance of ScmScheduleCopyFileContent with specified args.
     *
     * @param sourceSiteName
     *            source site name.
     * @param targetSiteName
     *            target site name.
     * @param maxStayTime
     *            file max stay time.
     * @param extraCondition
     *            extra file condition.
     * @throws ScmException
     *             if error happens.
     */
    public ScmScheduleCopyFileContent(String sourceSiteName, String targetSiteName,
            String maxStayTime, BSONObject extraCondition) throws ScmException {
        this(sourceSiteName, targetSiteName, maxStayTime, extraCondition, ScopeType.SCOPE_CURRENT);
    }

    /**
     * Create an instance of ScmScheduleCopyFileContent with specified args.
     *
     * @param sourceSiteName
     *            source site name.
     * @param targetSiteName
     *            target site name.
     * @param maxStayTime
     *            file max stay time.
     * @param extraCondition
     *            extra file condition.
     * @param scope
     *            file scope.
     * @throws ScmException
     *             if error happens.
     */
    public ScmScheduleCopyFileContent(String sourceSiteName, String targetSiteName,
            String maxStayTime, BSONObject extraCondition, ScopeType scope) throws ScmException {
        this.sourceSiteName = sourceSiteName;
        this.targetSiteName = targetSiteName;
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
     * Create an instance of ScmScheduleCopyFileContent with specified args.
     *
     * @param sourceSiteName
     *            source site name.
     * @param targetSiteName
     *            target site name.
     * @param maxStayTime
     *            file max stay time.
     * @param extraCondition
     *            extra file condition.
     * @param scope
     *            file scope.
     * @param maxExecTime
     *            every task max exec time in millisecond.
     * @throws ScmException
     *             if error happens.
     */
    public ScmScheduleCopyFileContent(String sourceSiteName, String targetSiteName,
            String maxStayTime, BSONObject extraCondition, ScopeType scope, long maxExecTime)
                    throws ScmException {
        this(sourceSiteName, targetSiteName, maxStayTime, extraCondition, scope);
        this.maxExecTime = maxExecTime;
    }

    /**
     * Create a instance of ScmScheduleCopyFileContent.
     *
     * @param content
     *            a bson containing information about
     *            ScmScheduleCopyFileContent.
     * @throws ScmException
     *             if error happens.
     */
    public ScmScheduleCopyFileContent(BSONObject content) throws ScmException {
        Object temp = null;
        temp = content.get(ScmAttributeName.Schedule.CONTENT_COPY_SOURCE_SITE);
        if (null != temp) {
            setSourceSiteName((String) temp);
        }

        temp = content.get(ScmAttributeName.Schedule.CONTENT_COPY_TARGET_SITE);
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
            this.scope = ScopeType.getScopeType((Integer) temp);
        }

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
     * Gets every task max exec time in millisecond.
     *
     * @return every task max exec time.
     */
    public long getMaxExecTime() {
        return maxExecTime;
    }

    /**
     * Sets every task max exec time in millisecond.
     *
     * @param maxExecTime
     *            every task max exec time.
     */
    public void setMaxExecTime(long maxExecTime) {
        this.maxExecTime = maxExecTime;
    }

    /**
     * Gets source site name.
     *
     * @return source site name.
     */
    public String getSourceSiteName() {
        return sourceSiteName;
    }

    /**
     * Sets source site name.
     *
     * @param sourceSiteName
     *            source site name.
     */
    public void setSourceSiteName(String sourceSiteName) {
        this.sourceSiteName = sourceSiteName;
    }

    /**
     * Gets target site name.
     *
     * @return target site name.
     */
    public String getTargetSiteName() {
        return targetSiteName;
    }

    /**
     * Sets target site name.
     *
     * @param targetSiteName
     *            target site name.
     */
    public void setTargetSiteName(String targetSiteName) {
        this.targetSiteName = targetSiteName;
    }

    /**
     * Gets file max stay time.
     *
     * @return file max stay time.
     */
    public String getMaxStayTime() {
        return maxStayTime;
    }

    /**
     * Sets file max stay time.
     *
     * @param maxStayTime
     *            file max stay time.
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
     *            extra file condition.
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
        obj.put(ScmAttributeName.Schedule.CONTENT_COPY_SOURCE_SITE, sourceSiteName);
        obj.put(ScmAttributeName.Schedule.CONTENT_COPY_TARGET_SITE, targetSiteName);
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

        if (!(o instanceof ScmScheduleCopyFileContent)) {
            return false;
        }

        ScmScheduleCopyFileContent right = (ScmScheduleCopyFileContent) o;

        if (sourceSiteName.equals(right.sourceSiteName)
                && targetSiteName.equals(right.targetSiteName)
                && maxStayTime.equals(right.maxStayTime)) {
            if (extraCondition != null && right.extraCondition != null) {
                return extraCondition.toString().equals(right.extraCondition.toString());
            }

            return extraCondition == right.extraCondition;
        }

        return false;
    }
}
