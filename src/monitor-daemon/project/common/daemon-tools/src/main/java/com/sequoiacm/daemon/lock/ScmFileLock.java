package com.sequoiacm.daemon.lock;

import com.sequoiacm.daemon.common.CommonUtils;
import com.sequoiacm.daemon.exception.ScmExitCode;
import com.sequoiacm.infrastructure.tool.exception.ScmToolsException;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.nio.channels.FileChannel;
import java.nio.channels.FileLock;

public class ScmFileLock {
    private RandomAccessFile raf;
    private FileChannel channel;
    private FileLock lock;
    private File file;

    public ScmFileLock(File file) {
        this.file = file;
    }

    public void unlock() {
        CommonUtils.releaseLock(lock);
        CommonUtils.closeResource(channel);
        CommonUtils.closeResource(raf);
    }

    public void lock() throws ScmToolsException {
        try {
            raf = new RandomAccessFile(file, "rw");
            channel = raf.getChannel();
            lock = channel.lock();
        }
        catch (FileNotFoundException e) {
            throw new ScmToolsException("Failed to find file,file:" + file.getAbsolutePath(),
                    ScmExitCode.FILE_NOT_FIND, e);
        }
        catch (IOException e) {
            throw new ScmToolsException("Failed to lock file", ScmExitCode.SYSTEM_ERROR, e);
        }
    }

    public void readLock() throws ScmToolsException{
        try {
            raf = new RandomAccessFile(file, "r");
            channel = raf.getChannel();
            lock = channel.lock(0L, Long.MAX_VALUE,true);
        }
        catch (FileNotFoundException e) {
            throw new ScmToolsException("Failed to find file,file:" + file.getAbsolutePath(),
                    ScmExitCode.FILE_NOT_FIND, e);
        }
        catch (IOException e) {
            throw new ScmToolsException("Failed to lock file", ScmExitCode.SYSTEM_ERROR, e);
        }
    }

    public boolean tryLock() throws ScmToolsException {
        try {
            raf = new RandomAccessFile(file, "rw");
            channel = raf.getChannel();
            lock = channel.tryLock();
            return lock != null;
        }
        catch (FileNotFoundException e) {
            throw new ScmToolsException("Failed to find file,file:" + file.getAbsolutePath(),
                    ScmExitCode.FILE_NOT_FIND, e);
        }
        catch (IOException e) {
            throw new ScmToolsException("Failed to lock file", ScmExitCode.SYSTEM_ERROR, e);
        }
    }
}
