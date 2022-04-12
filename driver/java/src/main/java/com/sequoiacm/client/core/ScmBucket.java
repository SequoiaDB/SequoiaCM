package com.sequoiacm.client.core;

import com.sequoiacm.client.element.ScmFileBasicInfo;
import com.sequoiacm.client.exception.ScmException;
import org.bson.BSONObject;

import java.util.Date;

public interface ScmBucket {
    /**
     * Return the bucket name.
     * 
     * @return bucket name.
     */
    String getName();

    /**
     * Return workspace of the bucket.
     * 
     * @return workspace name.
     */
    String getWorkspace();

    /**
     * Return username of the bucket.
     * 
     * @return username.
     */
    String getCreateUser();

    /**
     * Return creation time of the bucket.
     * 
     * @return creation time.
     */
    Date getCreateTime();

    /**
     * Return bucket id.
     * 
     * @return bucketid.
     */
    long getId();

    /**
     * Return a file with the specified file name in the bucket.
     * 
     * @param fileName
     *            file name.
     * @return file.
     * @throws ScmException
     *             if error happens.
     */
    ScmFile getFile(String fileName) throws ScmException;

    /**
     * List files in the bucket.
     * 
     * @param condition
     *            the condition for query
     * @param orderby
     *            the condition for sort.
     * @param skip
     *            skip to the first number record
     * @param limit
     *            return the total records of query, when value is -1, return all
     *            records
     * @return cursor.
     * @throws ScmException
     *             if error happens.
     */
    ScmCursor<ScmFileBasicInfo> listFile(BSONObject condition, BSONObject orderby, long skip,
            long limit) throws ScmException;

    /**
     * Constructs a new instance of the subclassable ScmFile class to be persisted
     * in the bucket.
     * 
     * @param fileName
     *            file name.
     * @return An object reference to a new instance of this class.
     * @throws ScmException
     *             if error happens.
     */
    ScmFile createFile(String fileName) throws ScmException;

    /**
     * Return the file count in the bucket.
     * 
     * @param condition
     *            file condition for count.
     * @return file count.
     * @throws ScmException
     *             if error happens.
     */
    long countFile(BSONObject condition) throws ScmException;
}
