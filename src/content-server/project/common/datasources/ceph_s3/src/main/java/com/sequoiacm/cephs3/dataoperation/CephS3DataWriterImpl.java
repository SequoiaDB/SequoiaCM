package com.sequoiacm.cephs3.dataoperation;

import com.sequoiacm.cephs3.CephS3Exception;
import com.sequoiacm.datasource.dataoperation.ScmDataWriter;
import com.sequoiacm.datasource.dataservice.ScmService;
import com.sequoiacm.infrastructure.common.annotation.SlowLog;
import com.sequoiacm.infrastructure.common.annotation.SlowLogExtra;

public class CephS3DataWriterImpl extends ScmDataWriter {
    private final CephS3MultipartUploader uploader;

    @SlowLog(operation = "createWriter", extras = {
            @SlowLogExtra(name = "writeCephS3BucketName", data = "bucketName"),
            @SlowLogExtra(name = "writeCephS3ObjectKey", data = "key") })
    public CephS3DataWriterImpl(String bucketName, String key, ScmService service,
            boolean createBucketIfNotExist) throws CephS3Exception {
        uploader = new CephS3MultipartUploader(service, bucketName, key, createBucketIfNotExist);
    }

    @Override
    public void write(byte[] content) throws CephS3Exception {
        write(content, 0, content.length);
    }

    @Override
    @SlowLog(operation = "writeData")
    public void write(byte[] content, int offset, int len) throws CephS3Exception {
        uploader.write(content, offset, len);
    }

    @Override
    @SlowLog(operation = "cancelWriter")
    public void cancel() {
        uploader.cancel();
    }

    @Override
    @SlowLog(operation = "closeWriter")
    public void close() throws CephS3Exception {
        uploader.complete();
        uploader.close();
    }

    @Override
    public long getSize() {
        return uploader.getFileSize();
    }

    @Override
    public String getCreatedTableName() {
        // TODO:no record now!
        return null;
    }

}
