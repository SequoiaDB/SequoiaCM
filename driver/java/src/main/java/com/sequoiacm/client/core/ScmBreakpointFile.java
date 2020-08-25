package com.sequoiacm.client.core;

import java.io.File;
import java.io.InputStream;
import java.util.Date;

import com.sequoiacm.client.common.ScmChecksumType;
import com.sequoiacm.client.exception.ScmException;

/**
 * The breakpoint continuingly upload file.
 */
public interface ScmBreakpointFile {

    /**
     * Get the workspace where the breakpoint file's in.
     *
     * @return the workspace
     */
    ScmWorkspace getWorkspace();

    /**
     * Get the name of the breakpoint file.
     *
     * @return the file name
     */
    String getFileName();

    /**
     * Get the site name where the breakpoint file's in.
     *
     * @return the site name
     */
    String getSiteName();

    /**
     * Get the checksum type of the breakpoint file.
     *
     * @return the checksum type
     */
    ScmChecksumType getChecksumType();

    /**
     * Get the checksum value of the uploaded file data.
     *
     * @return the checksum value
     */
    long getChecksum();

    /**
     * Get the data id of the breakpoint file.
     *
     * @return the data id
     */
    String getDataId();

    /**
     * Get whether the breakpoint file is completed.
     *
     * @return true if the breakpoint file is completed, otherwise false
     */
    boolean isCompleted();

    /**
     * Get the uploaded data size.
     *
     * @return the uploaded data size
     */
    long getUploadSize();

    /**
     * Get create user of the breakpoint file.
     *
     * @return the create user
     */
    String getCreateUser();

    /**
     * Get create time of the breakpoint file.
     *
     * @return the create time
     */
    Date getCreateTime();

    /**
     * Set create time of the breakpoint file.
     *
     * @param createTime
     *            the create time
     * @throws ScmException
     *             if exception happens
     */
    void setCreateTime(Date createTime) throws ScmException;

    /**
     * Get the last upload user of the breakpoint file.
     *
     * @return the last upload user
     */
    String getUploadUser();

    /**
     * Get the last upload time of the breakpoint file.
     *
     * @return the last upload time
     */
    Date getUploadTime();

    /**
     * Upload the input stream data. The stream should contain all the data of
     * the file. It will calculate the checksum of the front uploaded data and
     * compare with checksum of server side.
     *
     * @param dataStream
     *            the file data stream without offset
     * @throws ScmException
     *             if exception happens
     */
    void upload(InputStream dataStream) throws ScmException;

    /**
     * Upload the file. It will calculate the checksum of the front uploaded
     * data and compare with checksum of server side.
     *
     * @param file
     *            the file to be uploaded
     * @throws ScmException
     *             if exception happens
     */
    void upload(File file) throws ScmException;

    /**
     * Incremental upload the data. No checksum comparison.
     *
     * @param dataStream
     *            the file data stream with the upload size offset
     * @param isLastContent
     *            true if data in the stream is last content of the file,
     *            otherwise false
     * @throws ScmException
     *             if exception happens
     */
    void incrementalUpload(InputStream dataStream, boolean isLastContent) throws ScmException;

    /**
     * Returns true if the breakpoint file will calc md5 when it completed.
     *
     * @return true if the breakpoint file will calc md5 when it completed
     */
    boolean isNeedMd5();

    /**
     * Get the md5 of the breakpoint file.
     * 
     * @return return null if the breakpoint file is not completed or the
     *         breakpoint file don't need to calc md5.
     */
    String getMd5();

    /**
     * Calculate the md5 of the breakpoint file.
     */
    void calcMd5() throws ScmException;
}
