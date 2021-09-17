package com.sequoiacm.client.element;

import com.sequoiacm.infrastructure.statistics.common.ScmTimeAccuracy;

import java.util.Date;

/**
 * File statistics data.
 */
public class ScmFileStatisticInfo {
    private ScmFileStatisticsType type;
    private Date begin;
    private Date end;
    private String user;
    private String workspace;
    private ScmTimeAccuracy timeAccuracy;

    private int requestCount;
    private long avgTrafficSize;
    private long avgResponseTime;
    private long maxResponseTime;
    private long minResponseTime;
    private int failCount;

    public ScmFileStatisticInfo(ScmFileStatisticsType type, Date begin, Date end, String user,
            String workspace, ScmTimeAccuracy timeAccuracy, int requestCount, long avgTrafficSize,
            long avgResponseTime) {
        this.type = type;
        this.begin = begin;
        this.end = end;
        this.user = user;
        this.workspace = workspace;
        this.timeAccuracy = timeAccuracy;
        this.requestCount = requestCount;
        this.avgTrafficSize = avgTrafficSize;
        this.avgResponseTime = avgResponseTime;
    }

    public ScmFileStatisticInfo(ScmFileStatisticsType type, Date begin, Date end, String user,
            String workspace, ScmTimeAccuracy timeAccuracy, int requestCount, long avgTrafficSize,
            long avgResponseTime, long maxResponseTime, long minResponseTime, int failCount) {
        this.type = type;
        this.begin = begin;
        this.end = end;
        this.user = user;
        this.workspace = workspace;
        this.timeAccuracy = timeAccuracy;
        this.requestCount = requestCount;
        this.avgTrafficSize = avgTrafficSize;
        this.avgResponseTime = avgResponseTime;
        this.maxResponseTime = maxResponseTime;
        this.minResponseTime = minResponseTime;
        this.failCount = failCount;
    }

    /**
     * Returns the type of file statistics data.
     * 
     * @return type.
     */
    public ScmFileStatisticsType getType() {
        return type;
    }

    /**
     * Returns the begin of date, inclusive.
     * 
     * @return date.
     */
    public Date getBeginDate() {
        return begin;
    }

    /**
     * Returns the end of date, exclusive.
     * 
     * @return date.
     */
    public Date getEndDate() {
        return end;
    }

    /**
     * Returns the username.
     * 
     * @return username.
     */
    public String getUser() {
        return user;
    }

    /**
     * Returns the workspace.
     * 
     * @return workspace name.
     */
    public String getWorkspace() {
        return workspace;
    }

    /**
     * Returns the time accuracy.
     * 
     * @return time accuracy.
     */
    public ScmTimeAccuracy getTimeAccuracy() {
        return timeAccuracy;
    }

    /**
     * Returns the request count.
     * 
     * @return request count.
     */
    public int getRequestCount() {
        return requestCount;
    }

    /**
     * Returns the average traffic size.
     * 
     * @return average file size.
     */
    public long getAvgTrafficSize() {
        return avgTrafficSize;
    }

    /**
     * Returns the average response time.
     * 
     * @return average response time.
     */
    public long getAvgResponseTime() {
        return avgResponseTime;
    }

    /**
     * Returns the maximum response time.
     * 
     * @since 3.1.3
     * @return maximum response time.
     */
    public long getMaxResponseTime() {
        return maxResponseTime;
    }

    /**
     * Returns the minimum response time.
     * 
     * @since 3.1.3
     * @return minimum response time.
     */
    public long getMinResponseTime() {
        return minResponseTime;
    }

    /**
     * Returns the failed request count.
     * 
     * @since 3.1.3
     * @return failed request count.
     */
    public int getFailCount() {
        return failCount;
    }

    /**
     * Returns the success request count.
     * 
     * @since 3.1.3
     * @return success request count.
     */
    public int getSuccessCount() {
        return requestCount - failCount;
    }

    @Override
    public String toString() {
        return "ScmFileStatisticInfo{" + "type=" + type + ", begin=" + begin + ", end=" + end
                + ", user='" + user + '\'' + ", workspace='" + workspace + '\'' + ", timeAccuracy="
                + timeAccuracy + ", requestCount=" + requestCount + ", avgTrafficSize="
                + avgTrafficSize + ", avgResponseTime=" + avgResponseTime + ", maxResponseTime="
                + maxResponseTime + ", minResponseTime=" + minResponseTime + ", failCount="
                + failCount + ", successCount=" + getSuccessCount() + '}';
    }
}
