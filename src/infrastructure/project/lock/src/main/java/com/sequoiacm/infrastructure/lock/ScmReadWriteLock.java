package com.sequoiacm.infrastructure.lock;

/**
 * The abstract class of Distributed ReadWrite Lock.
 * It is implemented by ScmReadLock and ScmWriteLock.
 * 
 * @since SCM2.0
 */
public interface ScmReadWriteLock {
    
    /**
     * acquire a reader lock
     */
    public abstract ScmLock readLock();
    
    /**
     * acquire a writer lock
     */
    public abstract ScmLock writeLock();
}
