package com.sequoiacm.sequoiadb.dataopertion;

import com.sequoiacm.sequoiadb.SequoiadbException;

public interface SdbWriter {
    void write(byte[] data, int off, int len) throws SequoiadbException;

    // 撤销已经上传的数据
    void cancel();

    // 提交已经上传的数据（生成对象）
    String getCreatedTableName();

    // 释放对象资源
    void close() throws SequoiadbException;

    // 返回实时更新的写入文件大小
    long getFileSize();
}
