package com.sequoiacm.cephs3.dataoperation;

import com.sequoiacm.cephs3.CephS3Exception;

public interface CephS3DataUploader {
    void write(byte[] data, int off, int len) throws CephS3Exception;

    // 撤销已经上传的数据
    void cancel() throws CephS3Exception;

    // 提交已经上传的数据（生成对象）
    void complete() throws CephS3Exception;

    // 释放对象资源
    void close() throws CephS3Exception;

    // 返回实时更新的写入文件大小
    long getFileSize();
    //返回新创建桶名
    String getCreatedBucketName();

}
