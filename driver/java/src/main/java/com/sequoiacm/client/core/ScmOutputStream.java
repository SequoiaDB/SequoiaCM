package com.sequoiacm.client.core;

import com.sequoiacm.client.exception.ScmException;

/**
 * The interface to operate ScmOutputStream object.
 */
public interface ScmOutputStream {

    /**
     * Writes <code>len</code> bytes from the specified byte array starting at
     * offset <code>off</code> to this output stream.
     *
     * @param b
     *            The data.
     * @param off
     *            The start offset in the data.
     * @param len
     *            The number of bytes to write.
     * @throws ScmException
     *             If error happens
     */
    public void write(byte[] b, int off, int len) throws ScmException;

    /**
     * Writes <code>b.length</code> bytes from the specified byte array to this
     * output stream.
     *
     * @param b
     *            the data.
     * @throws ScmException
     *             If error happens
     */
    public void write(byte[] b) throws ScmException;

    /**
     * Save file with the data of this stream and close this stream.
     *
     * @throws ScmException
     *             If error happens
     */
    public void commit() throws ScmException;

    /**
     * Cancel to create file and close stream
     */
    public void cancel();
}
