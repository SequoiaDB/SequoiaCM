package com.sequoiacm.contentserver.remote;

import com.sequoiacm.exception.ScmServerException;

public abstract class ScmFileReader {
    public abstract void close();

    public abstract int read(byte[] buff, int offset, int len) throws ScmServerException;

    public abstract void seek(long size) throws ScmServerException;

    public abstract boolean isEof();

    public abstract long getSize() throws ScmServerException;
}
