package com.sequoiacm.client.core;

import java.io.OutputStream;

import com.sequoiacm.client.exception.ScmException;

/**
 * The interface to operate ScmInputStream object.
 */
public interface ScmInputStream {

    /**
     * Sets the stream-pointer offset,at whitch the next read occurs.
     *
     * @param seekType
     *            the seek type as below:
     *            <dl>
     *            <dt>CommDefine.SeekType.SCM_FILE_SEEK_SET:the offset
     *            position,measured in bytes from the beginning
     *            <dt>CommDefine.SeekType.SCM_FILE_SEEK_CUR:the offset
     *            position,measured in bytes from current position
     *            <dt>CommDefine.SeekType.SCM_FILE_SEEK_END:the offset
     *            position,measured in bytes from the end
     *            </dl>
     * @param size
     *            the offset position,measured in bytes from the seekType
     * @throws ScmException
     *             If error happens
     * @see com.sequoiacm.common.CommonDefine.SeekType
     * @since 2.2
     */
    public void seek(int seekType, long size) throws ScmException;

    /**
     * Reads data from this stream into an OutputStream
     *
     * @param out
     *            the outputStream into which the data is read
     * @throws ScmException
     *             If error happens
     * @since 2.2
     */
    public void read(OutputStream out) throws ScmException;

    /**
     * Reads up to len bytes of data from this stream into an array of bytes.
     *
     * @param b
     *            the buffer into which the data is read.
     * @param off
     *            the start offset in array b at which the data is written
     * @param len
     *            the maximum number of bytes read
     * @return the total number of bytes read into buffer,or -1 if there is no
     *         more data because the end of the stream has been readched
     * @throws ScmException
     *             If error happens
     * @since 2.2
     */
    public int read(byte[] b, int off, int len) throws ScmException;

    /**
     * close this stream
     *
     * @throws ScmException
     *             If error happens
     * @since 2.2
     */
    public void close() throws ScmException;
}
