package com.sequoiacm.infrastructure.tool.fileoperation;

import java.io.IOException;
import java.nio.channels.FileChannel;
import java.nio.channels.FileLock;

import com.sequoiacm.infrastructure.tool.exception.ScmBaseExitCode;
import com.sequoiacm.infrastructure.tool.exception.ScmToolsException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ScmFileLock {

    private Logger logger = LoggerFactory.getLogger(ScmFileLock.class);
    private FileChannel channel;
    private FileLock lock;

    public ScmFileLock(FileChannel channel) {
        this.channel = channel;
    }

    public void unlock() {
        if (lock != null) {
            try {
                lock.release();
            }
            catch (IOException e) {
                logger.warn("Failed to release lock:{}", lock, e);
            }
        }
    }

    public boolean tryLock() throws ScmToolsException {
        try {
            lock = channel.tryLock();
            return lock != null;
        }
        catch (IOException e) {
            throw new ScmToolsException("Failed to lock file", ScmBaseExitCode.SYSTEM_ERROR, e);
        }
    }

}
