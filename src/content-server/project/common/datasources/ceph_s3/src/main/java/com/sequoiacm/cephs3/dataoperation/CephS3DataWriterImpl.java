package com.sequoiacm.cephs3.dataoperation;

import com.sequoiacm.cephs3.CephS3Exception;
import com.sequoiacm.cephs3.dataservice.CephS3ConnWrapper;
import com.sequoiacm.cephs3.dataservice.CephS3DataService;
import com.sequoiacm.datasource.dataoperation.ScmDataWriter;
import com.sequoiacm.datasource.dataservice.ScmService;
import com.sequoiacm.infrastructure.common.annotation.SlowLog;
import com.sequoiacm.infrastructure.common.annotation.SlowLogExtra;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.ByteArrayInputStream;

public class CephS3DataWriterImpl extends ScmDataWriter {
    private static final Logger logger = LoggerFactory.getLogger(CephS3DataWriterImpl.class);
    private final CephS3UploaderWrapper uploader;
    private final String key;
    private final String bucket;

    @SlowLog(operation = "createWriter", extras = {
            @SlowLogExtra(name = "writeCephS3BucketName", data = "bucketName"),
            @SlowLogExtra(name = "writeCephS3ObjectKey", data = "key") })
    public CephS3DataWriterImpl(String bucketName, String key, ScmService service,
            boolean createBucketIfNotExist) throws CephS3Exception {
        this.key = key;
        this.bucket = bucketName;
        uploader = new CephS3UploaderWrapper((CephS3DataService) service, bucketName, key,
                createBucketIfNotExist);
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
        try {
            uploader.cancel();
        }
        finally {
            closeUploaderSilence();
        }
    }

    @Override
    @SlowLog(operation = "closeWriter")
    public void close() throws CephS3Exception {
        try {
            uploader.complete();
        }
        finally {
            closeUploaderSilence();
        }
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

    private void closeUploaderSilence() {
        try {
            uploader.close();
        }
        catch (Exception e) {
            logger.warn("failed to close uploader: bucket={}, key={}", bucket, key,
                    e);
        }
    }

}

