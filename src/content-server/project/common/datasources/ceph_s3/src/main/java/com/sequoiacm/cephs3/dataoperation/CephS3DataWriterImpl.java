package com.sequoiacm.cephs3.dataoperation;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.sequoiacm.cephs3.CephS3Exception;
import com.sequoiacm.cephs3.dataservice.CephS3DataService;
import com.sequoiacm.datasource.common.ScmDataWriterContext;
import com.sequoiacm.datasource.dataoperation.ScmDataWriter;
import com.sequoiacm.datasource.dataservice.ScmService;
import com.sequoiacm.datasource.metadata.cephs3.CephS3DataLocation;
import com.sequoiacm.infrastructure.common.annotation.SlowLog;
import com.sequoiacm.infrastructure.common.annotation.SlowLogExtra;

public class CephS3DataWriterImpl extends ScmDataWriter {
    private static final Logger logger = LoggerFactory.getLogger(CephS3DataWriterImpl.class);
    private final CephS3UploaderWrapper uploader;
    private final String key;
    private final BucketNameOption bucketNameOption;
    @SlowLog(operation = "createWriter", extras = {
            @SlowLogExtra(name = "writeCephS3BucketName", data = "bucketNameOption"),
            @SlowLogExtra(name = "writeCephS3ObjectKey", data = "key") })
    public CephS3DataWriterImpl(BucketNameOption bucketNameOption, String key, ScmService service,
            String wsName, CephS3DataLocation location, int siteId, ScmDataWriterContext context)
            throws CephS3Exception {
        this.key = key;
        this.bucketNameOption = bucketNameOption;
        uploader = new CephS3UploaderWrapper((CephS3DataService) service, bucketNameOption, key,
                wsName, location, siteId, context);
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
        uploader.complete();
        // complete 失败，不能释放资源，外面需要调用 cancel，cancel 做完后再负责释放资源
        closeUploaderSilence();
    }

    @Override
    public long getSize() {
        return uploader.getFileSize();
    }

    @Override
    public String getCreatedTableName() {
        return uploader.getCreatedBucketName();
    }

    private void closeUploaderSilence() {
        try {
            uploader.close();
        }
        catch (Exception e) {
            logger.warn("failed to close uploader: bucket={}, key={}", bucketNameOption, key,
                    e);
        }
    }

}

