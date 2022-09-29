package com.sequoiacm.client.element;

import com.sequoiacm.client.core.ScmAttributeName;
import com.sequoiacm.client.core.ScmQueryBuilder;
import com.sequoiacm.client.exception.ScmException;
import com.sequoiacm.client.exception.ScmInvalidArgumentException;
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
     * @throws ScmInvalidArgumentException
     *             if fileEndingTime is less than fileBeginningTime.
     */
    public void setFileBeginningTime(Date fileBeginningTime) throws ScmInvalidArgumentException {
        this.fileBeginningTime = fileBeginningTime;
        checkTimeCondition();
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
     * @throws ScmInvalidArgumentException
     *             if fileEndingTime is less than fileBeginningTime.
     */
    public void setFileEndingTime(Date fileEndingTime) throws ScmInvalidArgumentException {
        this.fileEndingTime = fileEndingTime;
        checkTimeCondition();
    }

    private void checkTimeCondition() throws ScmInvalidArgumentException {
        if (fileBeginningTime != null && fileEndingTime != null) {
            if (fileEndingTime.getTime() < fileBeginningTime.getTime()) {
                throw new ScmInvalidArgumentException(
                        "fileEndingTime must be greater than or equal to fileBeginningTime, fileBeginningTime="
                                + fileBeginningTime.getTime() + ", fileEndingTime="
                                + fileEndingTime.getTime());
            }
        }
    }

    public BSONObject toBSONObject() throws ScmException {
        if (fileBeginningTime == null && fileEndingTime == null) {
            return new BasicBSONObject();
        }
        ScmQueryBuilder builder = ScmQueryBuilder.start(ScmAttributeName.File.CREATE_TIME);
        if (fileEndingTime != null && fileBeginningTime != null
                && fileBeginningTime.getTime() == fileEndingTime.getTime()) {
            builder.is(fileEndingTime.getTime());
        }
        else {
            if (fileBeginningTime != null) {
                builder.greaterThanEquals(fileBeginningTime.getTime());
            }
            if (fileEndingTime != null) {
                builder.lessThan(fileEndingTime.getTime());
            }
        }
        return builder.get();
    }

    @Override
    public String toString() {
        return "ScmFileCondition{" + "fileBeginningTime=" + fileBeginningTime + ", fileEndingTime="
                + fileEndingTime + '}';
    }

}
