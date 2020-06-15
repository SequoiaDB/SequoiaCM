package com.sequoiadb.infrastructure.map.client;

import java.io.Closeable;

import com.sequoiadb.infrastructure.map.ScmMapServerException;

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
    T getNext() throws ScmMapServerException;

    @Override
    void close();
}
