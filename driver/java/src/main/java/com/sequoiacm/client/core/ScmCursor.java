package com.sequoiacm.client.core;

import java.io.Closeable;

import com.sequoiacm.client.exception.ScmException;

/**
 * Scm cursor.
 *
 * @param <T>
 *            element type.
 */
public interface ScmCursor<T> extends Closeable {
    /**
     * Returns true if the cursor has more elements.
     *
     * @return true or false.
     */
    boolean hasNext();

    /**
     * Returns the next element in the cursor.
     *
     * @return next element.
     * @throws ScmException
     *             if error happens.
     */
    T getNext() throws ScmException;

    @Override
    void close();
}
