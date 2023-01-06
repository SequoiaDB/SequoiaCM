package com.sequoiacm.datasource.dataoperation;

import org.bson.BSONObject;

import com.sequoiacm.datasource.ScmDatasourceException;

public interface ScmBreakpointDataWriter {
    void write(byte[] data, int offset, int length) throws ScmDatasourceException;

    void truncate(long length) throws ScmDatasourceException;

    void flush() throws ScmDatasourceException;

    // 该断点文件的数据已全部写入底层存储
    void complete() throws ScmDatasourceException;

    // 释放对象资源
    void close() throws ScmDatasourceException;

    // 删除断点文件数据
    void abort() throws ScmDatasourceException;

    BSONObject getBreakpointContext();

    String getCreatedTableName();
}
