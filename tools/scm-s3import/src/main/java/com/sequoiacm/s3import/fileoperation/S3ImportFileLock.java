package com.sequoiacm.s3import.fileoperation;

import com.sequoiacm.infrastructure.tool.exception.ScmToolsException;
import com.sequoiacm.s3import.common.CommonUtils;
import com.sequoiacm.s3import.exception.S3ImportExitCode;

import java.io.IOException;
import java.nio.channels.FileChannel;
import java.nio.channels.FileLock;

public class S3ImportFileLock {

    private FileChannel channel;
    private FileLock lock;

    public S3ImportFileLock(FileChannel channel) {
        this.channel = channel;
    }

    public void unlock() {
        CommonUtils.releaseLock(lock);
    }

    public boolean tryLock() throws ScmToolsException {
        try {
            lock = channel.tryLock();
            return lock != null;
        }
        catch (IOException e) {
            throw new ScmToolsException("Failed to lock file", S3ImportExitCode.SYSTEM_ERROR, e);
        }
    }

}
