package com.sequoiacm.datasource.dataoperation;

import com.sequoiacm.datasource.ScmDatasourceException;

public interface ScmDataReader {
    public void close();
    public int read(byte[] buff, int offset, int len) throws ScmDatasourceException;
    public void seek(long size) throws  ScmDatasourceException;
    public boolean isEof();
    public long getSize();
}
