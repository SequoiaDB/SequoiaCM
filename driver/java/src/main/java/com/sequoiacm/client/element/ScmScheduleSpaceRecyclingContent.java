package com.sequoiacm.client.element;

import com.sequoiacm.client.core.ScmAttributeName;
import com.sequoiacm.client.exception.ScmException;
import com.sequoiacm.client.exception.ScmInvalidArgumentException;
import org.bson.BSONObject;
import org.bson.BasicBSONObject;

/**
 * Schedule space recycling content.
 */
public class ScmScheduleSpaceRecyclingContent implements ScmScheduleContent {

    private String targetSite;

    private ScmSpaceRecycleScope spaceRecycleScope;

    private long maxExecTime;

    /**
     * 
     * @param targetSite
     *            target site name.
     * @param spaceRecycleScope
     *            space recycle scope.
     * @throws ScmInvalidArgumentException
     *             if error happens.
     */
    public ScmScheduleSpaceRecyclingContent(String targetSite,
            ScmSpaceRecycleScope spaceRecycleScope) throws ScmException {
        this.targetSite = targetSite;
        this.spaceRecycleScope = spaceRecycleScope;
        if (spaceRecycleScope == null) {
            throw new ScmInvalidArgumentException("spaceRecycleScope is null");
        }
    }

    /**
     *
     * @param targetSite
     *            target site name.
     * @param spaceRecycleScope
     *            space recycle scope.
     * @param maxExecTime
     *            every task max exec time.
     * @throws ScmInvalidArgumentException
     *             if error happens.
     */
    public ScmScheduleSpaceRecyclingContent(String targetSite,
            ScmSpaceRecycleScope spaceRecycleScope, long maxExecTime) throws ScmException {
        this(targetSite, spaceRecycleScope);
        this.maxExecTime = maxExecTime;
    }

    public ScmScheduleSpaceRecyclingContent(BSONObject content) throws ScmException {
        Object temp = null;
        temp = content.get(ScmAttributeName.Schedule.CONTENT_TARGET_SITE);
        if (null != temp) {
            setTargetSite((String) temp);
        }

        temp = content.get(ScmAttributeName.Schedule.CONTENT_RECYCLE_SCOPE);
        if (null != temp) {
            setSpaceRecycleScope(ScmSpaceRecycleScope.getScope((String) temp));
        }

        temp = content.get(ScmAttributeName.Schedule.CONTENT_MAX_EXEC_TIME);
        if (null != temp) {
            setMaxExecTime(((Number) temp).longValue());
        }

    }

    /**
     * Get the target site name.
     * 
     * @return the target site name.
     */
    public String getTargetSite() {
        return targetSite;
    }

    /**
     * Set the target site name.
     * 
     * @param targetSite
     *            the target site name.
     */
    public void setTargetSite(String targetSite) {
        this.targetSite = targetSite;
    }

    /**
     * Get the space recycle scope.
     * 
     * @return the space recycle scope
     */
    public ScmSpaceRecycleScope getSpaceRecycleScope() {
        return spaceRecycleScope;
    }

    /**
     * Set the space recycle scope.
     * 
     * @param spaceRecycleScope
     *            the space recycle scope.
     */
    public void setSpaceRecycleScope(ScmSpaceRecycleScope spaceRecycleScope) {
        this.spaceRecycleScope = spaceRecycleScope;
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
        BSONObject bsonObject = new BasicBSONObject();
        bsonObject.put(ScmAttributeName.Schedule.CONTENT_RECYCLE_SCOPE,
                spaceRecycleScope.getScope());
        bsonObject.put(ScmAttributeName.Schedule.CONTENT_TARGET_SITE, targetSite);
        bsonObject.put(ScmAttributeName.Schedule.CONTENT_MAX_EXEC_TIME, maxExecTime);
        return bsonObject;
    }
}
