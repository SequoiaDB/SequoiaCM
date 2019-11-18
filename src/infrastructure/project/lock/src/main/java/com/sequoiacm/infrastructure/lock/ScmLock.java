package com.sequoiacm.infrastructure.lock;

import java.util.concurrent.TimeUnit;

/**
 * The interface of Distributed ReadWrite Lock.
 *
 * @since SCM2.0
 */
public interface ScmLock {

    public static final long INFINITE_TIME = -1;

    /**
     * release a lock.
     */
    public abstract void unlock();

    /**
     * acquire a lock - blocking until it's available.
     *
     * @throws Exception ZK errors, connection interruptions
     */
    public abstract void lock() throws Exception;

    /**
     * acquire a lock.
     *
     * @param waitTime
     *         if the value is ScmLock.INFINITE_TIME, it waits until it achieves a lock.
     *         <p>
     *         Or the value is the millisecond. If it is overtime, it doesn't acquire any lock.
     * @return <p>
     *         true：acquire a lock success.
     *         <p>
     *         false:acquire a lock failed.
     * @throws Exception ZK errors, connection interruptions
     */
    public abstract boolean lock(long waitTime,TimeUnit unit) throws Exception;

    /**
     * try to acquire a lock.
     *
     * @return <p>
     *         true：acquire a lock success.
     *         <p>
     *         false: acquire a lock failed.
     * @throws Exception ZooKeeper Inner Error
     */
    public abstract boolean tryLock() throws Exception;

}
