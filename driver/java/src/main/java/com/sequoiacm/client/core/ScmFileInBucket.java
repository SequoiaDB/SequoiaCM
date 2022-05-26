package com.sequoiacm.client.core;

import com.sequoiacm.client.element.ScmId;
import com.sequoiacm.client.element.bizconf.ScmUploadConf;
import com.sequoiacm.client.exception.ScmException;
import com.sequoiacm.exception.ScmError;
import org.bson.BSONObject;

import java.io.InputStream;
import java.net.HttpURLConnection;

class ScmFileInBucket extends ScmFileImpl {

    private final String bucket;

    ScmFileInBucket(String bucket, long bucketId, String fileName) throws ScmException {
        super();
        setFileName(fileName);
        setBucketId(bucketId);
        this.bucket = bucket;
    }

    @Override
    public void setFileName(String fileName) throws ScmException {
        if (getFileName() == null) {
            super.setFileName(fileName);
            return;
        }
        if (!getFileName().equals(fileName)) {
            throw new ScmException(ScmError.INVALID_ARGUMENT,
                    "file name is different from initial file name: name=" + fileName
                            + ", initialFileName=" + getFileName());
        }
    }

    @Override
    public ScmId save() throws ScmException {
        return super.save(new ScmUploadConf(false, true));
    }

    @Override
    protected BSONObject sendTransformBreakpointFileRequest(ScmUploadConf conf)
            throws ScmException {
        return ws.getSession().getDispatcher().createFileInBucket(bucket,
                breakpointFile.getFileName(), toBSONObject(), conf.toBsonObject());
    }

    @Override
    protected BSONObject sendUploadFileRequest(InputStream data, ScmUploadConf conf)
            throws ScmException {
        return ws.getSession().getDispatcher().createFileInBucket(bucket, data,
                toBSONObject(), conf.toBsonObject());
    }

    @Override
    HttpURLConnection httpURLConnectionForSave(ScmUploadConf conf) throws ScmException {
        return ws.getSession().getDispatcher().createFileInBucketConn(bucket,
                toBSONObject(), conf.toBsonObject());
    }

    @Override
    public void delete(boolean isPhysical) throws ScmException {
        if (isExist()) {
            ScmSession conn;
            conn = ws.getSession();
            conn.getDispatcher().bucketDeleteFile(bucket, getFileName(), isPhysical);
            setIsDeleted(true);
        }
    }

    @Override
    public void delete() throws ScmException {
        delete(false);
    }
}
