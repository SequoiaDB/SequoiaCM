package com.sequoiacm.cephs3.dataoperation;

import com.sequoiacm.cephs3.CephS3Exception;
import com.sequoiacm.cephs3.dataservice.CephS3BreakpointFileContext;
import com.sequoiacm.datasource.ScmDatasourceException;
import com.sequoiacm.datasource.dataoperation.ScmBreakpointDataWriter;
import com.sequoiacm.datasource.dataservice.ScmService;
import org.bson.BSONObject;

public class CephS3BreakpointDataWriter implements ScmBreakpointDataWriter {

    private final CephS3MultipartUploader uploader;

    public CephS3BreakpointDataWriter(String bucketName, String key,
            CephS3BreakpointFileContext context, ScmService service, long writeDataOffSet,
            boolean createBucketIfNotExist) throws CephS3Exception {
        this.uploader = new CephS3MultipartUploader(service, bucketName, key, context.getUploadId(),
                writeDataOffSet, createBucketIfNotExist);
    }

    @Override
    public void write(byte[] data, int offset, int length) throws ScmDatasourceException {
        uploader.write(data, offset, length);
    }

    @Override
    public void truncate(long length) throws ScmDatasourceException {
    }

    @Override
    public void flush() throws ScmDatasourceException {
        uploader.flush();
    }

    @Override
    public void complete() throws ScmDatasourceException {
        uploader.complete();
    }

    @Override
    public void close() throws ScmDatasourceException {
        uploader.close();
    }

    @Override
    public void abort() throws ScmDatasourceException {
        uploader.cancel();
    }

    @Override
    public BSONObject getContext() {
        return new CephS3BreakpointFileContext(uploader.getUploadId()).toBSON();
    }

    @Override
    public String getCreatedTableName() {
        return null;
    }
}
