package com.sequoiacm.datasource.dataoperation;

import com.sequoiacm.datasource.ScmDatasourceException;

public interface ScmBreakpointDataWriter {
    void write(long dataOffset, byte[] data, int offset, int length) throws ScmDatasourceException;
    void truncate(long length) throws ScmDatasourceException;
    void flush() throws ScmDatasourceException;
    void close() throws ScmDatasourceException;
    String getCreatedTableName();
}
