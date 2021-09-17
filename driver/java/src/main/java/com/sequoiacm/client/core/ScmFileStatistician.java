package com.sequoiacm.client.core;

import com.sequoiacm.client.element.ScmFileStatisticInfo;
import com.sequoiacm.client.element.ScmFileStatisticsType;
import com.sequoiacm.client.exception.ScmException;
import com.sequoiacm.client.util.BsonUtils;
import com.sequoiacm.exception.ScmError;
import com.sequoiacm.infrastructure.statistics.common.ScmStatisticsDefine;
import com.sequoiacm.infrastructure.statistics.common.ScmStatisticsType;
import com.sequoiacm.infrastructure.statistics.common.ScmTimeAccuracy;
import org.bson.BSONObject;
import org.bson.BasicBSONObject;

import java.text.SimpleDateFormat;
import java.util.Date;

/**
 * File statistician.
 */
public class ScmFileStatistician {
    private final ScmSession ss;
    private String type;
    private Date beginDate;
    private Date endDate;
    private String user;
    private String workspace;
    private ScmTimeAccuracy timeAccuracy;

    ScmFileStatistician(ScmSession ss) {
        this.ss = ss;
    }

    /**
     * Sets the type of file statistics data.
     * 
     * @return a reference to this object
     */
    public ScmFileStatistician upload() {
        type = ScmStatisticsType.FILE_UPLOAD;
        return this;
    }

    /**
     * Sets the type of file statistics data.
     * 
     * @return a reference to this object
     */
    public ScmFileStatistician download() {
        type = ScmStatisticsType.FILE_DOWNLOAD;
        return this;
    }

    /**
     * Sets the username of file statistics data.
     * 
     * @param user
     *            username.
     * @return a reference to this object
     */
    public ScmFileStatistician user(String user) {
        this.user = user;
        return this;
    }

    /**
     * Sets the workspace of file statistics data.
     * 
     * @param workspace
     *            workspace name.
     * @return a reference to this object
     */
    public ScmFileStatistician workspace(String workspace) {
        this.workspace = workspace;
        return this;
    }

    /**
     * Sets the begin of date, inclusive.
     * 
     * @param beginDate
     *            begin of date.
     * @return a reference to this object
     */
    public ScmFileStatistician beginDate(Date beginDate) {
        this.beginDate = beginDate;
        return this;
    }

    /**
     * Sets the end of date, exclusive.
     * 
     * @param endDate
     *            end of date.
     * @return a reference to this object
     */
    public ScmFileStatistician endDate(Date endDate) {
        this.endDate = endDate;
        return this;
    }

    /**
     * Sets the time accuracy.
     * 
     * @param accuracy
     *            time accuracy.
     * @return a reference to this object
     */
    public ScmFileStatistician timeAccuracy(ScmTimeAccuracy accuracy) {
        this.timeAccuracy = accuracy;
        return this;
    }

    /**
     * Returns the file statistics data.
     * 
     * @return statistics data.
     * @throws ScmException
     *             if error happens.
     */
    public ScmFileStatisticInfo get() throws ScmException {
        if (type == null) {
            throw new ScmException(ScmError.INVALID_ARGUMENT,
                    "please specify file statistics type: call upload() or download()");
        }
        if (endDate == null || beginDate == null) {
            throw new ScmException(ScmError.INVALID_ARGUMENT,
                    "please specify begin and end date: begin=" + beginDate + ", end=" + endDate);
        }
        if (endDate.getTime() <= beginDate.getTime()) {
            throw new ScmException(ScmError.INVALID_ARGUMENT,
                    "the date of begin must be less than end: begin=" + beginDate + ", end="
                            + endDate);
        }
        BSONObject condition = new BasicBSONObject();
        SimpleDateFormat sdf = new SimpleDateFormat(ScmStatisticsDefine.DATE_PATTERN);
        condition.put(ScmStatisticsDefine.REST_FIELD_USER, user);
        condition.put(ScmStatisticsDefine.REST_FIELD_WORKSPACE, workspace);
        condition.put(ScmStatisticsDefine.REST_FIELD_TIME_ACCURACY,
                timeAccuracy == null ? null : timeAccuracy.name());
        condition.put(ScmStatisticsDefine.REST_FIELD_BEGIN, sdf.format(beginDate));
        condition.put(ScmStatisticsDefine.REST_FIELD_END, sdf.format(endDate));
        BSONObject statisticsBson = ss.getDispatcher().getStatisticsData(type, condition);
        return new ScmFileStatisticInfo(ScmFileStatisticsType.get(type), beginDate, endDate, user,
                workspace, timeAccuracy,
                BsonUtils.getIntegerChecked(statisticsBson,
                        ScmStatisticsDefine.REST_FIELD_REQ_COUNT),
                BsonUtils.getNumberChecked(statisticsBson,
                        ScmStatisticsDefine.REST_FIELD_AVG_TRAFFIC_SIZE).longValue(),
                BsonUtils.getNumberChecked(statisticsBson,
                        ScmStatisticsDefine.REST_FIELD_AVG_RESP_TIME).longValue(),
                BsonUtils.getNumberOrElse(statisticsBson,
                        ScmStatisticsDefine.REST_FIELD_MAX_RESP_TIME, 0).longValue(),
                BsonUtils.getNumberOrElse(statisticsBson,
                        ScmStatisticsDefine.REST_FIELD_MIN_RESP_TIME, 0).longValue(),
                BsonUtils.getNumberOrElse(statisticsBson,
                        ScmStatisticsDefine.REST_FIELD_FAIL_COUNT, 0).intValue());
    }
}
