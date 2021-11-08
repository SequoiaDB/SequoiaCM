package com.sequoiacm.daemon.lock;

import com.sequoiacm.daemon.common.CommonUtils;
import com.sequoiacm.daemon.exception.ScmExitCode;
import com.sequoiacm.infrastructure.tool.exception.ScmToolsException;

import java.io.IOException;
import java.nio.channels.FileChannel;
import java.nio.channels.FileLock;

public class ScmFileLock {
    private FileChannel channel;
    private FileLock lock;

    public ScmFileLock(FileChannel channel) {
        this.channel = channel;
    }

    public void unlock() {
        CommonUtils.releaseLock(lock);

    }

    public void lock() throws ScmToolsException {
        try {
            lock = channel.lock();
        }
        catch (IOException e) {
            throw new ScmToolsException("Failed to lock file", ScmExitCode.SYSTEM_ERROR, e);
        }
    }

    public boolean tryLock() throws ScmToolsException {
        try {
            lock = channel.tryLock();
            return lock != null;
        }
        catch (IOException e) {
            throw new ScmToolsException("Failed to lock file", ScmExitCode.SYSTEM_ERROR, e);
        }
    }
}
