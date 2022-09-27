package com.sequoiacm.client.element;

import com.sequoiacm.client.core.ScmAttributeName;
import com.sequoiacm.client.core.ScmQueryBuilder;
import com.sequoiacm.client.exception.ScmException;
import org.bson.BSONObject;
import org.bson.BasicBSONObject;

import java.util.Date;

public class ScmFileCondition {

    private Date fileBeginningTime;

    private Date fileEndingTime;

    /**
     * Get the beginning time(include) of the condition.
     * 
     * @return the beginning time(include) of the condition.
     */
    public Date getFileBeginningTime() {
        return fileBeginningTime;
    }

    /**
     * Set the beginning time(include) of the condition.
     * 
     * @param fileBeginningTime
     *            the beginning time(include) of the condition.
     */
    public void setFileBeginningTime(Date fileBeginningTime) {
        this.fileBeginningTime = fileBeginningTime;
    }

    /**
     * Get the ending time(exclude) of the condition.
     * 
     * @return the ending time(exclude) of the condition.
     */
    public Date getFileEndingTime() {
        return fileEndingTime;
    }

    /**
     * Set the ending time(exclude) of the condition.
     * 
     * @param fileEndingTime
     *            the ending time(exclude) of the condition.
     */
    public void setFileEndingTime(Date fileEndingTime) {
        this.fileEndingTime = fileEndingTime;
    }

    public BSONObject toBSONObject() throws ScmException {
        if (fileBeginningTime == null && fileEndingTime == null) {
            return new BasicBSONObject();
        }
        ScmQueryBuilder builder = ScmQueryBuilder.start(ScmAttributeName.File.CREATE_TIME);
        if (fileBeginningTime != null) {
            builder.greaterThanEquals(fileBeginningTime.getTime());
        }
        if (fileEndingTime != null) {
            builder.lessThan(fileEndingTime.getTime());
        }
        return builder.get();
    }

    @Override
    public String toString() {
        return "ScmFileCondition{" + "fileBeginningTime=" + fileBeginningTime + ", fileEndingTime="
                + fileEndingTime + '}';
    }

}
