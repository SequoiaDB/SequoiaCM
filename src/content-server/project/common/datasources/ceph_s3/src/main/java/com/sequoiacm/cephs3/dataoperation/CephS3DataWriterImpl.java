package com.sequoiacm.cephs3.dataoperation;

import com.sequoiacm.cephs3.CephS3Exception;
import com.sequoiacm.datasource.dataoperation.ScmDataWriter;
import com.sequoiacm.datasource.dataservice.ScmService;

public class CephS3DataWriterImpl extends ScmDataWriter {
    private final CephS3MultipartUploader uploader;

    public CephS3DataWriterImpl(String bucketName, String key, ScmService service)
            throws CephS3Exception {
        uploader = new CephS3MultipartUploader(service, bucketName, key);
    }

    @Override
    public void write(byte[] content) throws CephS3Exception {
        write(content, 0, content.length);
    }

    @Override
    public void write(byte[] content, int offset, int len) throws CephS3Exception {
        uploader.write(content, offset, len);
    }

    @Override
    public void cancel() {
        uploader.cancel();
    }

    @Override
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
