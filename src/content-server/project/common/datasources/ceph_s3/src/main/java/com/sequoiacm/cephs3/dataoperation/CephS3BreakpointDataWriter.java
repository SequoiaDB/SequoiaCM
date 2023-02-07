package com.sequoiacm.cephs3.dataoperation;

import com.sequoiacm.exception.ScmError;
import org.bson.BSONObject;

import com.sequoiacm.cephs3.CephS3Exception;
import com.sequoiacm.cephs3.dataservice.CephS3BreakpointFileContext;
import com.sequoiacm.datasource.ScmDatasourceException;
import com.sequoiacm.datasource.common.ScmDataWriterContext;
import com.sequoiacm.datasource.dataoperation.ScmBreakpointDataWriter;
import com.sequoiacm.datasource.dataservice.ScmService;
import com.sequoiacm.datasource.metadata.cephs3.CephS3DataLocation;
import com.sequoiacm.infrastructure.common.annotation.SlowLog;
import com.sequoiacm.infrastructure.common.annotation.SlowLogExtra;

public class CephS3BreakpointDataWriter implements ScmBreakpointDataWriter {

    private final CephS3MultipartUploader uploader;

    @SlowLog(operation = "createWriter", extras = {
            @SlowLogExtra(name = "writeCephS3BucketName", data = "bucketNameOption"),
            @SlowLogExtra(name = "writeCephS3ObjectKey", data = "key") })
    public CephS3BreakpointDataWriter(BucketNameOption bucketNameOption, String key,
            CephS3BreakpointFileContext context, ScmService service, long writeDataOffSet,
            CephS3DataLocation cephS3DataLocation, String wsName,
            ScmDataWriterContext writerContext) throws CephS3Exception {
        this.uploader = new CephS3MultipartUploader(service, bucketNameOption, key,
                context, writeDataOffSet, cephS3DataLocation, wsName, writerContext);
    }

    @Override
    @SlowLog(operation = "writeData")
    public void write(byte[] data, int offset, int length) throws ScmDatasourceException {
        try {
            uploader.write(data, offset, length);
        }
        catch (CephS3Exception e) {
            if (e.getS3ErrorCode().equals(CephS3Exception.ERR_CODE_QUOTA_EXCEEDED)) {
                throw new ScmDatasourceException(ScmError.DATA_STORAGE_QUOTA_EXCEEDED,
                        "failed to write data", e);
            }
            throw e;
        }
    }

    @Override
    @SlowLog(operation = "truncateData")
    public void truncate(long length) throws ScmDatasourceException {
    }

    @Override
    @SlowLog(operation = "flushData")
    public void flush() throws ScmDatasourceException {
        try {
            uploader.flush();
        }
        catch (CephS3Exception e) {
            if (e.getS3ErrorCode().equals(CephS3Exception.ERR_CODE_QUOTA_EXCEEDED)) {
                throw new ScmDatasourceException(ScmError.DATA_STORAGE_QUOTA_EXCEEDED,
                        "failed to flush data", e);
            }
            throw e;
        }
    }

    @Override
    @SlowLog(operation = "completeData")
    public void complete() throws ScmDatasourceException {
        try {
            uploader.complete();
        }
        catch (CephS3Exception e) {
            if (e.getS3ErrorCode().equals(CephS3Exception.ERR_CODE_QUOTA_EXCEEDED)) {
                throw new ScmDatasourceException(ScmError.DATA_STORAGE_QUOTA_EXCEEDED,
                        "failed to complete data", e);
            }
            throw e;
        }
    }

    @Override
    @SlowLog(operation = "closeWriter")
    public void close() throws ScmDatasourceException {
        uploader.close();
    }

    @Override
    @SlowLog(operation = "abortData")
    public void abort() throws ScmDatasourceException {
        uploader.cancel();
    }

    @Override
    public BSONObject getBreakpointContext() {
        return uploader.getBreakpointContext().getBSON();
    }

    @Override
    public String getCreatedTableName() {
        return null;
    }
}
